package com.example.serviceImpl;

import com.example.DTO.PrivilegeDTO;
import com.example.DTO.RoleDTO;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.service.RoleService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    // ✅ Create Role
    @Override
    public RoleDTO createRole(RoleDTO roleDTO) {
        Role role = convertToEntity(roleDTO);
        Role saved = roleRepository.save(role);
        return convertToDTO(saved);
    }

    // ✅ Update Role
    @Override
    public RoleDTO updateRole(Long roleId, RoleDTO roleDTO, String loggedInEmail) {
        User currentUser = userRepository.findByEmailIgnoreCase(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + loggedInEmail));

        Role existing = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        existing.setRoleName(roleDTO.getRoleName());
        existing.setDescription(roleDTO.getDescription());
        existing.setStatus(roleDTO.getStatus());

        if (existing.getAddedBy() == null) {
            existing.setAddedBy(currentUser.getId());
            existing.setAddedByName(currentUser.getFullName());
        }

        existing.setUpdatedBy(currentUser.getId());
        existing.setUpdatedByName(currentUser.getFullName());

        Role updated = roleRepository.save(existing);
        return convertToDTO(updated);
    }

    // ✅ Assign a single privilege to a role
    @Override
    public RoleDTO assignPrivilegeToRole(Long roleId, Long privilegeId, Long creatorId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        Privilege privilege = privilegeRepository.findById(privilegeId)
                .orElseThrow(() -> new RuntimeException("Privilege not found"));

        if (role.getPrivileges() == null) {
            role.setPrivileges(new HashSet<>());
        }

        role.getPrivileges().add(privilege);
        Role updated = roleRepository.save(role);
        return convertToDTO(updated);
    }

    // ✅ Get all roles
    @Override
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ Get role by ID
    @Override
    public RoleDTO getRoleById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        return convertToDTO(role);
    }

    // ✅ Update privileges of a role
    @Transactional
    @Override
    public RoleDTO updateRolePrivileges(Long roleId, Set<Long> selectedPrivilegeIds, String category) {
        log.info("🔹 Updating privileges for Role ID: {} and Category: {}", roleId, category);
        log.info("🔹 Selected privilege IDs: {}", selectedPrivilegeIds);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));

        // 🔹 Get currently assigned privileges
        Set<Privilege> currentPrivileges = new HashSet<>(role.getPrivileges());
        log.info("🔹 Current privileges count: {}", currentPrivileges.size());

        // 🔹 Fetch all privileges in this category
        Set<Privilege> categoryPrivileges = privilegeRepository.findByCategory(category);
        log.info("🔹 Found {} privileges in category '{}'", categoryPrivileges.size(), category);

        // 🔹 Fetch privileges selected by user
        Set<Privilege> selectedPrivileges = privilegeRepository.findAllById(selectedPrivilegeIds)
                .stream().collect(Collectors.toSet());

        // 🔹 Remove unchecked privileges only from this category
        currentPrivileges.removeIf(p ->
                categoryPrivileges.contains(p) && !selectedPrivilegeIds.contains(p.getId()));

        // 🔹 Add newly selected privileges
        currentPrivileges.addAll(selectedPrivileges);

        // 🔹 Save and update
        role.setPrivileges(currentPrivileges);
        Role updatedRole = roleRepository.save(role);

        log.info("✅ Updated privileges for Role '{}'. Total privileges now: {}",
                updatedRole.getRoleName(), updatedRole.getPrivileges().size());

        return mapToDTO(updatedRole);
    }

    // ✅ Delete Role (safe delete)
    @Override
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        long assignedCount = userRepository.countByRole_RoleId(roleId);
        if (assignedCount > 0) {
            throw new RuntimeException("Cannot delete role — it is still assigned to " + assignedCount + " user(s).");
        }

        // Remove privileges association before deleting
        role.getPrivileges().clear();
        roleRepository.delete(role);
    }

    // ==============================
    // 🔸 Helper Methods (DTO Mapping)
    // ==============================

    private RoleDTO convertToDTO(Role role) {
        Set<PrivilegeDTO> privilegeDTOs = role.getPrivileges() != null
                ? role.getPrivileges().stream()
                .map(p -> new PrivilegeDTO(
                        p.getId(),
                        p.getName(),
                        p.getCardType(),
                        true,
                        p.getStatus(),
                        p.getCategory()
                ))
                .collect(Collectors.toSet())
                : new HashSet<>();

        return RoleDTO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .addedBy(role.getAddedBy())
                .addedByName(role.getAddedByName())
                .updatedBy(role.getUpdatedBy())
                .updatedByName(role.getUpdatedByName())
                .createdDate(role.getCreatedDate())
                .updatedDate(role.getUpdatedDate())
                .privileges(privilegeDTOs)
                .build();
    }

    private Role convertToEntity(RoleDTO dto) {
        Role role = new Role();
        role.setRoleId(dto.getRoleId());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());

        if (dto.getPrivileges() != null) {
            Set<Privilege> privileges = dto.getPrivileges().stream()
                    .map(p -> privilegeRepository.findById(p.getId())
                            .orElseThrow(() -> new RuntimeException("Privilege not found with id: " + p.getId())))
                    .collect(Collectors.toSet());
            role.setPrivileges(privileges);
        }
        return role;
    }

    // ✅ Alternative mapping method used after updates
    private RoleDTO mapToDTO(Role role) {
        return RoleDTO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .addedBy(role.getAddedBy())
                .addedByName(role.getAddedByName())
                .updatedBy(role.getUpdatedBy())
                .updatedByName(role.getUpdatedByName())
                .createdDate(role.getCreatedDate())
                .updatedDate(role.getUpdatedDate())
                .privileges(
                        role.getPrivileges() != null
                                ? role.getPrivileges().stream()
                                .map(p -> PrivilegeDTO.builder()
                                        .id(p.getId())
                                        .name(p.getName())
                                        .cardType(p.getCardType())
                                        .selected(true)
                                        .status(p.getStatus())
                                        .category(p.getCategory())
                                        .build())
                                .collect(Collectors.toSet())
                                : Collections.emptySet()
                )
                .build();
    }


}
