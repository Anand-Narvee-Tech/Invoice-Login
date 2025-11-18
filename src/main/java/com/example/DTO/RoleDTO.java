package com.example.DTO;


import java.time.LocalDateTime;
import java.util.Set;

import com.example.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {
    private Long roleId;
    private String roleName;
    private String description;
    private String status;
    private Long addedBy;
    private String addedByName;
    private Long updatedBy;
    private String updatedByName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private Set<PrivilegeDTO> privileges;
}
