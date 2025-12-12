package com.example.service;


import com.example.DTO.PrivilegeDTO;


import java.util.List;
import java.util.Map;

public interface PrivilegeService {

    // Basic CRUD
    PrivilegeDTO createPrivilege(PrivilegeDTO privilegeDTO);

    PrivilegeDTO updatePrivilege(Long id, PrivilegeDTO privilegeDTO);

    public void deletePrivilege(Long id);

    // Fetch operations
    List<PrivilegeDTO> getAllPrivileges();

    PrivilegeDTO getPrivilegeById(Long id);

    List<PrivilegeDTO> getPrivilegesByCategory(String category);

    Map<String, List<PrivilegeDTO>> getAllPrivilegesGrouped();

    Map<String, List<PrivilegeDTO>> getPrivilegesByRole(Long roleId);

    Map<String, String> getEndpointPrivilegesMap();
    public void deletePrivilegesByCategoryId(Long categoryId);
}
