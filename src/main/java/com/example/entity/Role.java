package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"privileges"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "roleId")
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "roleid")
    private Long roleId;

    @Column(name = "rolename", nullable = false)
    private String roleName;

    private String description;
    private String status;
    private Long adminId;
    private Long addedBy;
    private Long updatedBy;
    private String addedByName;
    private String updatedByName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_privileges",
        joinColumns = @JoinColumn(name = "roleid"),
        inverseJoinColumns = @JoinColumn(name = "privilegeid")
    )
    private Set<Privilege> privileges = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) this.status = "Active";

        if (this.roleName == null || this.roleName.isBlank()) {
            throw new IllegalStateException("roleName cannot be null or blank!");
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
