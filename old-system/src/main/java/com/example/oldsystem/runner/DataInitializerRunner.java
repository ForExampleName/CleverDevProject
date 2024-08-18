package com.example.oldsystem.runner;

import com.example.oldsystem.entity.*;
import com.example.oldsystem.entity.old.Agency;
import com.example.oldsystem.entity.old.Client;
import com.example.oldsystem.entity.old.Comment;
import com.example.oldsystem.repository.NoteRepository;
import com.example.oldsystem.repository.PatientRepository;
import com.example.oldsystem.repository.UserRepository;
import com.example.oldsystem.repository.old.AgencyRepository;
import com.example.oldsystem.repository.old.ClientRepository;
import com.example.oldsystem.repository.old.CommentRepository;
import com.example.oldsystem.util.DateUtil;
import com.github.javafaker.Faker;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("with_init")
public class DataInitializerRunner implements ApplicationRunner {
    private static final int AGENCY_COUNT = 10; // min 2

    private static final int USER_COUNT = 20;
    private static final int UNIQUE_OLD_USER_COUNT = 5;

    private final int ALL_PATIENT_COUNT;
    private final int SAME_PATIENT_COUNT; // exists in both systems
    private final int OLD_PATIENT_UNIQUE_COUNT = 100; // exists only in old system
    private final int NEW_PATIENT_UNIQUE_COUNT = 200; // exists only in new system

    private static final int INACTIVE_PATIENT_PROBABILITY = 5; // 0 - 100 percent

    private static final int SAME_NOTE_COUNT = 7;
    private static final int OLD_LAST_MODIFIED_NOTE_COUNT = 4;
    private static final int NEW_LAST_MODIFIED_NOTE_COUNT = 4;

    private final int INITIALIZATION_THREAD_COUNT;

    private final PatientRepository patientRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final NoteRepository noteRepository;
    private final CommentRepository commentRepository;

    public DataInitializerRunner(PatientRepository patientRepository,
                                 AgencyRepository agencyRepository,
                                 UserRepository userRepository,
                                 ClientRepository clientRepository,
                                 NoteRepository noteRepository,
                                 CommentRepository commentRepository,
                                 @Value("${PATIENT_COUNT:2000}") int samePatientCount,
                                 @Value("${THREAD_COUNT:8}") int threadCount) {
        this.patientRepository = patientRepository;
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.noteRepository = noteRepository;
        this.commentRepository = commentRepository;
        SAME_PATIENT_COUNT = samePatientCount;
        INITIALIZATION_THREAD_COUNT = threadCount;
        ALL_PATIENT_COUNT = SAME_PATIENT_COUNT + OLD_PATIENT_UNIQUE_COUNT + NEW_PATIENT_UNIQUE_COUNT;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("STARTING DATA INITIALIZATION");

        Faker faker = new Faker();

        try {
            List<Agency> agencies = initAgencies(faker);
            List<String> existingUsernames = generateUsernames(faker, USER_COUNT - UNIQUE_OLD_USER_COUNT, Collections.emptyList());
            List<String> newUsernames = generateUsernames(faker, UNIQUE_OLD_USER_COUNT, existingUsernames);
            List<User> existingUsers = initUsers(existingUsernames);

            List<GeneralPatientInfo> generalPatientInfo = generateGeneralPatientInfo(faker);
            List<Client> clients = initClients(generalPatientInfo, agencies);
            List<Patient> patients = initPatients(generalPatientInfo, clients);

            generateNotesAndComments(faker, patients, clients, existingUsers, newUsernames);
        } catch (ExecutionException | InterruptedException e) {
            log.info("INITIALIZATION FAILED");
        }

        log.info("DATA INITIALIZATION ENDED");
    }

    private List<Agency> initAgencies(Faker faker) {
        Set<String> agencyNames = new HashSet<>();

        while (agencyNames.size() < AGENCY_COUNT) {
            agencyNames.add(faker.company().name());
        }

        return agencyRepository.saveAll(
                agencyNames.stream()
                        .map(name -> {
                            Agency agency = new Agency();
                            agency.setName(name);
                            return agency;
                        }).toList()
        );
    }

