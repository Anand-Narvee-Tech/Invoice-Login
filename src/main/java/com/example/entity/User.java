package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String mobileNumber;
    private String companyName;

    private String fullName;

    private Boolean active;
    private Boolean approved;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    private String position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false)
    private String primaryEmail;

    private String alternativeEmail;
    private String alternativeMobileNumber;
    private String taxId;
    private String businessId;
    private String preferredCurrency;
    private String invoicePrefix;

    @PrePersist
    public void prePersist() {
        if (this.primaryEmail == null && this.email != null) {
            this.primaryEmail = this.email;
        }
        if (this.fullName == null) {
            this.fullName = String.join(" ",
                    firstName != null ? firstName : "",
                    middleName != null ? middleName : "",
                    lastName != null ? lastName : ""
            ).trim();
        }
    }
}
