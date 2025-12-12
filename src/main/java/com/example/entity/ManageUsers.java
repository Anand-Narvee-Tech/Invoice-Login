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

    private String primaryEmail;
    
    @Column(name = "role_name")
    private String roleName;
    
    // Link to Role entity
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")  // This column must exist in DB
    private Role role;

    private Long updatedBy;
    private String addedByName;
    private String updatedByName;

    // STORED VALUE
    private String fullName;

    // COMPUTED NAME (DO NOT OVERRIDE stored fullname)
//    public String getComputedFullName() {
//        StringBuilder sb = new StringBuilder();
//        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
//        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName.trim());
//        if (lastName != null && !lastName.isBlank()) sb.append(" ").append(lastName.trim());
//        return sb.toString().trim();
//    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdBy;
}
