package com.example.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "privilegeid")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name; // e.g. CREATE_TASK, DELETE_PROJECT

    @Column(name = "cardType")
    private String cardType; // e.g. one, two

    @Column(name = "selected")
    private boolean selected = false;

    @Column(name = "status")
    private String status; // Active / Inactive

    @Column(name = "category") 
    private String category; // e.g. tasks, teamMember, projects

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

    // reverse mapping for roles
    @ManyToMany(mappedBy = "privileges", fetch = FetchType.LAZY)
    private Set<Role> roles ;

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
