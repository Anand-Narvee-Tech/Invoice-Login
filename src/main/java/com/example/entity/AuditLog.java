package com.example.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;              // e.g. "UPDATE", "CREATE", "DELETE"
    private String entityName;          // e.g. "ManageUsers"
    private Long entityId;              // e.g. 45
    private String performedBy;         // User full name
    private Long performedById;         // User ID
    private String email;               // User email
    private LocalDateTime timestamp;    // When action happened

    @Column(length = 2000)
    private String details;             // Optional: JSON or description of change
}
