package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_info") // removed trailing space
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

    @Column(nullable = false)
    private String primaryEmail;

    private String alternativeEmail;
    private String alternativeMobileNumber;
    private String taxId;
    private String businessId;
    private String prefferedCurrency;
    private String invoicePrefix;

    //  Auto-fill before insert
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
