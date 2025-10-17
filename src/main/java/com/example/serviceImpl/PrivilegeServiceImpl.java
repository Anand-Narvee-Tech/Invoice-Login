package com.example.serviceImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.service.PrivilegeService;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {

	@Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Privilege createPrivilege(Privilege privilege) {
        return privilegeRepository.save(privilege);
    }

    @Override
    public Privilege updatePrivilege(Long id, Privilege privilege) {
        Privilege existing = privilegeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Privilege not found"));

        existing.setName(privilege.getName());
        existing.setCardType(privilege.getCardType());
        existing.setSelected(privilege.isSelected());
        existing.setStatus(privilege.getStatus());
        existing.setCategory(privilege.getCategory());
        existing.setUpdatedBy(privilege.getUpdatedBy());
        existing.setUpdatedByName(privilege.getUpdatedByName());

        return privilegeRepository.save(existing);
    }

    @Override
    public void deletePrivilege(Long id) {
        privilegeRepository.deleteById(id);
    }

    @Override
    public List<Privilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Override
    public Privilege getPrivilegeById(Long id) {
        return privilegeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Privilege not found"));
    }

    @Override
    public List<Privilege> getPrivilegesByCategory(String category) {
        return privilegeRepository.getPrivilegesByCategory(category);
    }

    /**
     * Group all privileges by their category.
     */
    @Override
    public Map<String, List<Privilege>> getAllPrivilegesGrouped() {
        List<Privilege> privileges = privilegeRepository.findAll();
        return privileges.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Uncategorized",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }


    /**
     * Fetch all privileges for a role (selected=true).
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Privilege>> getPrivilegesByRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Ensure privileges are fetched eagerly
        Set<Long> assignedPrivilegeIds = role.getPrivileges().stream()
                .map(Privilege::getId)
                .collect(Collectors.toSet());

        List<Privilege> allPrivileges = privilegeRepository.findAll();

        // Mark selected ones
        allPrivileges.forEach(p -> {
            boolean isSelected = assignedPrivilegeIds.contains(p.getId());
            p.setSelected(isSelected);
        });

        // Group by category, handle nulls safely
        return allPrivileges.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Uncategorized",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
