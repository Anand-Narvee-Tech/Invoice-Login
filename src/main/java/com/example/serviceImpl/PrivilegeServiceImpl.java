package com.example.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.PrivilegeDTO;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.service.PrivilegeService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class PrivilegeServiceImpl implements PrivilegeService {
	
	@PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public PrivilegeDTO createPrivilege(PrivilegeDTO dto) {
        Privilege privilege = Privilege.builder()
                .name(dto.getName())
                .cardType(dto.getCardType())
                .status(dto.getStatus())
                .category(dto.getCategory())
                .build();
        Privilege saved = privilegeRepository.save(privilege);
        return convertToDTO(saved);
    }

    @Override
    public PrivilegeDTO updatePrivilege(Long id, PrivilegeDTO dto) {
        Privilege privilege = privilegeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Privilege not found with ID: " + id));

        privilege.setName(dto.getName());
        privilege.setCardType(dto.getCardType());
        privilege.setStatus(dto.getStatus());
        privilege.setCategory(dto.getCategory());

        Privilege updated = privilegeRepository.save(privilege);
        return convertToDTO(updated);
    }

    // 🟢 Safe Delete Privilege
    @Override
    @Transactional
    public void deletePrivilege(Long id) {
        try {
            Privilege privilege = privilegeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Privilege not found with ID: " + id));

            // ✅ Unlink from all roles first
            Set<Role> linkedRoles = new HashSet<>(privilege.getRoles());
            for (Role role : linkedRoles) {
                role.getPrivileges().remove(privilege);
                roleRepository.save(role);
            }

            // ✅ Explicitly clear the join table (prevents constraint issues)
            entityManager.createNativeQuery("DELETE FROM role_privileges WHERE privilegeid = :pid")
                    .setParameter("pid", id)
                    .executeUpdate();

            // ✅ Delete the privilege entity itself
            privilegeRepository.delete(privilege);
            privilegeRepository.flush();

            // ✅ Clear Hibernate persistence context (forces fresh state)
            entityManager.clear();

        } catch (Exception e) {
            throw new RuntimeException("Error deleting privilege ID " + id + ": " + e.getMessage(), e);
        }
    }


    @Override
    public List<PrivilegeDTO> getAllPrivileges() {
        return privilegeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PrivilegeDTO getPrivilegeById(Long id) {
        return privilegeRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Privilege not found with ID: " + id));
    }

    @Override
    public List<PrivilegeDTO> getPrivilegesByCategory(String category) {
        return privilegeRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<PrivilegeDTO>> getAllPrivilegesGrouped() {
        List<Privilege> privileges = privilegeRepository.findAll();

        return privileges.stream()
                .collect(Collectors.groupingBy(
                        Privilege::getCategory,
                        Collectors.mapping(this::convertToDTO, Collectors.toList())
                ));
    }

    @Override
    public Map<String, List<PrivilegeDTO>> getPrivilegesByRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));

        Map<String, List<PrivilegeDTO>> grouped = new HashMap<>();

        List<Privilege> allPrivileges = privilegeRepository.findAll();

        allPrivileges.forEach(privilege -> {
            String category = privilege.getCategory();
            grouped.putIfAbsent(category, new ArrayList<>());

            boolean selected = role.getPrivileges().contains(privilege);

            grouped.get(category).add(
                    PrivilegeDTO.builder()
                            .id(privilege.getId())
                            .name(privilege.getName())
                            .cardType(privilege.getCardType())
                            .selected(selected)
                            .status(privilege.getStatus())
                            .category(category)
                            .build()
            );
        });

        return grouped;
    }

    @Override
    public Map<String, String> getEndpointPrivilegesMap() {
        return Collections.emptyMap(); // implement later if needed
    }

    private PrivilegeDTO convertToDTO(Privilege privilege) {
        return PrivilegeDTO.builder()
                .id(privilege.getId())
                .name(privilege.getName())
                .cardType(privilege.getCardType())
                .selected(false)
                .status(privilege.getStatus())
                .category(privilege.getCategory())
                .build();
    }
}
