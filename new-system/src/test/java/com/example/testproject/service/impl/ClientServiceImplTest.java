package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.entity.ClientMapping;
import com.example.testproject.entity.Patient;
import com.example.testproject.exception.ConnectionException;
import com.example.testproject.exception.DataFetchException;
import com.example.testproject.repository.PatientRepository;
import com.example.testproject.statistics.Statistics;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {
    @Mock
    private Validator validator;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Statistics statistics;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    @DisplayName("Trying fetch client data with unreachable source")
    public void testFetchClientDataWithUnreachableSource() {
        final String innerMessage = "Unreachable";
        Exception exception = new RestClientException(innerMessage);

        when(restTemplate.postForEntity(anyString(), isNull(), eq(ClientDto[].class)))
                .thenThrow(exception);

        ConnectionException receivedException = Assertions.assertThrows(ConnectionException.class, () -> {
            clientService.fetchClientData();
        });
        Assertions.assertEquals(innerMessage, receivedException.getCause().getMessage());
        Assertions.assertEquals(exception, receivedException.getCause());
    }

    @Test
    @DisplayName("Fetching client data with null response body")
    public void testFetchClientDataWithNullResponseBody() {
        ResponseEntity<ClientDto[]> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), isNull(), eq(ClientDto[].class)))
                .thenReturn(response);

        Assertions.assertThrows(DataFetchException.class, () -> {
            clientService.fetchClientData();
        });
    }

    @Test
    @DisplayName("Fetching client data successfully")
    public void testFetchClientDataWithDataListReturn() {
        ClientDto[] clientData = new ClientDto[]{
                ClientDto.builder().guid(UUID.randomUUID()).build(),
                ClientDto.builder().guid(UUID.randomUUID()).build()
        };
        ResponseEntity<ClientDto[]> response = new ResponseEntity<>(clientData, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), isNull(), eq(ClientDto[].class)))
                .thenReturn(response);

        List<ClientDto> result = Assertions.assertDoesNotThrow(() -> clientService.fetchClientData());
        Assertions.assertEquals(clientData[0].guid(), result.get(0).guid());
        Assertions.assertEquals(clientData[1].guid(), result.get(1).guid());
    }

    @Test
    @DisplayName("Filtering invalid client data. All data valid")
    public void testFilterInvalidClientDataWithValidData() {
        List<ClientDto> clientData = new ArrayList<>();
        clientData.add(ClientDto.builder().build());
        clientData.add(ClientDto.builder().build());

        final int sizeBefore = clientData.size();

        when(validator.validate(any(ClientDto.class))).thenReturn(Set.of());

        clientService.filterInvalidClientData(clientData);

        Assertions.assertEquals(sizeBefore, clientData.size());
        verify(validator, times(clientData.size())).validate(any(ClientDto.class));
        verify(statistics).addInvalidClientData(0);
    }

    @Test
    @DisplayName("Filtering invalid client data. One item is invalid")
    public void testFilterInvalidClientDataWithInvalidItem() {
        List<ClientDto> clientData = new ArrayList<>();
        clientData.add(ClientDto.builder().build());
        clientData.add(ClientDto.builder().build());

        final int sizeBefore = clientData.size();

        Set<ConstraintViolation<ClientDto>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));

        when(validator.validate(any(ClientDto.class)))
                .thenReturn(violations)
                .thenReturn(Set.of());

        clientService.filterInvalidClientData(clientData);

        Assertions.assertEquals(sizeBefore - 1, clientData.size());
        verify(validator, times(sizeBefore)).validate(any(ClientDto.class));
        verify(statistics).addInvalidClientData(1);
    }

    @Test
    @DisplayName("Trying to create new client. One client is new")
    public void testCreateNewClientsWithOneNewClientInList() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f1")
                        .lastName("l1")
                        .build(),
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f2")
                        .lastName("l2")
                        .build()
        );

        ClientMapping mappingForFirstClient = new ClientMapping();
        mappingForFirstClient.setId(clientData.get(0).guid());

        Patient firstPatient = Patient.builder()
                .clientMappings(Set.of(mappingForFirstClient))
                .build();
        Patient secondPatient = Patient.builder()
                .clientMappings(new HashSet<>())
                .build();

        when(patientRepository.findByFirstNameAndLastName(clientData.get(0).firstName(), clientData.get(0).lastName()))
                .thenReturn(firstPatient);

        when(patientRepository.findByFirstNameAndLastName(clientData.get(1).firstName(), clientData.get(1).lastName()))
                .thenReturn(secondPatient);

        int createdCount = clientService.createNewClients(clientData);

        Assertions.assertEquals(1, createdCount);
        verify(patientRepository, times(1))
                .findByFirstNameAndLastName(clientData.get(0).firstName(), clientData.get(0).lastName());
        verify(patientRepository, times(1))
                .findByFirstNameAndLastName(clientData.get(1).firstName(), clientData.get(1).lastName());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Trying to create new client. All client are new")
    public void testCreateNewClientsWithAllExistingClients() {
        List<ClientDto> clientData = List.of(
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f1")
                        .lastName("l1")
                        .build(),
                ClientDto.builder()
                        .guid(UUID.randomUUID())
                        .firstName("f2")
                        .lastName("l2")
                        .build()
        );

        ClientMapping mappingForFirstClient = new ClientMapping();
        mappingForFirstClient.setId(clientData.get(0).guid());

        ClientMapping mappingForSecondClient = new ClientMapping();
        mappingForSecondClient.setId(clientData.get(1).guid());

        Patient firstPatient = Patient.builder()
                .clientMappings(Set.of(mappingForFirstClient))
                .build();
        Patient secondPatient = Patient.builder()
                .clientMappings(Set.of(mappingForSecondClient))
                .build();

        when(patientRepository.findByFirstNameAndLastName(clientData.get(0).firstName(), clientData.get(0).lastName()))
                .thenReturn(firstPatient);

        when(patientRepository.findByFirstNameAndLastName(clientData.get(1).firstName(), clientData.get(1).lastName()))
                .thenReturn(secondPatient);

        int createdCount = clientService.createNewClients(clientData);

        Assertions.assertEquals(0, createdCount);

        verify(patientRepository, times(1))
                .findByFirstNameAndLastName(clientData.get(0).firstName(), clientData.get(0).lastName());
        verify(patientRepository, times(1))
                .findByFirstNameAndLastName(clientData.get(1).firstName(), clientData.get(1).lastName());
        verify(patientRepository, never()).save(any(Patient.class));
    }
}
