package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.entity.ClientMapping;
import com.example.testproject.entity.Patient;
import com.example.testproject.exception.ConnectionException;
import com.example.testproject.exception.DataFetchException;
import com.example.testproject.repository.PatientRepository;
import com.example.testproject.service.ClientService;
import com.example.testproject.statistics.Statistics;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ClientServiceImpl implements ClientService {
    private final String CLIENT_URL;

    private final Validator validator;
    private final RestTemplate restTemplate;
    private final Statistics statistics;
    private final PatientRepository patientRepository;

    public ClientServiceImpl(Validator validator,
                             RestTemplate restTemplate,
                             Statistics statistics,
                             PatientRepository patientRepository,
                             @Value("${OLD_SYSTEM_CONTAINER_NAME:localhost}") String oldSystemHost) {
        this.validator = validator;
        this.restTemplate = restTemplate;
        this.statistics = statistics;
        this.patientRepository = patientRepository;
        CLIENT_URL = "http://" + oldSystemHost + ":8080/api/v1/clients";
    }

    @Override
    public List<ClientDto> fetchClientData() throws DataFetchException, ConnectionException {
        ResponseEntity<ClientDto[]> response;

        try {
            response = restTemplate.postForEntity(CLIENT_URL, null, ClientDto[].class);
        } catch (RestClientException e) {
            log.info("Client data fetch error");
            throw new ConnectionException("Old system connection error on client fetch", e);
        }

        if (response.getBody() == null) {
            log.info("Client data fetch error");
            throw new DataFetchException("Client data absent in response");
        }
        return Arrays.asList(response.getBody());
    }

    @Override
    public void filterInvalidClientData(List<ClientDto> clients) {
        int invalidClientDataCount = 0;

        Iterator<ClientDto> clientIterator = clients.iterator();
        while (clientIterator.hasNext()) {
            ClientDto client = clientIterator.next();
            if (!validator.validate(client).isEmpty()) {
                clientIterator.remove();
                ++invalidClientDataCount;
            }
        }

        statistics.addInvalidClientData(invalidClientDataCount);
    }

    @Override
    public int createNewClients(List<ClientDto> clients) {
        AtomicInteger newClientCount = new AtomicInteger(0);

        clients.forEach(client -> {
            Patient patient = patientRepository.findByFirstNameAndLastName(client.firstName(), client.lastName());

            boolean newClient = patient.getClientMappings().stream()
                    .map(ClientMapping::getId)
                    .noneMatch(id -> client.guid().equals(id));

            if (newClient) {
                ClientMapping mapping = new ClientMapping();
                mapping.setId(client.guid());
                patient.addClientMapping(mapping);
                patientRepository.save(patient);
                newClientCount.getAndIncrement();
            }
        });

        return newClientCount.get();
    }
}
