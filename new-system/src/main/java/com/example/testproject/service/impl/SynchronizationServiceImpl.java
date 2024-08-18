package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.service.ClientService;
import com.example.testproject.service.NoteService;
import com.example.testproject.service.PatientService;
import com.example.testproject.service.SynchronizationService;
import com.example.testproject.statistics.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SynchronizationServiceImpl implements SynchronizationService {
    private final ClientService clientService;
    private final PatientService patientService;
    private final NoteService noteService;
    private final Statistics statistics;

    public SynchronizationServiceImpl(ClientService clientService,
                                      PatientService patientService,
                                      NoteService noteService,
                                      Statistics statistics) {
        this.clientService = clientService;
        this.patientService = patientService;
        this.noteService = noteService;
        this.statistics = statistics;
    }

    @Override
    public void synchronize() {
        statistics.reset();
        log.info("Starting synchronization");
        try {
            List<ClientDto> clients = clientService.fetchClientData();
            clientService.filterInvalidClientData(clients);
            patientService.synchronizePatients(clients);
            noteService.synchronizeNotesConcurrently(clients);
            log.info("Synchronization finished");
        } catch (Exception e) {
            log.info("Synchronization failed");
            log.error(e.getMessage(), e);
        } finally {
            statistics.printStatistics();
        }
    }
}
