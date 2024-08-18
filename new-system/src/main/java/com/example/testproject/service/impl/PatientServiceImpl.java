package com.example.testproject.service.impl;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.dto.FullNameDto;
import com.example.testproject.entity.ClientMapping;
import com.example.testproject.entity.Patient;
import com.example.testproject.entity.Status;
import com.example.testproject.repository.PatientRepository;
import com.example.testproject.service.ClientService;
import com.example.testproject.service.PatientService;
import com.example.testproject.statistics.Statistics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final ClientService clientService;
    private final Statistics statistics;

    public PatientServiceImpl(PatientRepository patientRepository,
                              ClientService clientService,
                              Statistics statistics) {
        this.patientRepository = patientRepository;
        this.clientService = clientService;
        this.statistics = statistics;
    }

    @Override
    public void synchronizePatients(List<ClientDto> clients) {
        Map<Boolean, List<ClientDto>> clientsByExistence = clients.stream()
                .collect(Collectors.partitioningBy(
                        client -> patientRepository.existsByFirstNameAndLastName(client.firstName(), client.lastName())
                ));

        clientsByExistence.forEach((isExist, clientData) -> {
            if (isExist) {
                int count = clientService.createNewClients(clientData);
                statistics.addNewClients(count);
            } else {
                int count = createNewPatients(clientData);
                statistics.addNewPatients(count);
            }
        });
    }

    @Override
    public int createNewPatients(List<ClientDto> clients) {
        List<Patient> newPatients = new ArrayList<>();

        Map<FullNameDto, List<ClientDto>> clientsByFullName = clients.stream()
                .collect(Collectors.groupingBy(
                        item -> new FullNameDto(item.firstName(), item.lastName())
                ));

        clientsByFullName.forEach((fullName, clientData) -> {
            boolean isActive = clientData.stream()
                    .noneMatch(client -> client.status().equals(Status.INACTIVE.name()));

            Patient newPatient = Patient.builder()
                    .firstName(fullName.firstName())
                    .lastName(fullName.lastName())
                    .status(isActive ? Status.ACTIVE : Status.INACTIVE)
                    .build();

            List<ClientMapping> mappings = clientData.stream()
                    .map(ClientDto::guid)
                    .map(id -> {
                        ClientMapping mapping = new ClientMapping();
                        mapping.setId(id);
                        return mapping;
                    })
                    .toList();

            mappings.forEach(newPatient::addClientMapping);

            newPatients.add(newPatient);
        });

        patientRepository.saveAll(newPatients);

        return newPatients.size();
    }

    @Override
    public Patient findPatientByClientGuid(UUID clientGuid) {
        return patientRepository.findByClientGuid(clientGuid);
    }
}
