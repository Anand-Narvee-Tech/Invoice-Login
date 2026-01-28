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
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ManageUsers {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String firstName;
	private String middleName;
	private String lastName;

	// 🔥 REQUIRED FIELD
	@Column(name = "company_domain", nullable = false)
	@jakarta.validation.constraints.NotBlank(message = "Company domain is mandatory")
	private String companyDomain;

	@Column(unique = true, nullable = false)
	@jakarta.validation.constraints.NotBlank(message = "Email is mandatory")
	private String email;

	private String primaryEmail;

	@Column(name = "role_name")
	private String roleName;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "roleid")
	private Role role;

	private Long updatedBy;

	@Column(name = "added_by_name")
	private String addedByName;

	@Column(name = "updated_by_name")
	private String updatedByName;

	// 🔥 REQUIRED FIELD
	@Column(name = "full_name", nullable = false)
	private String fullName;

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

	@Column(name = "approved")
	private Boolean approved = false;

	@Column(name = "active")
	private Boolean active = true;

	/* -------------------- Lifecycle hooks -------------------- */

	@PrePersist
	@PreUpdate
	public void prePersistUpdate() {

		// 🔥 HARD STOP (prevents DB crash)
		if (companyDomain == null || companyDomain.isBlank()) {
			throw new IllegalStateException("companyDomain must not be null or blank");
		}

		this.firstName = capitalize(this.firstName);
		this.middleName = capitalize(this.middleName);
		this.lastName = capitalize(this.lastName);

		if (this.fullName == null || this.fullName.isBlank()) {
			this.fullName = computeFullName();
		}

		if (this.fullName == null || this.fullName.isBlank()) {
			throw new IllegalStateException("fullName must not be null or blank");
		}
	}

	/* -------------------- Helpers -------------------- */

	public String computeFullName() {
		StringBuilder sb = new StringBuilder();
		if (firstName != null && !firstName.isBlank())
			sb.append(firstName.trim());
		if (middleName != null && !middleName.isBlank())
			sb.append(" ").append(middleName.trim());
		if (lastName != null && !lastName.isBlank())
			sb.append(" ").append(lastName.trim());
		return sb.toString().trim();
	}

	private String capitalize(String value) {
		if (value == null || value.isBlank())
			return value;
		value = value.trim();
		return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
	}
}
