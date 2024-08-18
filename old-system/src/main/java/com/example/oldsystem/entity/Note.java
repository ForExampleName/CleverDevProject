package com.example.oldsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "PATIENT_NOTE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOTE", length = 4000)
    private String note;

    @Column(name = "CREATED_DATE_TIME", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "LAST_MODIFIED_DATE_TIME")
    private LocalDateTime modifiedAt;

    @ManyToOne
    @JoinColumn(name = "CREATED_BY_USER_ID")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "LAST_MODIFIED_BY_USER_ID")
    private User modifiedBy;

    @Column(name = "COMMENT_ID")
    private UUID commentId;

    @ManyToOne
    @JoinColumn(name = "PATIENT_ID", nullable = false)
    private Patient patient;
}
