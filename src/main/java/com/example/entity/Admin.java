package com.example.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "updated_profile")
public class Admin {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    private String fullName;

	   @Column(unique = true, nullable = false)
	    private String primaryEmail;
	    private String alternativeEmail;
	    private String mobileNumber;
	    private String alternativeMobileNumber;
	    private String companyName;
        private String taxId;	    
	    private String businessId;
	    private String prefferedCurrency;
	    private String invoicePrefix;
//	    private String country;
//	    private String zipCode;
//	    private String linkedinProfile;
//	    private String profileImageUrl;
//
//	    private LocalDateTime createdAt;
//	    private LocalDateTime updatedAt;

//	    // OTP fields
//	    private String otp;
//	    private LocalDateTime otpGeneratedTime;

//	    @PrePersist
//	    public void onCreate() {
//	        this.createdAt = LocalDateTime.now();
//	    }
//
//	    @PreUpdate
//	    public void onUpdate() {
//	        this.updatedAt = LocalDateTime.now();
//	    }

}