    private List<String> generateUsernames(Faker faker, int count, List<String> reference) {
        Set<String> usernames = new HashSet<>();

        while (usernames.size() < count) {
            String username = faker.name().username();
            if (!reference.contains(username)) {
                usernames.add(username);
            }
        }

        return new ArrayList<>(usernames);
    }

    private List<User> initUsers(List<String> usernames) {
        return userRepository.saveAll(
                usernames.stream().map(username -> {
                    User user = new User();
                    user.setLogin(username);
                    return user;
                }).toList()
        );
    }

    private List<GeneralPatientInfo> generateGeneralPatientInfo(Faker faker) {
        List<GeneralPatientInfo> patientInfos = new ArrayList<>();

        // generate data for same patients in both systems then unique for old system
        // and then unique for new one - in this order
        for (int i = 0; i < ALL_PATIENT_COUNT; i++) {
            GeneralPatientInfo info = new GeneralPatientInfo();

            while (true) {
                String firstName = faker.name().firstName();
                String lastName = faker.name().lastName();

                boolean exist = patientInfos.stream().anyMatch(p ->
                        p.getFirstName().equals(firstName) && p.getLastName().equals(lastName)
                );

                if (!exist) {
                    info.setFirstName(firstName);
                    info.setLastName(lastName);
                    break;
                }
            }

            info.setBirthDate(DateUtil.toLocalDate(faker.date().birthday(18, 60)));
            info.setCreatedAt(LocalDateTime.now());
            info.setStatus(
                    ThreadLocalRandom.current().nextInt(0, 100) < INACTIVE_PATIENT_PROBABILITY ?
                            Status.INACTIVE.name()
                            : Status.ACTIVE.name()
            );


            patientInfos.add(info);
        }

        return patientInfos;
    }

    private List<Client> initClients(List<GeneralPatientInfo> generalPatientInfos, List<Agency> agencies) {
        //clients that have their analogue in new system
        List<Client> clients = generalPatientInfos.stream()
                .limit(SAME_PATIENT_COUNT)
                .map(info -> {
                    Client firstClient = info.buildClient(agencies.get(
                            ThreadLocalRandom.current().nextInt(0, AGENCY_COUNT / 2)
                    ));
                    Client secondClient = info.buildClient(agencies.get(
                            ThreadLocalRandom.current().nextInt(AGENCY_COUNT / 2, AGENCY_COUNT)
                    ));
                    return List.of(firstClient, secondClient);
                }).flatMap(Collection::stream)
                .collect(Collectors.toList());

        //clients that unique in old system, will be created in new system during sync
        clients.addAll(generalPatientInfos.stream()
                .skip(SAME_PATIENT_COUNT)
                .limit(OLD_PATIENT_UNIQUE_COUNT)
                .map(info -> info.buildClient(
                        agencies.get(ThreadLocalRandom.current().nextInt(0, AGENCY_COUNT))
                ))
                .toList());

        //to test new client for existing patient (always 3 new client)
        Agency agency = new Agency();
        agency.setName("To test new client for existing patient");
        agencyRepository.save(agency);

        IntStream.of(1, 2, 3).forEach(i -> {
            Client client = generalPatientInfos.get(i).buildClient(agency);
            clientRepository.save(client);
        });

        return clientRepository.saveAll(clients);
    }

    private List<Patient> initPatients(List<GeneralPatientInfo> generalPatientInfos, List<Client> clients) {
        List<Patient> patients = new ArrayList<>(SAME_PATIENT_COUNT + NEW_PATIENT_UNIQUE_COUNT);

        // first 'SAME_PATIENT_COUNT' items in generalPatientInfos have two copies each in clients beginning
        for (int i = 0, sameClientStartIdx = 0; i < SAME_PATIENT_COUNT; i++, sameClientStartIdx += 2) {
            patients.add(generalPatientInfos.get(i).buildPatient(
                    List.of(clients.get(sameClientStartIdx).getId(), clients.get(sameClientStartIdx + 1).getId())
            ));
        }

        patients.addAll(generalPatientInfos.stream()
                .skip(SAME_PATIENT_COUNT + OLD_PATIENT_UNIQUE_COUNT)
                .map(GeneralPatientInfo::buildUniquePatient)
                .toList());

        return patientRepository.saveAll(patients);
    }

