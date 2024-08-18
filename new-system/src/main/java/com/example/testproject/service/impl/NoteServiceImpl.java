package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.dto.CommentRequestDto;
import com.example.testproject.dto.CommentResponseDto;
import com.example.testproject.entity.Note;
import com.example.testproject.entity.Patient;
import com.example.testproject.entity.Status;
import com.example.testproject.entity.User;
import com.example.testproject.repository.NoteRepository;
import com.example.testproject.service.NoteService;
import com.example.testproject.service.PatientService;
import com.example.testproject.service.UserService;
import com.example.testproject.statistics.Statistics;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class NoteServiceImpl implements NoteService {
    private final int THREAD_COUNT;
    private final String NOTE_URL;

    private final Validator validator;
    private final RestTemplate restTemplate;

    private final UserService userService;
    private final PatientService patientService;
    private final NoteRepository noteRepository;
    private final Statistics statistics;

    public NoteServiceImpl(Validator validator,
                           RestTemplate restTemplate,
                           UserService userService,
                           PatientService patientService,
                           NoteRepository noteRepository,
                           Statistics statistics,
                           @Value("${OLD_SYSTEM_CONTAINER_NAME:localhost}") String oldSystemHost,
                           @Value("${THREAD_COUNT}") int threadCount) {
        this.validator = validator;
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.patientService = patientService;
        this.noteRepository = noteRepository;
        this.statistics = statistics;
        NOTE_URL = "http://" + oldSystemHost + ":8080/api/v1/notes";
        THREAD_COUNT = threadCount;
    }

    @Override
    public void synchronizeNotesConcurrently(List<ClientDto> clients) throws ExecutionException, InterruptedException {
        List<Runnable> runnables = new ArrayList<>(THREAD_COUNT);
        int blockSize = clients.size() / THREAD_COUNT;

        for (int i = 0; i < THREAD_COUNT; i++) {
            final boolean lastBlock = (i == THREAD_COUNT - 1);
            final int currentBlock = i;
            runnables.add(() -> {
                List<ClientDto> threadClients;
                if (lastBlock) {
                    threadClients = clients.subList(currentBlock * blockSize, clients.size());
                } else {
                    threadClients = clients.subList(currentBlock * blockSize, (currentBlock + 1) * blockSize);
                }
                synchronizeNotes(threadClients);
            });
        }

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        List<? extends Future<?>> results = runnables.stream()
                .map(executorService::submit)
                .toList();

        executorService.shutdown();

        for (int thread = 0; thread < THREAD_COUNT; thread++) {
            results.get(thread).get();
        }
    }

    // for purpose of this whole task fetched previously client data is used
    @Override
    public void synchronizeNotes(List<ClientDto> clients) {
        Map<UUID, String> agenciesByClientId = new HashMap<>(clients.size());
        clients.forEach(client -> agenciesByClientId.put(client.guid(), client.agency()));

        List<Note> newAndUpdatedNotes = new ArrayList<>();

        agenciesByClientId.forEach((clientGuid, agency) -> {
            List<CommentResponseDto> commentResponse = fetchClientComments(clientGuid, agency);
            filterInvalidCommentData(commentResponse);

            commentResponse.forEach(comment -> {
                Patient patient = patientService.findPatientByClientGuid(comment.clientGuid());
                if (patient.getStatus().equals(Status.ACTIVE)) {
                    User user = userService.createUserIfNotExists(comment.loggedUser());

                    Note note;
                    if (noteRepository.existsByCommentId(comment.guid())) {
                        note = updateNodeIfNeeded(comment, user, patient, statistics);
                    } else {
                        note = buildNewNote(comment, user, patient);
                        statistics.addNewNotes(1);
                    }

                    newAndUpdatedNotes.add(note);
                }
            });
        });

        noteRepository.saveAll(newAndUpdatedNotes);
    }

    @Override
    public List<CommentResponseDto> fetchClientComments(UUID clientGuid, String agency) {
        CommentRequestDto request = CommentRequestDto.builder()
                .agency(agency)
                .clientGuid(clientGuid)
                .dateFrom(LocalDate.now().minusYears(1))
                .dateTo(LocalDate.now())
                .build();

        ResponseEntity<CommentResponseDto[]> response;

        try {
            response = restTemplate.postForEntity(NOTE_URL, request, CommentResponseDto[].class);
        } catch (RestClientException e) {
            log.error("Old system connection error on comment fetch. Client guid:{}, agency:{}", clientGuid, agency);
            return Collections.emptyList();
        }

        return response.getBody() == null ? Collections.emptyList() : Arrays.asList(response.getBody());
    }

    @Override
    public void filterInvalidCommentData(List<CommentResponseDto> commentData) {
        int invalidCommentDataCount = 0;

        Iterator<CommentResponseDto> commentIterator = commentData.iterator();
        while (commentIterator.hasNext()) {
            CommentResponseDto comment = commentIterator.next();
            if (!validator.validate(comment).isEmpty()) {
                commentIterator.remove();
                ++invalidCommentDataCount;
            }
        }

        statistics.addInvalidComments(invalidCommentDataCount);
    }

    private Note buildNewNote(CommentResponseDto commentData, User user, Patient patient) {
        return Note.builder()
                .note(commentData.comments())
                .createdAt(commentData.createdDateTime())
                .modifiedAt(commentData.modifiedDateTime())
                .createdBy(user)
                .commentId(commentData.guid())
                .patient(patient)
                .build();
    }

    private Note updateNodeIfNeeded(CommentResponseDto comment, User commentUser, Patient patient, Statistics statistics) {
        Note existingNote = noteRepository.findByCommentId(comment.guid());

        boolean isUpdated = false;
        boolean isCommentNewer = (existingNote.getModifiedAt() == null && comment.modifiedDateTime() != null)
                || (existingNote.getModifiedAt() != null && comment.modifiedDateTime() != null
                && comment.modifiedDateTime().isAfter(existingNote.getModifiedAt()));

        // modified time was changed and now comment is newer
        if (isCommentNewer) {
            existingNote.setModifiedAt(comment.modifiedDateTime());
            isUpdated = true;
        }

        // note was reassigned to different client representing different patient
        if (!existingNote.getPatient().getId().equals(patient.getId())) {
            existingNote.setPatient(patient);
            isUpdated = true;
        }

        // note text was changed
        if (isCommentNewer && !Objects.equals(existingNote.getNote(), comment.comments())) {
            existingNote.setNote(comment.comments());
        }

        // modifying user may be changed
        if (isCommentNewer) {
            if (existingNote.getModifiedBy() == null
                    || !Objects.equals(existingNote.getModifiedBy().getLogin(), commentUser.getLogin())) {
                existingNote.setModifiedBy(commentUser);
            }
        }

        // creating user was changed
        if (isCommentNewer && !Objects.equals(existingNote.getCreatedBy().getLogin(), commentUser.getLogin())) {
            existingNote.setCreatedBy(commentUser);
        }

        // created time was changed
        if (!Objects.equals(existingNote.getCreatedAt(), comment.createdDateTime())) {
            existingNote.setCreatedAt(comment.createdDateTime());
            isUpdated = true;
        }

        // situation when data can be modified without modified time change is also counting
        if (isUpdated) {
            statistics.addUpdatedNotes(1);
        }

        return existingNote;
    }
}
