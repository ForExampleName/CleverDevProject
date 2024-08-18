package com.example.testproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "OLD_CLIENT_MAPPING")
@Getter
@Setter
@NoArgsConstructor
public class ClientMapping {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "PATIENT_ID", nullable = false)
    private Patient patient;
}
