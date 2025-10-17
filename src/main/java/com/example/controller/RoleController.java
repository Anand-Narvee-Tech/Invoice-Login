package com.example.controller;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.commons.RestAPIResponse;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.serviceImpl.RoleServiceImpl;

@RestController
@RequestMapping("/auth/roles")
public class RoleController {
	
	@Autowired
    private RoleServiceImpl roleServiceImpl;

    @PostMapping("/save")
    public ResponseEntity<RestAPIResponse> createRole(@RequestBody Role role) {
        try {
            Role saved = roleServiceImpl.createRole(role);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Role saved successfully", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to save Role: " + e.getMessage(), null));
        }
    }

    @PostMapping("/privilege/save")
    public ResponseEntity<RestAPIResponse> assignPrivilegesToRole(@RequestBody Map<String, Object> payload) {
        try {
            Long roleId = Long.parseLong(payload.get("roleId").toString());
            List<Integer> privilegeIds = (List<Integer>) payload.get("privilegeIds");

            Set<Long> privilegeIdSet = privilegeIds.stream()
                    .map(Integer::longValue)
                    .collect(Collectors.toSet());

            Role updatedRole = roleServiceImpl.updateRolePrivileges(roleId, privilegeIdSet);

            return ResponseEntity.ok(new RestAPIResponse("Success",
                    "Privileges assigned successfully", updatedRole));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to assign privileges: " + e.getMessage(), null));
        }
    }

    @GetMapping("/getall")
    public ResponseEntity<RestAPIResponse> getAll() {
        try {
            return ResponseEntity.ok(new RestAPIResponse("Success", "All roles retrieved successfully",roleServiceImpl.getAllRoles()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to retrieve roles: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RestAPIResponse> getRoleById(@PathVariable Long roleId) {
        try {
            return ResponseEntity.ok(new RestAPIResponse("Success", "Role retrieved successfully",
                    roleServiceImpl.getRoleById(roleId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to retrieve Role: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/{roleId}/privileges")
    public ResponseEntity<RestAPIResponse> getPrivilegesByRole(@PathVariable Long roleId) {
        try {
            Role role = roleServiceImpl.getRoleById(roleId);
            Set<Privilege> privileges = role.getPrivileges();
            return ResponseEntity.ok(new RestAPIResponse("Success", "Fetched privileges for role", privileges));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to fetch privileges: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RestAPIResponse> updateRole(@PathVariable Long roleId, @RequestBody Role role) {
        try {
            Role updatedRole = roleServiceImpl.updateRole(roleId, role);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Role updated successfully", updatedRole));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestAPIResponse("Error", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to update Role: " + e.getMessage(), null));
        }
    }


    @PutMapping("/{roleId}/privileges")
    public ResponseEntity<RestAPIResponse> updateRolePrivileges(@PathVariable Long roleId, @RequestBody Set<Long> privilegeIds) {
        try {
            Role role = roleServiceImpl.updateRolePrivileges(roleId, privilegeIds);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Privileges updated successfully", role));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to update privileges: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<RestAPIResponse> deleteRoleById(@PathVariable Long roleId) {
        try {
            roleServiceImpl.deleteRole(roleId);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Role deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to delete Role: " + e.getMessage(), null));
        }
    }
}