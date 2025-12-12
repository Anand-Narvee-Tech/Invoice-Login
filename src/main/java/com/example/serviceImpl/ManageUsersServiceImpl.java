package com.example.serviceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
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
        // Full name
        String fullName = (entity.getFullName() != null && !entity.getFullName().isBlank())
                ? entity.getFullName()
                : buildFullName(entity);

        // Dynamically compute addedByName
        String addedByName = "SYSTEM";
        if (entity.getAddedBy() != null) {
            Optional<User> addedByUser = userRepository.findById(entity.getAddedBy().getId());
            addedByName = addedByUser.map(this::buildFullName).orElse(buildFullName(entity.getAddedBy()));
        }

        // Dynamically compute updatedByName
        String updatedByName = null;
        if (entity.getUpdatedBy() != null) {
            Optional<User> updatedByUser = userRepository.findById(entity.getUpdatedBy());
            updatedByName = updatedByUser.map(this::buildFullName).orElse(entity.getUpdatedByName());
        }

        return ManageUserDTO.builder()
                .id(entity.getId())
                .fullName(fullName)
                .firstName(entity.getFirstName())
                .middleName(entity.getMiddleName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .primaryEmail(entity.getPrimaryEmail())
                .roleName(entity.getRoleName())
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
    public ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail) {
        User currentUser = getCurrentLoggedInUser(loggedInEmail);
        String currentUserRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

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

        manageUsers.setRoleName(manageUsers.getRoleName().toUpperCase());
        manageUsers.setAddedBy(currentUser);
        manageUsers.setAddedByName(buildFullName(currentUser));
        manageUsers.setCreatedBy(currentUser);
//        manageUsers.setUpdatedBy(currentUser.getId());
//        manageUsers.setUpdatedByName(buildFullName(currentUser));

        if(manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
            manageUsers.setFullName(manageUsers.getFullName().trim());
        }

        ManageUsers saved = manageUserRepository.save(manageUsers);

        // Sync to User table
        userRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresentOrElse(u -> {
            if (u.getCreatedBy() == null) u.setCreatedBy(currentUser);

            Role role = roleRepository.findByRoleNameIgnoreCase(saved.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + saved.getRoleName()));
            u.setRole(role);

            u.setFullName(buildFullName(saved));
            u.setPrimaryEmail(saved.getPrimaryEmail());

            userRepository.save(u);
        }, () -> {
            User user = new User();
            user.setEmail(saved.getEmail());
            user.setFirstName(saved.getFirstName());
            user.setMiddleName(saved.getMiddleName());
            user.setLastName(saved.getLastName());
            user.setFullName(buildFullName(saved));
            user.setPrimaryEmail(saved.getPrimaryEmail());
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
        // Fetch current logged-in user
        User currentUser = getCurrentLoggedInUser(loggedInEmail);

        // Fetch existing ManageUsers entity
        ManageUsers existing = manageUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ----------------------------
        // 1️⃣ Handle fullName properly
        // ----------------------------
        if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
            String[] parts = manageUsers.getFullName().trim().split("\\s+");

            existing.setFirstName(parts[0]);

            if (parts.length == 2) {
                existing.setMiddleName(null);
                existing.setLastName(parts[1]);
            } else if (parts.length > 2) {
                existing.setMiddleName(String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)));
                existing.setLastName(parts[parts.length - 1]);
            } else {
                existing.setMiddleName(null);
                existing.setLastName(null);
            }
        } else {
            existing.setFirstName(manageUsers.getFirstName());
            existing.setMiddleName(manageUsers.getMiddleName());
            existing.setLastName(manageUsers.getLastName());
        }

        // Update email and role if provided
        existing.setEmail(manageUsers.getEmail());
        if (manageUsers.getRoleName() != null) {
            existing.setRoleName(manageUsers.getRoleName().toUpperCase());
        }

        // ----------------------------
        // 2️⃣ Rebuild fullName dynamically
        // ----------------------------
        existing.setFullName(buildFullName(existing));
        existing.setUpdatedBy(currentUser.getId());
        existing.setUpdatedByName(buildFullName(currentUser));

        ManageUsers saved = manageUserRepository.save(existing);

        // ----------------------------
        // 3️⃣ Audit log
        // ----------------------------
        AuditLog audit = AuditLog.builder()
                .action("UPDATE")
                .entityName("ManageUsers")
                .entityId(saved.getId())
                .performedBy(buildFullName(currentUser))
                .performedById(currentUser.getId())
                .email(currentUser.getEmail())
                .timestamp(LocalDateTime.now())
                .details("Updated ManageUser ID: " + saved.getId())
                .build();
        auditLogRepository.save(audit);

        // ----------------------------
        // 4️⃣ Sync with User table
        // ----------------------------
        userRepository.findByEmailIgnoreCase(existing.getEmail()).ifPresentOrElse(user -> {
            user.setFirstName(existing.getFirstName());
            user.setMiddleName(existing.getMiddleName());
            user.setLastName(existing.getLastName());
            user.setFullName(existing.getFullName());
            user.setPrimaryEmail(existing.getEmail());
            userRepository.save(user);
        }, () -> {
            User user = new User();
            user.setEmail(existing.getEmail());
            user.setFirstName(existing.getFirstName());
            user.setMiddleName(existing.getMiddleName());
            user.setLastName(existing.getLastName());
            user.setFullName(existing.getFullName());
            user.setPrimaryEmail(existing.getEmail());
            user.setActive(true);
            user.setApproved(true);
            userRepository.save(user);
        });

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

    /** ================= PAGINATION + SEARCH ================= **/
    @Override
    public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(
            int page,
            int size,
            String sortField,
            String sortDir,
            String keyword
    ) {

        // Map UI field "name" → DB field "fullName"
        if ("name".equalsIgnoreCase(sortField)) {
            sortField = "fullName";
        }

        // Valid fields
        Set<String> validFields = Set.of(
                "id", "firstName", "middleName", "lastName", "fullName",
                "email", "primaryEmail", "roleName",
                "addedByName", "updatedByName"
        );

        // Fallback to id only if sorting field is invalid
        if (!validFields.contains(sortField)) {
            sortField = "id";
        }

        // PRIMARY FIELD ONLY (NO SECONDARY SORT)
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Search Specification
        Specification<ManageUsers> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), likeKeyword),
                    cb.like(cb.lower(root.get("middleName")), likeKeyword),
                    cb.like(cb.lower(root.get("lastName")), likeKeyword),
                    cb.like(cb.lower(root.get("fullName")), likeKeyword),
                    cb.like(cb.lower(root.get("email")), likeKeyword),
                    cb.like(cb.lower(root.get("primaryEmail")), likeKeyword),
                    cb.like(cb.lower(root.get("roleName")), likeKeyword),
                    cb.like(cb.lower(root.get("addedByName")), likeKeyword),
                    cb.like(cb.lower(root.get("updatedByName")), likeKeyword)
            );
        };

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

        Optional<ManageUsers> optionalManageUser =
                manageUserRepository.findByEmailIgnoreCase(user.getEmail());

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
