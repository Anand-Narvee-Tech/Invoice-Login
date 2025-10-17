 package com.example.controller;

import java.util.List;
import java.util.Map;

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
import com.example.serviceImpl.PrivilegeServiceImpl;

@RestController
@RequestMapping("/auth/privileges")
public class PrivilegeController {
	
	 @Autowired
	    private PrivilegeServiceImpl privilegeServiceImpl;

	    @PostMapping("/save")
	    public ResponseEntity<RestAPIResponse> createPrivilege(@RequestBody Privilege privilege) {
	        try {
	            Privilege save = privilegeServiceImpl.createPrivilege(privilege);
	            return ResponseEntity.ok(new RestAPIResponse("success", "Privilege saved successfully", save));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to save privilege: " + e.getMessage(), null));
	        }
	    }

	    @GetMapping("/getall")
	    public ResponseEntity<RestAPIResponse> getAllPrivileges() {
	        try {
	            Map<String, List<Privilege>> groupedPrivileges = privilegeServiceImpl.getAllPrivilegesGrouped();
	            return ResponseEntity.ok(new RestAPIResponse("success", "Successfully fetched all Privileges", groupedPrivileges));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to fetch all privileges: " + e.getMessage(), null));
	        }
	    }

	    @GetMapping("/role/{roleId}")
	    public ResponseEntity<RestAPIResponse> getPrivilegesByRole(@PathVariable Long roleId) {
	        try {
	            Map<String, List<Privilege>> privileges = privilegeServiceImpl.getPrivilegesByRole(roleId);
	            return ResponseEntity.ok(new RestAPIResponse("success", "Successfully fetched privileges by role", privileges));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to fetch privileges by role: " + e.getMessage(), null));
	        }
	    }

	    @GetMapping("/{id}")
	    public ResponseEntity<RestAPIResponse> getPrivilegeById(@PathVariable Long id) {
	        try {
	            Privilege privilege = privilegeServiceImpl.getPrivilegeById(id);
	            return ResponseEntity.ok(new RestAPIResponse("success", "Privilege fetched successfully", privilege));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to fetch privilege: " + e.getMessage(), null));
	        }
	    }

	    @PutMapping("/{id}")
	    public ResponseEntity<RestAPIResponse> updatePrivilege(@PathVariable Long id, @RequestBody Privilege privilege) {
	        try {
	            Privilege updated = privilegeServiceImpl.updatePrivilege(id, privilege);
	            return ResponseEntity.ok(new RestAPIResponse("success", "Privilege updated successfully", updated));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to update privilege: " + e.getMessage(), null));
	        }
	    }

	    @DeleteMapping("/{id}")
	    public ResponseEntity<RestAPIResponse> deletePrivilege(@PathVariable Long id) {
	        try {
	            privilegeServiceImpl.deletePrivilege(id);
	            return ResponseEntity.ok(new RestAPIResponse("success", "Privilege deleted successfully", null));
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new RestAPIResponse("error", "Failed to delete privilege: " + e.getMessage(), null));
	        }
	    }
}
