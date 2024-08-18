package com.example.oldsystem.repository;

import com.example.oldsystem.entity.Patient;
import org.springframework.data.repository.ListCrudRepository;

public interface PatientRepository extends ListCrudRepository<Patient, Long> {
}
