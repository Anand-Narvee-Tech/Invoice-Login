package com.example.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.DTO.ManageUserDTO;
import com.example.entity.ManageUsers;

public interface ManageUserService {

    ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail);

    ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail);

    void deleteUser(Long id, String loggedInEmail);

    List<ManageUserDTO> getAllUsers(String loggedInEmail);

    ManageUserDTO getById(Long id);

    // 🆕 Additional methods for isolation-based access
    ManageUserDTO getByEmail(String email);

    ManageUserDTO getByIdAndLoggedInUser(Long id, String loggedInEmail);
    
    public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(String loggedInEmail, int page, int size,String sortField,  String sortDir, String keyword) ;
}
