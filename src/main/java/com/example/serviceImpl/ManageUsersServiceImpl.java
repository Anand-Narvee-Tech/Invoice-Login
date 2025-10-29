package com.example.serviceImpl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.DTO.ManageUserDTO;
import com.example.entity.AuditLog;
import com.example.entity.ManageUsers;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.AuditLogRepository;
import com.example.repository.ManageUserRepository;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.service.ManageUserService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ManageUsersServiceImpl implements ManageUserService {

    private final ManageUserRepository manageUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    
    private static final Logger log = LoggerFactory.getLogger(ManageUsersServiceImpl.class);

    /** ================= FETCH LOGGED-IN USER ================= **/
    private User getCurrentLoggedInUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + email));
    }

    /** ================= CONVERT ENTITY TO DTO ================= **/
    private ManageUserDTO convertToDTO(ManageUsers entity) {
        return ManageUserDTO.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .middleName(entity.getMiddleName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .roleName(entity.getRoleName())
                .addedBy(entity.getAddedBy() != null ? entity.getAddedBy().getId().toString() : null)
                .addedByName(entity.getAddedBy() != null ? entity.getAddedBy().getFullName() : "SYSTEM")
                .updatedBy(entity.getUpdatedBy())
                .updatedByName(entity.getUpdatedByName())
                .build();
    }

    /** ================= CREATE USER ================= **/
    @Override
    public ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        String currentUserRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        // Permission checks
        if (!List.of("SUPERADMIN", "ADMIN").contains(currentUserRole.toUpperCase())) {
            throw new RuntimeException("You do not have permission to create users");
        }
        if ("ADMIN".equalsIgnoreCase(currentUserRole) &&
                "SUPERADMIN".equalsIgnoreCase(manageUsers.getRoleName())) {
            throw new RuntimeException("ADMIN cannot create SUPERADMIN");
        }
        if (manageUserRepository.existsByEmailIgnoreCase(manageUsers.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (manageUsers.getRoleName() != null) {
            manageUsers.setRoleName(manageUsers.getRoleName().toUpperCase());
        }

        // ✅ Set audit fields
        manageUsers.setAddedBy(currentUser);                        // User entity
        manageUsers.setCreatedBy(currentUser);                      // User entity
        manageUsers.setUpdatedBy(currentUser.getId());             // ID as Long
        manageUsers.setUpdatedByName(buildFullName(currentUser));  // Full name as String

        ManageUsers saved = manageUserRepository.save(manageUsers);

        // Linked User entity creation/update
        userRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresentOrElse(u -> {
            if (u.getCreatedBy() == null) u.setCreatedBy(currentUser);
            Role role = roleRepository.findByRoleNameIgnoreCase(saved.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + saved.getRoleName()));
            u.setRole(role);
            userRepository.save(u);
        }, () -> {
            User user = new User();
            user.setEmail(saved.getEmail());
            user.setFirstName(saved.getFirstName());
            user.setMiddleName(saved.getMiddleName());
            user.setLastName(saved.getLastName());
            user.setFullName(buildFullName(saved));
            user.setApproved(true);
            user.setActive(true);
            user.setCreatedBy(currentUser);

            Role role = roleRepository.findByRoleNameIgnoreCase(saved.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + saved.getRoleName()));
            user.setRole(role);
            userRepository.save(user);
        });

        return convertToDTO(saved);
    }


    /** ================= UPDATE USER ================= **/
    @Override
    public ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        ManageUsers existing = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existing.setFirstName(manageUsers.getFirstName());
        existing.setMiddleName(manageUsers.getMiddleName());
        existing.setLastName(manageUsers.getLastName());
        existing.setEmail(manageUsers.getEmail());
        if (manageUsers.getRoleName() != null) {
            existing.setRoleName(manageUsers.getRoleName().toUpperCase());
        }

       
        existing.setUpdatedBy(currentUser.getId());
        existing.setUpdatedByName(buildFullName(currentUser));

        ManageUsers saved = manageUserRepository.save(existing);

        // ✅ Save Audit Log
        AuditLog audit = AuditLog.builder()
                .action("UPDATE")
                .entityName("ManageUsers")
                .entityId(saved.getId())
                .performedBy(buildFullName(currentUser))
                .performedById(currentUser.getId())
                .email(currentUser.getEmail())
                .timestamp(LocalDateTime.now())
                .details("User '" + buildFullName(currentUser) + "' updated ManageUser ID: " + saved.getId())
                .build();
        auditLogRepository.save(audit);

        // ✅ Log to console/log file
        log.info("User '{}' updated ManageUser record ID: {}", audit.getPerformedBy(), audit.getEntityId());

        userRepository.findByEmailIgnoreCase(existing.getEmail()).ifPresent(user -> {
            user.setFirstName(existing.getFirstName());
            user.setMiddleName(existing.getMiddleName());
            user.setLastName(existing.getLastName());
            user.setFullName(buildFullName(user));
            userRepository.save(user);
        });

        return convertToDTO(saved);
    }
    /** ================= DELETE USER ================= **/
	    @Override
	    public void deleteUser(Long id, String loggedInEmail) {
	        // 1. Get current logged-in user
	        User currentUser = getCurrentLoggedInUser(loggedInEmail);

	        // 2. Find the target ManageUser record
	        ManageUsers manageUser = manageUserRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("User not found"));

	        // 3. Prevent ADMIN from deleting SUPERADMIN
	        if ("ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName()) &&
	                "SUPERADMIN".equalsIgnoreCase(manageUser.getRoleName())) {
	            throw new RuntimeException("ADMIN cannot delete SUPERADMIN");
	        }

	        // 4. Check if current user has DELETE_MANAGE_USERS privilege
	        boolean hasDeletePrivilege = currentUser.getRole().getPrivileges().stream()
	                .anyMatch(p -> "DELETE_MANAGE_USERS".equalsIgnoreCase(p.getName()));

	        if (!hasDeletePrivilege) {
	            throw new RuntimeException("You do not have DELETE_MANAGE_USERS privilege");
	        }

	        // 5. Proceed with deletion
	        userRepository.findByEmailIgnoreCase(manageUser.getEmail())
	                .ifPresent(userRepository::delete);

	        manageUserRepository.deleteById(id);
	    }
                                 

    /** ================= GET ALL USERS ================= **/
    @Override
    public List<ManageUserDTO> getAllUsers(String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        List<ManageUsers> users;
        if ("SUPERADMIN".equalsIgnoreCase(roleName)) {
            users = manageUserRepository.findAll();
        } else if ("ADMIN".equalsIgnoreCase(roleName)) {
            users = manageUserRepository.findAll().stream()
                    .filter(u -> !"SUPERADMIN".equalsIgnoreCase(u.getRoleName()))
                    .collect(Collectors.toList());
        } else {
            users = manageUserRepository.findByEmailIgnoreCase(currentUser.getEmail())
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }

        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /** ================= GET BY ID ================= **/
    @Override
    public ManageUserDTO getById(Long id) {
        ManageUsers entity = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(entity);
    }

    /** ================= GET BY EMAIL ================= **/
    @Override
    public ManageUserDTO getByEmail(String email) {
        ManageUsers entity = manageUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return convertToDTO(entity);
    }

    /** ================= GET BY ID & LOGGED-IN USER ================= **/
    @Override
    public ManageUserDTO getByIdAndLoggedInUser(Long id, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        ManageUsers targetUser = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        String role = currentUser.getRole().getRoleName();
        if ("SUPERADMIN".equalsIgnoreCase(role)) {
            return convertToDTO(targetUser);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            if ("SUPERADMIN".equalsIgnoreCase(targetUser.getRoleName())) {
                throw new RuntimeException("ADMIN cannot view SUPERADMIN data");
            }
            return convertToDTO(targetUser);
        } else if (targetUser.getEmail().equalsIgnoreCase(loggedInEmail)) {
            return convertToDTO(targetUser);
        } else {
            throw new RuntimeException("You can only view your own data");
        }
    }

    /** ================= HELPERS ================= **/
    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) sb.append(user.getFirstName().trim());
        if (user.getMiddleName() != null && !user.getMiddleName().isBlank())
            sb.append(" ").append(user.getMiddleName().trim());
        if (user.getLastName() != null && !user.getLastName().isBlank())
            sb.append(" ").append(user.getLastName().trim());
        return sb.length() > 0 ? sb.toString() : user.getEmail();
    }

    private String buildFullName(ManageUsers user) {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) sb.append(user.getFirstName().trim());
        if (user.getMiddleName() != null && !user.getMiddleName().isBlank())
            sb.append(" ").append(user.getMiddleName().trim());
        if (user.getLastName() != null && !user.getLastName().isBlank())
            sb.append(" ").append(user.getLastName().trim());
        return sb.length() > 0 ? sb.toString() : user.getEmail();
    }

	@Override
	public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(String loggedInEmail, int page, int size,
			String sortField, String sortDir, String keyword) {
		
		User currentUser = getCurrentLoggedInUser(loggedInEmail);
		String roleName = currentUser.getRole().getRoleName();
		
		Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending()
				                                                                      :  Sort.by(sortField).descending();
		Pageable pageable = PageRequest.of(page, size , sort);
		
		Page<ManageUsers> userPage;
		
		if(keyword != null && !keyword.isBlank()) {
			userPage = manageUserRepository.search(keyword, pageable);
		}else {
			userPage = manageUserRepository.findAll(pageable);
		}
		 List<ManageUsers> filteredUsers;
		    if ("SUPERADMIN".equalsIgnoreCase(roleName)) {
		        filteredUsers = userPage.getContent();
		    } else if ("ADMIN".equalsIgnoreCase(roleName)) {
		        filteredUsers = userPage.getContent().stream()
		                .filter(u -> !"SUPERADMIN".equalsIgnoreCase(u.getRoleName()))
		                .collect(Collectors.toList());
		    } else {
		        filteredUsers = userPage.getContent().stream()
		                .filter(u -> u.getEmail().equalsIgnoreCase(loggedInEmail))
		                .collect(Collectors.toList());
		    }

		    List<ManageUserDTO> dtoList = filteredUsers.stream()
		            .map(this::convertToDTO)
		            .collect(Collectors.toList());

		    return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
		}
		
	}

