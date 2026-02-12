package com.example.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.example.DTO.ManageUserDTO;
import com.example.DTO.SortingRequestDTO;
import com.example.DTO.UserUpdateRequest;
import com.example.entity.ManageUsers;
import com.example.entity.User;

public interface ManageUserService {

	// Create a new ManageUser
	ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail);

	// Update ManageUser
	ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail);

	// Delete ManageUser
	void deleteUser(Long id, String loggedInEmail);

	// Get all users (list)
	List<ManageUserDTO> getAllUsers(String loggedInEmail);

	// Get user by ID
	ManageUserDTO getById(Long id);

	// Get user by email
	ManageUserDTO getByEmail(String email);

	// Get user by ID with respect to logged-in user permissions
	ManageUserDTO getByIdAndLoggedInUser(Long id, String loggedInEmail);

	// Pagination + Search
	public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(int page, int size, String sortField, String sortDir,
			String keyword);

	// Update user profile
	User updateUserProfile(UserUpdateRequest request, MultipartFile profileImage, String loggedInEmail);

	// Upload profile file
	String uploadFile(MultipartFile file, Long userId) throws IOException;

	// Bhagi

	User updateUserProfileDynamic(UserUpdateRequest request);

	UserUpdateRequest mapToDto(User user);

	// Bhagi

	/** ================= UPDATE USER PROFILE ================= **/

	User updateUserProfile(UserUpdateRequest request, String loggedInEmail);

	// Bhargav

//    Page<ManageUserDTO> getAllManageUsersWithSorting(SortingRequestDTO sortingRequestDTO);

	Page<ManageUserDTO> getAllManageUsersWithSorting(SortingRequestDTO sortingRequestDTO, String loggedInEmail);

	// Bhargav

}