    private void generateNotesAndComments(Faker faker, List<Patient> patients, List<Client> clients,
                                          List<User> existingUsers, List<String> newUsernames) throws ExecutionException, InterruptedException {
        filterUniquePatients(patients).forEach(patient -> generateNotesForUniquePatient(
                faker, patient, existingUsers)
        );

        List<Patient> nonUniquePatients = filterNonUniquePatients(patients);
        generateNotesAndCommentsConcurrently(faker, nonUniquePatients, clients, existingUsers, newUsernames);

        filterUniqueClients(patients, clients).forEach(client -> generateCommentsForUniqueClient(
                faker, client, existingUsers, newUsernames)
        );
    }

    private void generateNotesForUniquePatient(Faker faker, Patient patient, List<User> users) {
        for (int i = 0; i < SAME_NOTE_COUNT; i++) {
            noteRepository.save(Note.builder()
                    .note(faker.lorem().sentence(15))
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .createdBy(users.get(ThreadLocalRandom.current().nextInt(0, users.size())))
                    .patient(patient)
                    .build());
        }
    }

    private void generateNotesAndCommentsConcurrently(Faker faker, List<Patient> patients, List<Client> clients,
                                                      List<User> existingUsers, List<String> newUsernames) throws ExecutionException, InterruptedException {
        List<Runnable> runnables = new ArrayList<>(INITIALIZATION_THREAD_COUNT);
        int blockSize = patients.size() / INITIALIZATION_THREAD_COUNT;

        for (int i = 0; i < INITIALIZATION_THREAD_COUNT; i++) {
            final boolean lastBlock = (i == INITIALIZATION_THREAD_COUNT - 1);
            final int currentBlock = i;
            runnables.add(() -> {
                List<Patient> threadPatients;
                if (lastBlock) {
                    threadPatients = patients.subList(currentBlock * blockSize, patients.size());
                } else {
                    threadPatients = patients.subList(currentBlock * blockSize, (currentBlock + 1) * blockSize);
                }
                threadPatients.forEach(threadPatient -> generateNotesAndCommentsInBothSystems(
                        faker, threadPatient, clients, existingUsers, newUsernames)
                );
            });
        }

        ExecutorService executorService = Executors.newFixedThreadPool(INITIALIZATION_THREAD_COUNT);

        List<? extends Future<?>> results = runnables.stream()
                .map(executorService::submit)
                .toList();

        executorService.shutdown();

        for (int thread = 0; thread < INITIALIZATION_THREAD_COUNT; thread++) {
            results.get(thread).get(); // not very effective but still faster
        }
    }

