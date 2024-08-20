package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.exception.ConnectionException;
import com.example.testproject.service.ClientService;
import com.example.testproject.service.NoteService;
import com.example.testproject.service.PatientService;
import com.example.testproject.statistics.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SynchronizationServiceImplTest {
    @Mock
    private ClientService clientService;

    @Mock
    private PatientService patientService;

    @Mock
    private NoteService noteService;

    @Mock
    private Statistics statistics;

    @InjectMocks
    private SynchronizationServiceImpl synchronizationService;

    @Test
    @DisplayName("Trying to synchronize successfully")
    void testSynchronizationSuccess() throws Exception {
        List<ClientDto> clients = List.of(
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build()
        );

        when(clientService.fetchClientData()).thenReturn(clients);
        doNothing().when(clientService).filterInvalidClientData(clients);
        doNothing().when(patientService).synchronizePatients(clients);
        doNothing().when(noteService).synchronizeNotesConcurrently(clients);

        synchronizationService.synchronize();

        verify(statistics).reset();
        verify(clientService).fetchClientData();
        verify(clientService).filterInvalidClientData(clients);
        verify(patientService).synchronizePatients(clients);
        verify(noteService).synchronizeNotesConcurrently(clients);
        verify(statistics).generateStatisticsMessage();
    }

    @Test
    @DisplayName("Trying to synchronize. Network failure")
    void testSynchronizationFailure() throws Exception {
        Exception exception = new ConnectionException("Test exception", new RestClientException(""));

        when(clientService.fetchClientData()).thenThrow(exception);

        synchronizationService.synchronize();

        verify(statistics).reset();
        verify(clientService).fetchClientData();
        verify(statistics).generateStatisticsMessage();
        verify(clientService, never()).filterInvalidClientData(any());
        verify(patientService, never()).synchronizePatients(any());
        verify(noteService, never()).synchronizeNotesConcurrently(any());
    }
}
