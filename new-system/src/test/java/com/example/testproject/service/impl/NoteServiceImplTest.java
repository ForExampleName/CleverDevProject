package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.dto.CommentResponseDto;
import com.example.testproject.entity.Note;
import com.example.testproject.entity.Patient;
import com.example.testproject.entity.Status;
import com.example.testproject.entity.User;
import com.example.testproject.repository.NoteRepository;
import com.example.testproject.service.PatientService;
import com.example.testproject.service.UserService;
import com.example.testproject.statistics.Statistics;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NoteServiceImplTest {
    @Mock
    private Validator validator;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserService userService;

    @Mock
    private PatientService patientService;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private Statistics statistics;

    private NoteServiceImpl noteService;

    @BeforeEach
    void setUp() {
        // must be done this way because of @Value in NoteServiceImpl constructor
        // mockito try to parse '@Value("${THREAD_COUNT:4}") int threadCount' to int value
        // but fails because 'threadCount' got nothing
        // test class for 'ClientServiceImpl' is ok because CLIENT_URL just gets null
        // but this cannot be used with int field 'threadCount'
        String oldSystemHost = "localhost";
        int threadCount = 4;
        noteService = new NoteServiceImpl(validator, restTemplate, userService,
                patientService, noteRepository, statistics, oldSystemHost, threadCount);
    }

    @Test
    @DisplayName("Testing concurrent note synchronization")
    public void testConcurrentNoteSynchronization() throws ExecutionException, InterruptedException {
        List<ClientDto> clientData = List.of(
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build()
        );

        NoteServiceImpl spy = spy(noteService);
        doNothing()
                .when(spy)
                .synchronizeNotes(any());

        spy.synchronizeNotesConcurrently(clientData);

        ArgumentCaptor<List<ClientDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(spy, times(clientData.size())).synchronizeNotes(captor.capture());

        Set<UUID> uuids = new HashSet<>();
        captor.getAllValues().forEach(arg -> {
            boolean isConsistOfOne = arg.size() == 1;
            Assertions.assertTrue(isConsistOfOne);

            boolean isUniqueClient = !uuids.contains(arg.get(0).guid());
            Assertions.assertTrue(isUniqueClient);
            uuids.add(arg.get(0).guid());
        });
    }

    @Test
    @DisplayName("Testing simple note synchronization. Comment patient is inactive")
    public void testNoteSynchronizeWithInactivePatient() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .agency("a")
                        .build()
        );

        NoteServiceImpl spy = spy(noteService);

        List<CommentResponseDto> response = List.of(CommentResponseDto.builder().build());
        doReturn(response)
                .when(spy)
                .fetchClientComments(clientData.get(0).guid(), clientData.get(0).agency());
        doNothing()
                .when(spy)
                .filterInvalidCommentData(any());

        Patient inactivePatient = Patient.builder()
                .status(Status.INACTIVE)
                .build();

        when(patientService.findPatientByClientGuid(any()))
                .thenReturn(inactivePatient);

        spy.synchronizeNotes(clientData);

        verify(userService, never()).createUserIfNotExists(any());
        verify(noteRepository, never()).existsByCommentId(any());
        verify(statistics, never()).addNewNotes(anyInt());

        ArgumentCaptor<List<Note>> captor = ArgumentCaptor.forClass(List.class);
        verify(noteRepository, times(1)).saveAll(captor.capture());

        Assertions.assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("Testing simple note synchronization. Comment patient is inactive")
    public void testNoteSynchronizeWithActivePatient() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .agency("a")
                        .build()
        );

        NoteServiceImpl spy = spy(noteService);

        List<CommentResponseDto> response = List.of(CommentResponseDto.builder().build());
        doReturn(response)
                .when(spy)
                .fetchClientComments(clientData.get(0).guid(), clientData.get(0).agency());
        doNothing()
                .when(spy)
                .filterInvalidCommentData(any());

        Patient inactivePatient = Patient.builder()
                .status(Status.INACTIVE)
                .build();

        when(patientService.findPatientByClientGuid(any()))
                .thenReturn(inactivePatient);

        spy.synchronizeNotes(clientData);

        verify(userService, never()).createUserIfNotExists(any());
        verify(noteRepository, never()).existsByCommentId(any());
        verify(statistics, never()).addNewNotes(anyInt());

        ArgumentCaptor<List<Note>> captor = ArgumentCaptor.forClass(List.class);
        verify(noteRepository, times(1)).saveAll(captor.capture());

        Assertions.assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("Testing simple note synchronization. Comment patient is active. Note already exists")
    public void testNoteSynchronizeWithActivePatientAndExistingNote() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .agency("a")
                        .build()
        );

        Patient patient = Patient.builder()
                .id(1L)
                .status(Status.ACTIVE)
                .build();

        Note note = new Note();
        note.setPatient(patient);

        NoteServiceImpl spy = spy(noteService);

        List<CommentResponseDto> response = List.of(CommentResponseDto.builder().build());
        doReturn(response)
                .when(spy)
                .fetchClientComments(clientData.get(0).guid(), clientData.get(0).agency());
        doNothing()
                .when(spy)
                .filterInvalidCommentData(any());

        when(patientService.findPatientByClientGuid(any()))
                .thenReturn(patient);
        when(userService.createUserIfNotExists(any()))
                .thenReturn(new User());
        when(noteRepository.existsByCommentId(any()))
                .thenReturn(true);
        when(noteRepository.findByCommentId(any()))
                .thenReturn(note);

        spy.synchronizeNotes(clientData);

        verify(userService, times(1)).createUserIfNotExists(any());
        verify(noteRepository, times(1)).existsByCommentId(any());
        verify(noteRepository, times(1)).findByCommentId(any());
        verify(statistics, never()).addNewNotes(anyInt());

        ArgumentCaptor<List<Note>> captor = ArgumentCaptor.forClass(List.class);
        verify(noteRepository, times(1)).saveAll(captor.capture());

        Assertions.assertEquals(1, captor.getValue().size());
    }

    @Test
    @DisplayName("Testing simple note synchronization. Comment patient is active. Note is new")
    public void testNoteSynchronizeWithActivePatientAndNoteIsNew() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .agency("a")
                        .build()
        );

        Patient patient = Patient.builder()
                .status(Status.ACTIVE)
                .build();

        NoteServiceImpl spy = spy(noteService);

        List<CommentResponseDto> response = List.of(CommentResponseDto.builder().build());
        doReturn(response)
                .when(spy)
                .fetchClientComments(clientData.get(0).guid(), clientData.get(0).agency());
        doNothing()
                .when(spy)
                .filterInvalidCommentData(any());

        when(patientService.findPatientByClientGuid(any()))
                .thenReturn(patient);
        when(userService.createUserIfNotExists(any()))
                .thenReturn(new User());
        when(noteRepository.existsByCommentId(any()))
                .thenReturn(false);

        spy.synchronizeNotes(clientData);

        verify(userService, times(1)).createUserIfNotExists(any());
        verify(noteRepository, times(1)).existsByCommentId(any());
        verify(statistics, times(1)).addNewNotes(1);

        ArgumentCaptor<List<Note>> captor = ArgumentCaptor.forClass(List.class);
        verify(noteRepository, times(1)).saveAll(captor.capture());

        Assertions.assertEquals(1, captor.getValue().size());
    }

    @Test
    @DisplayName("Trying to fetch client comments with unreachable source")
    public void testFetchClientCommentsWithUnreachableSource() {
        final String innerMessage = "Unreachable";
        Exception exception = new RestClientException(innerMessage);

        when(restTemplate.postForEntity(anyString(), any(), eq(CommentResponseDto[].class)))
                .thenThrow(exception);

        List<CommentResponseDto> response = noteService.fetchClientComments(UUID.randomUUID(), "agency");

        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("Trying to fetch client comments with null response body")
    public void testFetchClientCommentsWithNullResponseBody() {
        when(restTemplate.postForEntity(anyString(), any(), eq(CommentResponseDto[].class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<CommentResponseDto> response = noteService.fetchClientComments(UUID.randomUUID(), "agency");

        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("Fetching client comments successfully")
    public void testFetchClientCommentsWithDataListReturn() {
        CommentResponseDto[] commentData = new CommentResponseDto[]{
                CommentResponseDto.builder().guid(UUID.randomUUID()).build()
        };
        ResponseEntity<CommentResponseDto[]> response = new ResponseEntity<>(commentData, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(CommentResponseDto[].class)))
                .thenReturn(response);

        List<CommentResponseDto> result = noteService.fetchClientComments(UUID.randomUUID(), "agency");
        Assertions.assertEquals(commentData[0].guid(), result.get(0).guid());
    }

    @Test
    @DisplayName("Filtering invalid comment data. All data valid")
    public void testFilterInvalidCommentDataWithValidData() {
        List<CommentResponseDto> commentData = new ArrayList<>();
        commentData.add(CommentResponseDto.builder().build());
        commentData.add(CommentResponseDto.builder().build());

        final int sizeBefore = commentData.size();

        when(validator.validate(any(CommentResponseDto.class))).thenReturn(Set.of());

        noteService.filterInvalidCommentData(commentData);

        Assertions.assertEquals(sizeBefore, commentData.size());
        verify(validator, times(commentData.size())).validate(any(CommentResponseDto.class));
        verify(statistics).addInvalidComments(0);
    }

    @Test
    @DisplayName("Filtering invalid comment data. One item is invalid")
    public void testFilterInvalidCommentDataWithInvalidItem() {
        List<CommentResponseDto> commentData = new ArrayList<>();
        commentData.add(CommentResponseDto.builder().build());
        commentData.add(CommentResponseDto.builder().build());

        final int sizeBefore = commentData.size();

        Set<ConstraintViolation<CommentResponseDto>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));

        when(validator.validate(any(CommentResponseDto.class)))
                .thenReturn(violations)
                .thenReturn(Set.of());

        noteService.filterInvalidCommentData(commentData);

        Assertions.assertEquals(sizeBefore - 1, commentData.size());
        verify(validator, times(sizeBefore)).validate(any(CommentResponseDto.class));
        verify(statistics).addInvalidComments(1);
    }
}