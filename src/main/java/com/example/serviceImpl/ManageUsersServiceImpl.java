package com.example.serviceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.DTO.ManageUserDTO;
import com.example.DTO.UserUpdateRequest;
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

    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Autowired
    private UserNameSyncServiceImpl userNameSyncServiceImpl;

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
        String fullName = entity.getFullName() != null && !entity.getFullName().isBlank()
                ? entity.getFullName().trim().replaceAll("\\s+", " ")
                : buildFullName(entity);

        String addedByName = entity.getAddedByName() != null ? entity.getAddedByName() : "SYSTEM";

        String updatedByName = entity.getUpdatedByName(); // already stored
        if (updatedByName == null && entity.getUpdatedBy() != null) {
            Optional<User> updatedByUser = userRepository.findById(entity.getUpdatedBy());
            updatedByName = updatedByUser.map(this::buildFullName).orElse(null);
        }

        return ManageUserDTO.builder()
                .id(entity.getId())
                .fullName(fullName)
                .firstName(entity.getFirstName())
                .middleName(entity.getMiddleName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .primaryEmail(entity.getPrimaryEmail())
                .roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
                .addedBy(entity.getAddedBy() != null ? entity.getAddedBy().getId().toString() : null)
                .addedByName(addedByName)
                .updatedBy(entity.getUpdatedBy())
                .updatedByName(updatedByName)
                .build();
    }
    /** ================= BUILD FULL NAME ================= **/
	    private String buildFullName(ManageUsers user) {
	        return Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
	                .filter(s -> s != null && !s.isBlank())
	                .collect(Collectors.joining(" "));
	    }
	
	    private String buildFullName(User user) {
	        return Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
	                .filter(s -> s != null && !s.isBlank())
	                .collect(Collectors.joining(" "));
	    }

    /** ================= CREATE USER ================= **/
	    @Override
	    @Transactional
	    public ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail) {

	        // 1️⃣ Get current logged-in user
	        User currentUser = getCurrentLoggedInUser(loggedInEmail);
	        String currentUserRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

	        // 2️⃣ Role permission checks
	        if (!List.of("SUPERADMIN", "ADMIN").contains(currentUserRole.toUpperCase())) {
	            throw new RuntimeException("You do not have permission to create users");
	        }
	        if ("ADMIN".equalsIgnoreCase(currentUserRole) &&
	                "SUPERADMIN".equalsIgnoreCase(manageUsers.getRoleName())) {
	            throw new RuntimeException("ADMIN cannot create SUPERADMIN");
	        }

	        // 3️⃣ Email duplication check
	        if (manageUserRepository.existsByEmailIgnoreCase(manageUsers.getEmail())) {
	            throw new RuntimeException("Email already exists");
	        }

	        // 4️⃣ Fetch Role entity and set in ManageUsers
	        Role role = roleRepository.findByRoleNameIgnoreCase(manageUsers.getRoleName())
	                .orElseThrow(() -> new RuntimeException("Role not found: " + manageUsers.getRoleName()));
	        manageUsers.setRole(role);                       // ✅ important: set entity
	        manageUsers.setRoleName(role.getRoleName());     // optional: keep string if needed

	        // 5️⃣ Set audit fields
	        manageUsers.setAddedBy(currentUser);
	        manageUsers.setAddedByName(buildFullName(currentUser));
	        manageUsers.setCreatedBy(currentUser);

	        // 6️⃣ Handle fullName
	        if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
	            manageUsers.setFullName(manageUsers.getFullName().trim());
	        } else {
	            manageUsers.setFullName(buildFullName(manageUsers)); // fallback if empty
	        }

	        // 7️⃣ Save ManageUsers entity
	        ManageUsers saved = manageUserRepository.save(manageUsers);

	        // 8️⃣ Sync to User table
	        userRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresentOrElse(u -> {
	            if (u.getCreatedBy() == null) u.setCreatedBy(currentUser);
	            u.setRole(role);                            // ✅ set role entity
	            u.setFullName(saved.getFullName());
	            u.setPrimaryEmail(saved.getPrimaryEmail());
	            userRepository.save(u);
	        }, () -> {
	            User user = new User();
	            user.setEmail(saved.getEmail());
	            user.setFirstName(saved.getFirstName());
	            user.setMiddleName(saved.getMiddleName());
	            user.setLastName(saved.getLastName());
	            user.setFullName(saved.getFullName());
	            user.setPrimaryEmail(saved.getPrimaryEmail());
	            user.setApproved(true);
	            user.setActive(true);
	            user.setCreatedBy(currentUser);
	            user.setRole(role);                          // ✅ set role entity
	            userRepository.save(user);
	        });

	        // 9️⃣ Convert to DTO and return
	        return convertToDTO(saved);
	    }




    /** ================= UPDATE USER ================= **/
    @Override
    @Transactional
    public ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail) {

        // ---------------- 1️Get current logged-in user ----------------
        User currentUser = getCurrentLoggedInUser(loggedInEmail);

        // ---------------- 2️ Fetch existing ManageUsers ----------------
        
        ManageUsers existing = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        String oldFullName = existing.getFullName();

        // ---------------- 3️Handle name updates ----------------
        if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
            String[] parts = manageUsers.getFullName().trim().split("\\s+");
            existing.setFirstName(parts[0]);
            existing.setMiddleName(parts.length > 2
                    ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1))
                    : null);
            existing.setLastName(parts.length > 1 ? parts[parts.length - 1] : null);
        } else {
            existing.setFirstName(manageUsers.getFirstName());
            existing.setMiddleName(manageUsers.getMiddleName());
            existing.setLastName(manageUsers.getLastName());
        }

        existing.setEmail(manageUsers.getEmail());
        existing.setFullName(buildFullName(existing));
        existing.setUpdatedBy(currentUser.getId());
        existing.setUpdatedByName(buildFullName(currentUser));

        // ---------------- 4️ Handle role updates safely ----------------
        Role role = null;
        if (manageUsers.getRoleName() != null && !manageUsers.getRoleName().isBlank()) {
            String roleName = manageUsers.getRoleName().trim().toUpperCase();
            existing.setRoleName(roleName);

            //  Use case-insensitive role lookup
            role = roleRepository.findByRoleNameIgnoreCase(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

            // Assign the Role entity to ManageUsers
            existing.setRole(role);
        } else {
            existing.setRoleName(null);
            existing.setRole(null);
        }

        // ---------------- 5️ Save ManageUsers ----------------
        ManageUsers saved = manageUserRepository.save(existing);

        // ---------------- 6️ Sync User table ----------------
        User user = userRepository.findByEmailIgnoreCase(saved.getEmail())
                .orElseGet(User::new);

        user.setEmail(saved.getEmail());
        user.setPrimaryEmail(saved.getEmail());
        user.setFirstName(saved.getFirstName());
        user.setMiddleName(saved.getMiddleName());
        user.setLastName(saved.getLastName());
        user.setFullName(saved.getFullName());
        user.setActive(true);
        user.setApproved(true);

        if (role != null) {
            user.setRole(role);  // SINGLE source of truth
        } else {
            user.setRole(null);
        }

        userRepository.save(user);
        
        String newFullName = saved.getFullName();

        if (!Objects.equals(oldFullName, newFullName)) {
            userNameSyncServiceImpl.syncUserFullName(
                    user.getId(),     // IMPORTANT: User ID
                    newFullName
            );
        }


        // ---------------- 7️Audit log ----------------
        auditLogRepository.save(
                AuditLog.builder()
                        .action("UPDATE")
                        .entityName("ManageUsers")
                        .entityId(saved.getId())
                        .performedBy(buildFullName(currentUser))
                        .performedById(currentUser.getId())
                        .email(currentUser.getEmail())
                        .timestamp(LocalDateTime.now())
                        .details("Updated ManageUser ID: " + saved.getId())
                        .build()
        );

        // ---------------- 8️ Return DTO ----------------
        return convertToDTO(saved);
    }


    /** ================= DELETE USER ================= **/
    @Override
    public void deleteUser(Long id, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        ManageUsers manageUser = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName()) &&
                "SUPERADMIN".equalsIgnoreCase(manageUser.getRoleName())) {
            throw new RuntimeException("ADMIN cannot delete SUPERADMIN");
        }

        boolean hasDeletePrivilege = currentUser.getRole().getPrivileges().stream()
                .anyMatch(p -> "DELETE_MANAGE_USERS".equalsIgnoreCase(p.getName()));

        if (!hasDeletePrivilege) {
            throw new RuntimeException("You do not have DELETE_MANAGE_USERS privilege");
        }

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

    /** ================= PAGINATION + SEARCH (FIXED ALPHABETICAL) ================= **/
    @Override
    public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(
            int page,
            int size,
            String sortField,
            String sortDir,
            String keyword
    ) {

        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            sortDir = "asc";
        }

        // Map external sortField names to entity fields
        Map<String, String> sortFieldMap = Map.of(
                "id", "id",
                "firstName", "firstName",
                "middleName", "middleName",
                "lastName", "lastName",
                "fullName", "fullName",
                "email", "email",
                "primaryEmail", "primaryEmail",
                "roleName", "roleName",
                "addedByName", "addedByName",
                "updatedByName", "updatedByName"
        );

        String mappedSortField = sortFieldMap.getOrDefault(sortField, "id");

        //  Specification for keyword search
        Specification<ManageUsers> spec = (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String like = "%" + keyword.trim().toLowerCase() + "%";
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("firstName")), like));
            predicates.add(cb.like(cb.lower(root.get("middleName")), like));
            predicates.add(cb.like(cb.lower(root.get("lastName")), like));
            predicates.add(cb.like(cb.lower(root.get("fullName")), like));
            predicates.add(cb.like(cb.lower(root.get("email")), like));
            predicates.add(cb.like(cb.lower(root.get("primaryEmail")), like));
            predicates.add(cb.like(cb.lower(root.get("roleName")), like));
            predicates.add(cb.like(cb.lower(root.get("addedByName")), like));
            predicates.add(cb.like(cb.lower(root.get("updatedByName")), like));

            return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        //  Sort case-insensitive
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc")
                        ? Sort.Order.desc(mappedSortField).ignoreCase()
                        : Sort.Order.asc(mappedSortField).ignoreCase());

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ManageUsers> userPage = manageUserRepository.findAll(spec, pageable);

        List<ManageUserDTO> dtoList = userPage.getContent()
                .stream()
                .map(this::convertToDTO)
                .toList();

        return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
    }

    
    /** ================= UPDATE USER PROFILE ================= **/
    @Override
    public User updateUserProfile(UserUpdateRequest request, MultipartFile profileImage, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);

        boolean isAdmin = currentUser.getRole() != null &&
                List.of("SUPERADMIN", "ADMIN").contains(currentUser.getRole().getRoleName().toUpperCase());

        User userToUpdate = (isAdmin && request.getId() != null) ?
                userRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("User not found")) :
                currentUser;

        userToUpdate.setFullName(request.getFullName());
        userToUpdate.setPrimaryEmail(request.getPrimaryEmail());
        userToUpdate.setAlternativeEmail(request.getAlternativeEmail());
        userToUpdate.setMobileNumber(request.getMobileNumber());
        userToUpdate.setAlternativeMobileNumber(request.getAlternativeMobileNumber());
        userToUpdate.setTaxId(request.getTaxId());
        userToUpdate.setBusinessId(request.getBusinessId());
        userToUpdate.setPreferredCurrency(request.getPreferredCurrency());
        userToUpdate.setInvoicePrefix(request.getInvoicePrefix());
        userToUpdate.setCompanyName(request.getCompanyName());

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String savedFileName = uploadFile(profileImage, userToUpdate.getId());
                userToUpdate.setProfilePicPath(savedFileName);
            } catch (Exception e) {
                throw new RuntimeException("Error saving profile image");
            }
        }

        return userRepository.save(userToUpdate);
    }

    /** ================= UPLOAD FILE ================= **/
    @Override
    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = "user_" + userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    /** ================= MAP USER TO DTO ================= **/
    @Override
    public UserUpdateRequest mapToDto(User user) {
        Optional<ManageUsers> optionalManageUser = manageUserRepository.findByEmailIgnoreCase(user.getEmail());
        String fullName = user.getFullName();
        String primaryEmail = user.getPrimaryEmail();

        if (optionalManageUser.isPresent()) {
            ManageUsers manageUser = optionalManageUser.get();
            if (manageUser.getFullName() != null && !manageUser.getFullName().isBlank()) {
                fullName = manageUser.getFullName().trim();
            }
            if (manageUser.getEmail() != null && !manageUser.getEmail().isBlank()) {
                primaryEmail = manageUser.getEmail().trim();
            }
        }

        String profileUrl = null;
        if (user.getProfilePicPath() != null && !user.getProfilePicPath().isEmpty()) {
            profileUrl = "http://localhost:1717/uploads/profile/" + user.getProfilePicPath();
        }

        return UserUpdateRequest.builder()
                .id(user.getId())
                .fullName(fullName)
                .primaryEmail(primaryEmail)
                .alternativeEmail(user.getAlternativeEmail())
                .mobileNumber(user.getMobileNumber())
                .alternativeMobileNumber(user.getAlternativeMobileNumber())
                .companyName(user.getCompanyName())
                .taxId(user.getTaxId())
                .businessId(user.getBusinessId())
                .preferredCurrency(user.getPreferredCurrency())
                .invoicePrefix(user.getInvoicePrefix())
                .profilePicPath(profileUrl)
                .build();
    }

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

    @Override
    public User updateUserProfileDynamic(
            Long id,
            String mobileNumber,
            String alternativeEmail,
            String alternativeMobileNumber,
            String companyName,
            String invoicePrefix,
            String taxId,
            String businessId,
            String preferredCurrency,
            MultipartFile profileImage) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMobileNumber(mobileNumber);
        user.setAlternativeEmail(alternativeEmail);
        user.setAlternativeMobileNumber(alternativeMobileNumber);
        user.setCompanyName(companyName);
        user.setInvoicePrefix(invoicePrefix);
        user.setTaxId(taxId);
        user.setBusinessId(businessId);
        user.setPreferredCurrency(preferredCurrency);

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String savedFileName = uploadFile(profileImage, user.getId());
                user.setProfilePicPath(savedFileName);
            } catch (IOException e) {
                throw new RuntimeException("Error saving profile image", e);
            }
        }

        return userRepository.save(user);
    }
}