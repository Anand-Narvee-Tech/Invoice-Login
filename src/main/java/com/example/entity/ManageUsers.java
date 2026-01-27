package com.example.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    
    @Column(nullable = false)
    private String companyDomain;

    @Column(unique = true, nullable = false)
    private String email;

    private String primaryEmail;

    @Column(name = "role_name")
    private String roleName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleid")
    private Role role;

    // ID fields for audit
    private Long updatedBy;

    // Stored name fields
 
    @Column(name = "added_by_name")
    private String addedByName;

    @Column(name = "updated_by_name")
    private String updatedByName;

    // Stored full name
    @JsonProperty("fullName")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Relations for addedBy and createdBy
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Optional helper method to compute full name
    public String computeFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName.trim());
        if (lastName != null && !lastName.isBlank()) sb.append(" ").append(lastName.trim());
        return sb.toString().trim();
    }
    
    @PrePersist
    @PreUpdate
    public void capitalizeNames() {
        this.firstName = capitalize(this.firstName);
        this.middleName = capitalize(this.middleName);
        this.lastName = capitalize(this.lastName);

        // ✅ DO NOT override frontend fullName
        if (this.fullName == null || this.fullName.isBlank()) {
            this.fullName = computeFullName();
        }
    }


    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        value = value.trim();
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

	
    
    @Column(name = "approved")
    private Boolean approved = false;

    @Column(name = "active")
    private Boolean active = true;

    
    
}