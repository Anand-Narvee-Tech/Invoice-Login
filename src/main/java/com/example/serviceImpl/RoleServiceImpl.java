package com.example.serviceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.service.RoleService;


@Service
public class RoleServiceImpl implements RoleService{

	@Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Long roleId, Role role) {
        Role existing = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        existing.setRoleName(role.getRoleName());
        existing.setDescription(role.getDescription());
        existing.setUpdatedBy(role.getUpdatedBy());
        existing.setUpdatedByName(role.getUpdatedByName());
        existing.setStatus(role.getStatus());

        return roleRepository.save(existing);
    }

    @Override
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    @Override
    public Role assignPrivilegeToRole(Long roleId, Long privilegeId, Long creatorId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        // Fetch privilege
        Privilege privilege = privilegeRepository.findById(privilegeId)
                .orElseThrow(() -> new RuntimeException("Privilege not found"));

        // Add it safely
        if (role.getPrivileges() == null) {
            role.setPrivileges(new HashSet<>());
        }

        role.getPrivileges().add(privilege);
        return roleRepository.save(role);
    }
    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Override
    public Role updateRolePrivileges(Long roleId, Set<Long> privilegeIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<Privilege> privileges = privilegeIds.stream()
                .map(id -> privilegeRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Privilege not found with ID: " + id)))
                .collect(Collectors.toSet());

        role.setPrivileges(privileges);
        return roleRepository.save(role);
    }
}