package com.example.testproject.repository;

import com.example.testproject.entity.Patient;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface PatientRepository extends ListCrudRepository<Patient, Long> {
    Patient findByFirstNameAndLastName(String firstName, String lastName);

    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT cm.patient FROM ClientMapping cm WHERE cm.id=?1")
    Patient findByClientGuid(UUID clientGuid);
}