    private void generateNotesAndCommentsInBothSystems(Faker faker, Patient patient, List<Client> clients,
                                                       List<User> existingUsers, List<String> newUsernames) {
        List<Client> patientClients = filterPatientClients(clients, patient);
        patientClients.forEach(client -> {
            List<Note> notes = new ArrayList<>(SAME_NOTE_COUNT);
            for (int i = 0; i < SAME_NOTE_COUNT; i++) {
                LocalDateTime time = LocalDateTime.now().minusDays(3);
                String text = faker.lorem().sentence(15);
                User user = existingUsers.get(ThreadLocalRandom.current().nextInt(0, existingUsers.size()));

                Comment comment = commentRepository.save(Comment.builder()
                        .comment(text)
                        .createdAt(time)
                        .user(user.getLogin())
                        .client(client)
                        .build());

                notes.add(Note.builder()
                        .note(text)
                        .createdAt(time)
                        .createdBy(user)
                        .commentId(comment.getId())
                        .patient(patient)
                        .build());
            }

            for (int i = 0; i < OLD_LAST_MODIFIED_NOTE_COUNT; i++) {
                User user = existingUsers.get(ThreadLocalRandom.current().nextInt(0, existingUsers.size()));
                String newUsername = newUsernames.get(ThreadLocalRandom.current().nextInt(0, newUsernames.size()));
                LocalDateTime time = LocalDateTime.now().minusDays(3);

                Comment comment = commentRepository.save(Comment.builder()
                        .comment(faker.lorem().sentence(15))
                        .createdAt(time)
                        .modifiedAt(LocalDateTime.now())
                        .user(i % 2 == 0 ? user.getLogin() : newUsername)
                        .client(client)
                        .build());

                notes.add(Note.builder()
                        .note(faker.lorem().sentence(15))
                        .createdAt(time)
                        .createdBy(user)
                        .commentId(comment.getId())
                        .patient(patient)
                        .build());
            }

            for (int i = 0; i < NEW_LAST_MODIFIED_NOTE_COUNT; i++) {
                User user = existingUsers.get(ThreadLocalRandom.current().nextInt(0, existingUsers.size()));
                String newUsername = newUsernames.get(ThreadLocalRandom.current().nextInt(0, newUsernames.size()));
                LocalDateTime time = LocalDateTime.now().minusDays(3);

                Comment comment = commentRepository.save(Comment.builder()
                        .comment(faker.lorem().sentence(15))
                        .createdAt(time)
                        .user(i % 2 == 0 ? user.getLogin() : newUsername)
                        .client(client)
                        .build());

                notes.add(Note.builder()
                        .note(faker.lorem().sentence(15))
                        .createdAt(time)
                        .modifiedAt(LocalDateTime.now())
                        .createdBy(user)
                        .modifiedBy(user)
                        .commentId(comment.getId())
                        .patient(patient)
                        .build());
            }

            noteRepository.saveAll(notes);
        });
    }

    private void generateCommentsForUniqueClient(Faker faker, Client client, List<User> existingUsers, List<String> newUsernames) {
        for (int i = 0; i < SAME_NOTE_COUNT; i++) {
            User user = existingUsers.get(ThreadLocalRandom.current().nextInt(0, existingUsers.size()));
            String newUsername = newUsernames.get(ThreadLocalRandom.current().nextInt(0, newUsernames.size()));

            commentRepository.save(Comment.builder()
                    .comment(faker.lorem().sentence(15))
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .modifiedAt(LocalDateTime.now().minusDays(3))
                    .user(i % 4 == 0 ? user.getLogin() : newUsername)
                    .client(client)
                    .build());
        }
    }

    private List<Patient> filterUniquePatients(List<Patient> patients) {
        return patients.stream()
                .filter(patient -> patient.getClientMappings().isEmpty())
                .toList();
    }

    private List<Patient> filterNonUniquePatients(List<Patient> patients) {
        return patients.stream()
                .filter(patient -> !patient.getClientMappings().isEmpty())
                .toList();
    }

    private List<Client> filterUniqueClients(List<Patient> patients, List<Client> clients) {
        return clients.stream()
                .filter(client -> patients.stream()
                        .noneMatch(patient -> patient.getClientMappings().stream()
                                .map(ClientMapping::getId)
                                .toList()
                                .contains(client.getId())
                        )
                )
                .toList();
    }

    private List<Client> filterPatientClients(List<Client> clients, Patient patient) {
        return clients.stream()
                .filter(client ->
                        patient.getClientMappings().stream()
                                .map(ClientMapping::getId)
                                .toList()
                                .contains(client.getId())
                ).toList();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class GeneralPatientInfo {
        private String firstName;
        private String lastName;
        private String status;
        private LocalDate birthDate;
        private LocalDateTime createdAt;

        public Client buildClient(Agency agency) {
            return Client.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(status)
                    .birthDate(birthDate)
                    .createdAt(createdAt)
                    .agency(agency)
                    .build();
        }

        public Patient buildUniquePatient() {
            return Patient.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(Status.valueOf(status))
                    .build();
        }

        public Patient buildPatient(List<UUID> oldSystemUserIds) {
            Patient patient = buildUniquePatient();

            oldSystemUserIds.forEach(id -> {
                ClientMapping mapping = new ClientMapping();
                mapping.setId(id);
                patient.addClientMapping(mapping);
            });

            return patient;
        }
    }
}
