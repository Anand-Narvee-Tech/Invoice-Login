package com.example.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleid")
    private Long roleId;

    @Column(name = "rolename", nullable = false, unique = true)
    private String roleName;

    private String description;

    @Column(name = "status")
    private String status; // Active / Inactive

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "addedby")
    private Long addedBy;

    @Column(name = "updatedby")
    private Long updatedBy;

    private String addedByName;
    private String updatedByName;

    @Column(name = "createddate")
    private LocalDateTime createdDate;

    @Column(name = "updateddate")
    private LocalDateTime updatedDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_privileges",
        joinColumns = @JoinColumn(name = "roleid"),
        inverseJoinColumns = @JoinColumn(name = "privilegeid")
    )
    private Set<Privilege> privileges;

    @PrePersist
    public void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.status = "Active";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}