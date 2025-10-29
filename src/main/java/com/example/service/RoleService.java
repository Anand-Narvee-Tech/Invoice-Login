package com.example.service;


import java.util.List;
import java.util.Set;

import com.example.DTO.RoleDTO;

public interface RoleService {

    RoleDTO createRole(RoleDTO roleDTO);

    RoleDTO updateRole(Long roleId, RoleDTO roleDTO, String loggedInEmail);

    void deleteRole(Long roleId);

    RoleDTO assignPrivilegeToRole(Long roleId, Long privilegeId, Long adminId);

    List<RoleDTO> getAllRoles();

    RoleDTO getRoleById(Long id);

    // ✅ Updated signature
    RoleDTO updateRolePrivileges(Long roleId, Set<Long> selectedPrivilegeIds, String category);
}
