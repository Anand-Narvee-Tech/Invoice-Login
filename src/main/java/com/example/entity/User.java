package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_info")
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

    // This is the string representation (e.g. ADMIN, ACCOUNTANT, USER)
    @Column(name = "user_role")
    private String userRole;

    //  This is the link to the Role table (foreign key)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false)
    private String primaryEmail;

    private String alternativeEmail;
    private String alternativeMobileNumber;
    private String taxId;
    private String businessId;
    private String prefferedCurrency;
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
