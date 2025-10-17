package com.example.service;

import java.util.List;
import java.util.Set;

import com.example.entity.Role;

public interface RoleService {
	
	Role createRole(Role role);
    Role updateRole(Long roleId, Role role);
    void deleteRole(Long roleId);
    Role assignPrivilegeToRole(Long roleId, Long privilegeId, Long adminId);
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    
    Role updateRolePrivileges(Long roleId, Set<Long> privilegeIds);
}
