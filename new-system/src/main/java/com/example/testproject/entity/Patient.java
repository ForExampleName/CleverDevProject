package com.example.testproject.entity;

import com.example.testproject.converter.StatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "PATIENT_PROFILE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Builder.Default
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true, mappedBy = "patient")
    private Set<ClientMapping> clientMappings = new HashSet<>();

    @Column(name = "STATUS_ID", nullable = false)
    @Convert(converter = StatusConverter.class)
    private Status status;

    public void addClientMapping(ClientMapping clientMapping) {
        Objects.requireNonNull(clientMapping, "Client mapping must not be null");
        clientMappings.add(clientMapping);
        clientMapping.setPatient(this);
    }
}
