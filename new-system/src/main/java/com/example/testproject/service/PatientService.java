package com.example.testproject.service;


import com.example.testproject.dto.ClientDto;
import com.example.testproject.entity.Patient;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    void synchronizePatients(List<ClientDto> clients);

    int createNewPatients(List<ClientDto> clients);

    Patient findPatientByClientGuid(UUID clientGuid);
}
