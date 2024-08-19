package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.entity.ClientMapping;
import com.example.testproject.entity.Patient;
import com.example.testproject.repository.PatientRepository;
import com.example.testproject.service.ClientService;
import com.example.testproject.statistics.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientServiceImplTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private Statistics statistics;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Test
    @DisplayName("Testing patients synchronization")
    public void testPatientsSynchronization() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build()
        );

        when(patientRepository.existsByFirstNameAndLastName(any(), any()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(clientService.createNewClients(any()))
                .thenReturn(2);

        // spy testing object to mock inner 'createNewPatients' method call
        PatientServiceImpl spy = spy(patientService);
        doReturn(1)
                .when(spy)
                .createNewPatients(any());

        spy.synchronizePatients(clientData);

        verify(clientService, times(1))
                .createNewClients(List.of(clientData.get(0), clientData.get(1)));
        verify(spy, times(1))
                .createNewPatients(List.of(clientData.get(2)));
        verify(statistics, times(1)).addNewClients(2);
        verify(statistics, times(1)).addNewPatients(1);
    }

    @Test
    @DisplayName("Trying to create new patients based on clients")
    public void testCreateNewPatientsWithSeveralNewClients() {
        final int uniquePatients = 2;

        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f1").lastName("l1")
                        .status("ACTIVE")
                        .build(),
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f2").lastName("l2")
                        .status("ACTIVE")
                        .build(),
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f1").lastName("l1")
                        .status("ACTIVE")
                        .build()
        );

        int result = patientService.createNewPatients(clientData);

        Assertions.assertEquals(uniquePatients, result);

        ArgumentCaptor<List<Patient>> captor = ArgumentCaptor.forClass(List.class);
        verify(patientRepository, times(1)).saveAll(captor.capture());

        captor.getAllValues().forEach(patients -> {
            Assertions.assertEquals(uniquePatients, patients.size());

            Patient patientWithTwoClients = patients.stream()
                    .filter(p -> p.getFirstName().equals(clientData.get(0).firstName())
                            && p.getLastName().equals(clientData.get(0).lastName()))
                    .findFirst().get();

            Patient patientWithOneClient = patients.stream()
                    .filter(p -> p.getFirstName().equals(clientData.get(1).firstName())
                            && p.getLastName().equals(clientData.get(1).lastName()))
                    .findFirst().get();

            boolean areFirstAndThirdClientsTogether = patientWithTwoClients.getClientMappings().stream()
                    .map(ClientMapping::getId)
                    .toList()
                    .containsAll(List.of(clientData.get(0).guid(), clientData.get(2).guid()));

            boolean isSecondClientSeparated = patientWithOneClient.getClientMappings().stream()
                    .map(ClientMapping::getId)
                    .toList()
                    .contains(clientData.get(1).guid());

            Assertions.assertTrue(areFirstAndThirdClientsTogether);
            Assertions.assertTrue(isSecondClientSeparated);
        });
    }

    @Test
    @DisplayName("Trying to find existing patient by client GUID")
    public void testFindPatientByClientGuidWithExistingPatient() {
        when(patientRepository.findByClientGuid(any())).thenReturn(new Patient());
        Patient patient = patientService.findPatientByClientGuid(UUID.randomUUID());
        Assertions.assertNotNull(patient);
    }

    @Test
    @DisplayName("Trying find existing patient by client GUID")
    public void testFindPatientByClientGuidWithNewPatient() {
        when(patientRepository.findByClientGuid(any())).thenReturn(null);
        Patient patient = patientService.findPatientByClientGuid(UUID.randomUUID());
        Assertions.assertNull(patient);
    }
}
