package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "manage_users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ManageUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String roleName;  // SUPERADMIN, ADMIN, etc.

    // Stores the ID of the user who last updated this record
    private Long updatedBy;

    // Stores the full name of the user who last updated this record
    private String updatedByName;

    // Admin (User) who added this ManageUser record
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    // User who created this ManageUser record (system tracking)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdBy;
}
