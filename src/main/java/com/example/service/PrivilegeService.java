package com.example.service;

import java.util.List;
import java.util.Map;

import com.example.entity.Privilege;

public interface PrivilegeService {
    
	 Privilege createPrivilege(Privilege privilege);

	    Privilege updatePrivilege(Long id, Privilege privilege);

	    void deletePrivilege(Long id);

	    List<Privilege> getAllPrivileges();

	    Privilege getPrivilegeById(Long id);

	    List<Privilege> getPrivilegesByCategory(String category);

	    Map<String, List<Privilege>> getAllPrivilegesGrouped();

	    Map<String, List<Privilege>> getPrivilegesByRole(Long roleId);
}
