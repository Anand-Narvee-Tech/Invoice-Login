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
import com.example.DTO.SortingRequestDTO;
import com.example.DTO.UserUpdateRequest;
import com.example.entity.AuditLog;
import com.example.entity.ManageUsers;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.exception.BusinessException;
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

	@Autowired
	private  ManageUserRepository manageUserRepository;
	
	@Autowired
	private  UserRepository userRepository;
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
		// Bhargav
		return ManageUserDTO.builder().id(entity.getId()).fullName(fullName).firstName(entity.getFirstName())
				.middleName(entity.getMiddleName()).lastName(entity.getLastName()).email(entity.getEmail())
				.primaryEmail(entity.getPrimaryEmail())
				.companyName(entity.getCompanyName())
				.roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
				.addedBy(entity.getAddedBy() != null ? entity.getAddedBy().getId().toString() : null)
				.addedByName(addedByName).updatedBy(entity.getUpdatedBy()).updatedByName(updatedByName)

				// 🔽 Newly added fields
				.state(entity.getState()).country(entity.getCountry()).pincode(entity.getPincode())
				.telephone(entity.getTelephone()).ein(entity.getEin()).gstin(entity.getGstin())
				.website(entity.getWebsite()).address(entity.getAddress()).city(entity.getCity()).build();
	}
	// Bhargav

//		return ManageUserDTO.builder().id(entity.getId()).fullName(fullName).firstName(entity.getFirstName())
//				.middleName(entity.getMiddleName()).lastName(entity.getLastName()).email(entity.getEmail())
//				.primaryEmail(entity.getPrimaryEmail())
//				.roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
//				.addedBy(entity.getAddedBy() != null ? entity.getAddedBy().getId().toString() : null)
//				.addedByName(addedByName).updatedBy(entity.getUpdatedBy()).updatedByName(updatedByName).build();
//	}

	/** ================= BUILD FULL NAME ================= **/
	private String buildFullName(ManageUsers user) {
		return Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
				.filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
	}

	private String buildFullName(User user) {
		return Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
				.filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
	}

	private String extractDomain(String email) {
		if (email == null || !email.contains("@")) {
			throw new RuntimeException("Invalid email address");
		}
		return email.substring(email.indexOf("@") + 1).toLowerCase();
	}

	/** ================= CREATE USER ================= **/
	@Override
	@Transactional
	public ManageUserDTO createUser(ManageUsers manageUsers, String loggedInEmail) {

		User currentUser = getCurrentLoggedInUser(loggedInEmail);

		if (currentUser.getRole() == null || currentUser.getRole().getRoleName() == null) {
			throw new BusinessException("Logged-in user role not found");
		}

		String currentUserRole = currentUser.getRole().getRoleName();

		if (!List.of("SUPERADMIN", "ADMIN").contains(currentUserRole.toUpperCase())) {
			throw new BusinessException("You do not have permission to create users");
		}

		if ("ADMIN".equalsIgnoreCase(currentUserRole) && "SUPERADMIN".equalsIgnoreCase(manageUsers.getRoleName())) {
			throw new BusinessException("ADMIN cannot create SUPERADMIN");
		}

		String newUserEmail = manageUsers.getEmail().trim().toLowerCase();
		manageUsers.setEmail(newUserEmail);

		if (manageUserRepository.existsByEmailIgnoreCase(newUserEmail)) {
			throw new BusinessException("Email already exists");
		}

		String currentDomain = extractDomain(currentUser.getEmail());
		String newUserDomain = extractDomain(newUserEmail);

		if (!currentDomain.equalsIgnoreCase(newUserDomain)) {
			throw new BusinessException("You can create users only for your own company");
		}

		manageUsers.setCompanyDomain(currentDomain);

		Role role = roleRepository.findByRoleNameIgnoreCase(manageUsers.getRoleName())
				.orElseThrow(() -> new BusinessException("Role not found: " + manageUsers.getRoleName()));

		manageUsers.setRole(role);
		manageUsers.setRoleName(role.getRoleName());

		if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
			manageUsers.setFullName(manageUsers.getFullName().trim());
		}

		manageUsers.setAddedBy(currentUser);
		manageUsers.setCreatedBy(currentUser);
		manageUsers.setAddedByName(buildFullName(currentUser));

		ManageUsers savedManageUser = manageUserRepository.save(manageUsers);

		userRepository.findByEmailIgnoreCase(newUserEmail).ifPresentOrElse(existingUser -> {

			// Update existing user
			if (existingUser.getCreatedBy() == null) {
				existingUser.setCreatedBy(currentUser);
			}

			existingUser.setRole(role);
			existingUser.setFullName(savedManageUser.getFullName());
			existingUser.setPrimaryEmail(savedManageUser.getPrimaryEmail());
			existingUser.setActive(true);
			existingUser.setApproved(true);

			userRepository.save(existingUser);

		}, () -> {

			// Create new user
			User user = new User();
			user.setEmail(savedManageUser.getEmail());
			user.setFirstName(savedManageUser.getFirstName());
			user.setMiddleName(savedManageUser.getMiddleName());
			user.setLastName(savedManageUser.getLastName());
			user.setFullName(savedManageUser.getFullName());
			user.setPrimaryEmail(savedManageUser.getPrimaryEmail());
			// Bhargav
			user.setCompanyName(savedManageUser.getCompanyName());
			user.setMobileNumber(savedManageUser.getMobileNumber());
			user.setState(savedManageUser.getState());
			user.setCountry(savedManageUser.getCountry());
			user.setCity(savedManageUser.getCity());
			user.setPincode(savedManageUser.getPincode());
			user.setTelephone(savedManageUser.getTelephone());
			user.setWebsite(savedManageUser.getWebsite());
			user.setEin(savedManageUser.getEin());
			user.setAddress(savedManageUser.getAddress());
			// Bhargav
			user.setApproved(true);
			user.setActive(true);
			user.setCreatedBy(currentUser);
			user.setRole(role);
			System.err.println(userRepository.save(user));
			userRepository.save(user);
		});

		return convertToDTO(savedManageUser);
	}

	/** ================= UPDATE USER PROFILE ================= **/
	@Override
	public User updateUserProfile(UserUpdateRequest request, String loggedInEmail) {
		// ✅ Get the currently logged-in user
		User currentUser = getCurrentLoggedInUser(loggedInEmail);

		// ✅ Admins can update any user, normal users can only update their own
		User userToUpdate;

		boolean isAdmin = currentUser.getRole() != null
				&& List.of("SUPERADMIN", "ADMIN").contains(currentUser.getRole().getRoleName().toUpperCase());

		if (isAdmin && request.getId() != null && request.getId() > 0) {
			// Admin updating another user
			userToUpdate = userRepository.findById(request.getId())
					.orElseThrow(() -> new RuntimeException("User not found with id: " + request.getId()));
		} else {
			// Normal user updating their own profile
			userToUpdate = userRepository.findByEmailIgnoreCase(loggedInEmail)
					.orElseThrow(() -> new RuntimeException("Logged-in user not found: " + loggedInEmail));
		}

		// ✅ Update editable fields
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

		// ✅ Save update
		User updatedUser = userRepository.save(userToUpdate);

		// ✅ Update manage_users table audit fields if applicable
		manageUserRepository.findByEmailIgnoreCase(updatedUser.getEmail()).ifPresent(manageUser -> {
			manageUser.setUpdatedBy(currentUser.getId());
			manageUser.setUpdatedByName(buildFullName(currentUser));
			manageUserRepository.save(manageUser);
		});

		return updatedUser;
	}

//Bhargav	
	/** ================= UPDATE USER ================= **/
//	@Override
//	@Transactional
//	public ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail) {
//
//		// ---------------- 1️Get current logged-in user ----------------
//		User currentUser = getCurrentLoggedInUser(loggedInEmail);
//
//		// ---------------- 2️ Fetch existing ManageUsers ----------------
//
//		ManageUsers existing = manageUserRepository.findById(id)
//				.orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
//
//		String oldFullName = existing.getFullName();
//
//		// ---------------- 3️Handle name updates ----------------
//		if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {
//			String[] parts = manageUsers.getFullName().trim().split("\\s+");
//			existing.setFirstName(parts[0]);
//			existing.setMiddleName(
//					parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)) : null);
//			existing.setLastName(parts.length > 1 ? parts[parts.length - 1] : null);
//		} else {
//			existing.setFirstName(manageUsers.getFirstName());
//			existing.setMiddleName(manageUsers.getMiddleName());
//			existing.setLastName(manageUsers.getLastName());
//		}
//
//		existing.setEmail(manageUsers.getEmail());
//		existing.setFullName(buildFullName(existing));
//		existing.setUpdatedBy(currentUser.getId());
//		existing.setUpdatedByName(buildFullName(currentUser));
//
//		// ---------------- 4️ Handle role updates safely ----------------
//		Role role = null;
//		if (manageUsers.getRoleName() != null && !manageUsers.getRoleName().isBlank()) {
//			String roleName = manageUsers.getRoleName().trim().toUpperCase();
//			existing.setRoleName(roleName);
//
//			// Use case-insensitive role lookup
//			role = roleRepository.findByRoleNameIgnoreCase(roleName)
//					.orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
//
//			// Assign the Role entity to ManageUsers
//			existing.setRole(role);
//		} else {
//			existing.setRoleName(null);
//			existing.setRole(null);
//		}
//
//		// ---------------- 5️ Save ManageUsers ----------------
//		ManageUsers saved = manageUserRepository.save(existing);
//
//		// ---------------- 6️ Sync User table ----------------
//		User user = userRepository.findByEmailIgnoreCase(saved.getEmail()).orElseGet(User::new);
//
//		user.setEmail(saved.getEmail());
//		user.setPrimaryEmail(saved.getEmail());
//		user.setFirstName(saved.getFirstName());
//		user.setMiddleName(saved.getMiddleName());
//		user.setLastName(saved.getLastName());
//		user.setFullName(saved.getFullName());
//		user.setActive(true);
//		user.setApproved(true);
//
//		if (role != null) {
//			user.setRole(role); // SINGLE source of truth
//		} else {
//			user.setRole(null);
//		}
//
//		userRepository.save(user);
//
//		String newFullName = saved.getFullName();
//
//		if (!Objects.equals(oldFullName, newFullName)) {
//			userNameSyncServiceImpl.syncUserFullName(user.getId(), // IMPORTANT: User ID
//					newFullName);
//		}
//
//		// ---------------- 7️Audit log ----------------
//		auditLogRepository.save(AuditLog.builder().action("UPDATE").entityName("ManageUsers").entityId(saved.getId())
//				.performedBy(buildFullName(currentUser)).performedById(currentUser.getId())
//				.email(currentUser.getEmail()).timestamp(LocalDateTime.now())
//				.details("Updated ManageUser ID: " + saved.getId()).build());
//
//		// ---------------- 8️ Return DTO ----------------
//		return convertToDTO(saved);
//	}

//Bhargav	

	@Override
	@Transactional
	public ManageUserDTO updateUser(Long id, ManageUsers manageUsers, String loggedInEmail) {

		// ---------------- 1️⃣ Get current logged-in user ----------------
		User currentUser = getCurrentLoggedInUser(loggedInEmail);

		// ---------------- 2️⃣ Fetch existing ManageUsers ----------------
		ManageUsers existing = manageUserRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

		String oldFullName = existing.getFullName();

		// ---------------- 3️⃣ Handle name updates ----------------
		if (manageUsers.getFullName() != null && !manageUsers.getFullName().isBlank()) {

			String[] parts = manageUsers.getFullName().trim().split("\\s+");
			existing.setFirstName(parts[0]);
			existing.setMiddleName(
					parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)) : null);
			existing.setLastName(parts.length > 1 ? parts[parts.length - 1] : null);

		} else {
			if (manageUsers.getFirstName() != null)
				existing.setFirstName(manageUsers.getFirstName());
			if (manageUsers.getMiddleName() != null)
				existing.setMiddleName(manageUsers.getMiddleName());
			if (manageUsers.getLastName() != null)
				existing.setLastName(manageUsers.getLastName());
		}

		existing.setFullName(buildFullName(existing));

		// ---------------- 4️⃣ Core fields ----------------
		if (manageUsers.getEmail() != null && !manageUsers.getEmail().isBlank()) {
			existing.setEmail(manageUsers.getEmail());
		}

		if (manageUsers.getPrimaryEmail() != null && !manageUsers.getPrimaryEmail().isBlank()) {
			existing.setPrimaryEmail(manageUsers.getPrimaryEmail());
		}

		if (manageUsers.getMobileNumber() != null && !manageUsers.getMobileNumber().isBlank()) {
			existing.setMobileNumber(manageUsers.getMobileNumber());
		}

		if (manageUsers.getCompanyName() != null && !manageUsers.getCompanyName().isBlank()) {
			existing.setCompanyName(manageUsers.getCompanyName());
		}

		// ---------------- 5️⃣ Newly added fields ----------------
		if (manageUsers.getState() != null)
			existing.setState(manageUsers.getState());
		if (manageUsers.getCountry() != null)
			existing.setCountry(manageUsers.getCountry());
		if (manageUsers.getPincode() != null)
			existing.setPincode(manageUsers.getPincode());
		
		if (manageUsers.getCity() != null)
			existing.setCity(manageUsers.getCity());
		
		if (manageUsers.getTelephone() != null)
			existing.setTelephone(manageUsers.getTelephone());
		if (manageUsers.getEin() != null)
			existing.setEin(manageUsers.getEin());
		if (manageUsers.getGstin() != null)
			existing.setGstin(manageUsers.getGstin());
		if (manageUsers.getWebsite() != null)
			existing.setWebsite(manageUsers.getWebsite());
		if (manageUsers.getAddress() != null)
			existing.setAddress(manageUsers.getAddress());

		// ---------------- 6️⃣ Handle role updates safely ----------------
		Role role = null;
		if (manageUsers.getRoleName() != null && !manageUsers.getRoleName().isBlank()) {

			String roleName = manageUsers.getRoleName().trim().toUpperCase();
			existing.setRoleName(roleName);

			role = roleRepository.findByRoleNameIgnoreCase(roleName)
					.orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

			existing.setRole(role);
		}
		// ❌ DO NOT reset role if not provided

		// ---------------- 7️⃣ Audit fields ----------------
		existing.setUpdatedBy(currentUser.getId());
		existing.setUpdatedByName(buildFullName(currentUser));
		// existing.setUpdatedOn(LocalDateTime.now());

		// ---------------- 8️⃣ Save ManageUsers ----------------
		ManageUsers saved = manageUserRepository.save(existing);

		// ---------------- 9️⃣ Sync User table (NO new record) ----------------
		User user = userRepository.findByEmailIgnoreCase(saved.getEmail())
				.orElseThrow(() -> new RuntimeException("Linked User not found"));

		user.setFirstName(saved.getFirstName());
		user.setMiddleName(saved.getMiddleName());
		user.setLastName(saved.getLastName());
		user.setFullName(saved.getFullName());
		user.setEmail(saved.getEmail());
		user.setPrimaryEmail(saved.getEmail());

		if (role != null) {
			user.setRole(role);
		}

		userRepository.save(user);

		// ---------------- 🔟 Sync username if changed ----------------
		if (!Objects.equals(oldFullName, saved.getFullName())) {
			userNameSyncServiceImpl.syncUserFullName(user.getId(), saved.getFullName());
		}

		// ---------------- 1️⃣1️⃣ Audit log ----------------
		auditLogRepository.save(AuditLog.builder().action("UPDATE").entityName("ManageUsers").entityId(saved.getId())
				.performedBy(buildFullName(currentUser)).performedById(currentUser.getId())
				.email(currentUser.getEmail()).timestamp(LocalDateTime.now())
				.details("Updated ManageUser ID: " + saved.getId()).build());

		// ---------------- 1️⃣2️⃣ Return DTO ----------------
		return convertToDTO(saved);
	}

	/** ================= DELETE USER ================= **/
	@Override
	public void deleteUser(Long id, String loggedInEmail) {
		User currentUser = getCurrentLoggedInUser(loggedInEmail);
		ManageUsers manageUser = manageUserRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if ("ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName())
				&& "SUPERADMIN".equalsIgnoreCase(manageUser.getRoleName())) {
			throw new RuntimeException("ADMIN cannot delete SUPERADMIN");
		}

		boolean hasDeletePrivilege = currentUser.getRole().getPrivileges().stream()
				.anyMatch(p -> "DELETE_MANAGE_USERS".equalsIgnoreCase(p.getName()));

		if (!hasDeletePrivilege) {
			throw new RuntimeException("You do not have DELETE_MANAGE_USERS privilege");
		}

		userRepository.findByEmailIgnoreCase(manageUser.getEmail()).ifPresent(userRepository::delete);

		manageUserRepository.deleteById(id);
	}

	/** ================= GET ALL USERS ================= **/
//	@Override
//	public List<ManageUserDTO> getAllUsers(String loggedInEmail) {
//		User currentUser = getCurrentLoggedInUser(loggedInEmail);
//		String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;
//
//		List<ManageUsers> users;
//		if ("SUPERADMIN".equalsIgnoreCase(roleName)) {
//			users = manageUserRepository.findAll();
//		} else if ("ADMIN".equalsIgnoreCase(roleName)) {
//			users = manageUserRepository.findAll().stream().filter(u -> !"SUPERADMIN".equalsIgnoreCase(u.getRoleName()))
//					.collect(Collectors.toList());
//		} else {
//			users = manageUserRepository.findByEmailIgnoreCase(currentUser.getEmail()).map(List::of)
//					.orElse(Collections.emptyList());
//		}
//
//		return users.stream().map(this::convertToDTO).collect(Collectors.toList());
//	}

	@Override
	public List<ManageUserDTO> getAllUsers(String loggedInEmail) {

		User currentUser = getCurrentLoggedInUser(loggedInEmail);

		String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

		String domain = extractDomain(currentUser.getEmail());

		List<ManageUsers> users;

		if ("SUPERADMIN".equalsIgnoreCase(roleName)) {

			// Superadmin can see everything
			users = manageUserRepository.findAll();

		} else if ("ADMIN".equalsIgnoreCase(roleName)) {

			// 🔥 ADMIN should see ONLY their domain users
			users = manageUserRepository.findByCompanyDomainIgnoreCase(domain);

		} else {

			// Normal user can see only himself
			users = manageUserRepository.findByEmailIgnoreCase(currentUser.getEmail()).map(List::of)
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

	/**
	 * ================= PAGINATION + SEARCH (FIXED ALPHABETICAL) =================
	 **/
	@Override
	public Page<ManageUserDTO> getAllUsersWithPaginationAndSearch(int page, int size, String sortField, String sortDir,
			String keyword) {

		if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
			sortDir = "asc";
		}

		// Map external sortField names to entity fields
		Map<String, String> sortFieldMap = Map.of("id", "id", "firstName", "firstName", "middleName", "middleName",
				"lastName", "lastName", "fullName", "fullName", "email", "email", "primaryEmail", "primaryEmail",
				"roleName", "roleName", "addedByName", "addedByName", "updatedByName", "updatedByName");

		String mappedSortField = sortFieldMap.getOrDefault(sortField, "id");

		// Specification for keyword search
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

		// Sort case-insensitive
		Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Order.desc(mappedSortField).ignoreCase()
				: Sort.Order.asc(mappedSortField).ignoreCase());

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<ManageUsers> userPage = manageUserRepository.findAll(spec, pageable);

		List<ManageUserDTO> dtoList = userPage.getContent().stream().map(this::convertToDTO).toList();

		return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
	}

	/** ================= UPDATE USER PROFILE ================= **/
	@Override
	public User updateUserProfile(UserUpdateRequest request, MultipartFile profileImage, String loggedInEmail) {
		User currentUser = getCurrentLoggedInUser(loggedInEmail);

		boolean isAdmin = currentUser.getRole() != null
				&& List.of("SUPERADMIN", "ADMIN").contains(currentUser.getRole().getRoleName().toUpperCase());

		User userToUpdate = (isAdmin && request.getId() != null)
				? userRepository.findById(request.getId()).orElseThrow(() -> new RuntimeException("User not found"))
				: currentUser;

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
	
//Bhargav	
//	@Override
//	public UserUpdateRequest mapToDto(User user) {
//		Optional<ManageUsers> optionalManageUser = manageUserRepository.findByEmailIgnoreCase(user.getEmail());
//		String fullName = user.getFullName();
//		String primaryEmail = user.getPrimaryEmail();
//
//		if (optionalManageUser.isPresent()) {
//			ManageUsers manageUser = optionalManageUser.get();
//			if (manageUser.getFullName() != null && !manageUser.getFullName().isBlank()) {
//				fullName = manageUser.getFullName().trim();
//			}
//			if (manageUser.getEmail() != null && !manageUser.getEmail().isBlank()) {
//				primaryEmail = manageUser.getEmail().trim();
//			}
//		}
//
//		String profileUrl = null;
//		if (user.getProfilePicPath() != null && !user.getProfilePicPath().isEmpty()) {
//			profileUrl = "http://localhost:1717/uploads/profile/" + user.getProfilePicPath();
//		}
//
//		return UserUpdateRequest.builder().id(user.getId()).fullName(fullName).primaryEmail(primaryEmail)
//				.alternativeEmail(user.getAlternativeEmail()).mobileNumber(user.getMobileNumber())
//				.alternativeMobileNumber(user.getAlternativeMobileNumber()).companyName(user.getCompanyName())
//				.taxId(user.getTaxId()).businessId(user.getBusinessId()).preferredCurrency(user.getPreferredCurrency())
//				.invoicePrefix(user.getInvoicePrefix()).profilePicPath(profileUrl).build();
//	}
//	
//Bhargav	
	
	
	@Override
	public UserUpdateRequest mapToDto(User user) {

	    return UserUpdateRequest.builder()
	            .id(user.getId())
	            .fullName(user.getFullName())
	            .primaryEmail(user.getPrimaryEmail())
	            .alternativeEmail(user.getAlternativeEmail())
	            .mobileNumber(user.getMobileNumber())
	            .alternativeMobileNumber(user.getAlternativeMobileNumber())
	            .taxId(user.getTaxId())
	            .businessId(user.getBusinessId())
	            .preferredCurrency(user.getPreferredCurrency())
	            .invoicePrefix(user.getInvoicePrefix())
	            .companyName(user.getCompanyName())
	            .state(user.getState())
	            .country(user.getCountry())
	            .city(user.getCity())
	            .pincode(user.getPincode())
	            .telephone(user.getTelephone())
	            .ein(user.getEin())
	            .gstin(user.getGstin())
	            .website(user.getWebsite())
	            .address(user.getAddress())
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

	

// comment by Bhargav
//	@Override
//	public User updateUserProfileDynamic(Long id, String mobileNumber, String alternativeEmail,
//			String alternativeMobileNumber, String companyName, String invoicePrefix, String taxId, String businessId,
//			String preferredCurrency, MultipartFile profileImage) {
//
//		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
//
//		user.setMobileNumber(mobileNumber);
//		user.setAlternativeEmail(alternativeEmail);
//		user.setAlternativeMobileNumber(alternativeMobileNumber);
//		user.setCompanyName(companyName);
//		user.setInvoicePrefix(invoicePrefix);
//		user.setTaxId(taxId);
//		user.setBusinessId(businessId);
//		user.setPreferredCurrency(preferredCurrency);
//
//		if (profileImage != null && !profileImage.isEmpty()) {
//			try {
//				String savedFileName = uploadFile(profileImage, user.getId());
//				user.setProfilePicPath(savedFileName);
//			} catch (IOException e) {
//				throw new RuntimeException("Error saving profile image", e);
//			}
//		}
//
//		return userRepository.save(user);
//	}
// comment by Bhargav
	
	@Override
	@Transactional
	public User updateUserProfileDynamic(UserUpdateRequest request) {

	    // ---------- UPDATE USER TABLE ----------

	    User user = userRepository.findById(request.getId())
	            .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getId()));

	    // Get email from the existing user entity
	    String userEmail = user.getEmail();
	    
	    
	    if (request.getFullName() != null)
	        user.setFullName(request.getFullName());
	    
	    if (request.getEmail() != null)
	        user.setEmail(request.getEmail());

	    if (request.getMobileNumber() != null)
	        user.setMobileNumber(request.getMobileNumber());

	    
	    if(request.getInvoicePrefix() !=null)
	    	user.setInvoicePrefix(request.getInvoicePrefix());
	    
	    if (request.getCompanyName() != null)
	        user.setCompanyName(request.getCompanyName());

	    if (request.getAddress() != null)
	        user.setAddress(request.getAddress());

	    if (request.getState() != null)
	        user.setState(request.getState());

	    if (request.getCountry() != null)
	        user.setCountry(request.getCountry());
	    
	    if (request.getCity() != null)
	        user.setCity(request.getCity());

	    if (request.getPincode() != null)
	        user.setPincode(request.getPincode());

	    if (request.getPreferredCurrency() != null)
	        user.setPreferredCurrency(request.getPreferredCurrency());

	    if (request.getTaxId() != null)
	        user.setTaxId(request.getTaxId());

	    if (request.getBusinessId() != null)
	        user.setBusinessId(request.getBusinessId());
	    
	    if (request.getTelephone() != null)
	    	user.setTelephone(request.getTelephone());

	    if (request.getEin() != null)
	    	user.setEin(request.getEin());

	    if (request.getGstin() != null)
	    	user.setGstin(request.getGstin());

	    if (request.getWebsite() != null)
	        user.setWebsite(request.getWebsite());


	    userRepository.save(user);

	    // ---------- UPDATE MANAGE_USERS TABLE ----------

	    // Use email from User entity instead of request
	    ManageUsers manageUser = manageUserRepository.findByEmail(userEmail);

	    if (manageUser == null) {
	        throw new RuntimeException("Manage user not found with email: " + userEmail);
	    }

	    if (request.getFullName() != null)
	        manageUser.setFullName(request.getFullName());

	    if (request.getPrimaryEmail() != null)
	        manageUser.setPrimaryEmail(request.getPrimaryEmail());

	    if (request.getMobileNumber() != null)
	        manageUser.setMobileNumber(request.getMobileNumber());

	    if (request.getCompanyName() != null)
	        manageUser.setCompanyName(request.getCompanyName());

	    if (request.getAddress() != null)
	        manageUser.setAddress(request.getAddress());

	    if (request.getState() != null)
	        manageUser.setState(request.getState());

	    if (request.getCity() != null)
	    	manageUser.setCity(request.getCity());
	    
	    if (request.getCountry() != null)
	        manageUser.setCountry(request.getCountry());
	    
	    if(request.getInvoicePrefix() !=null)
	    	manageUser.setInvoicePrefix(request.getInvoicePrefix());
	    
	    
	    if (request.getTaxId() != null)
	        user.setTaxId(request.getTaxId());


	    if (request.getPincode() != null)
	        manageUser.setPincode(request.getPincode());

	    if (request.getTelephone() != null)
	        manageUser.setTelephone(request.getTelephone());

	    if (request.getEin() != null)
	        manageUser.setEin(request.getEin());

	    if (request.getGstin() != null)
	        manageUser.setGstin(request.getGstin());

	    if (request.getWebsite() != null)
	        manageUser.setWebsite(request.getWebsite());

	    manageUserRepository.save(manageUser);

	    return user;
	}


	//Bhargav
	
	

/* Addedby Bhargav */
	
//	@Override
//	public Page<ManageUserDTO> getAllManageUsersWithSorting(SortingRequestDTO sortingRequestDTO) {
////	    logger.info("!!! inside class: UserServiceImpl, !! method: getAllManageUsersWithSorting");
//	    
//	    String sortField = sortingRequestDTO.getSortField();
//	    String sortOrder = sortingRequestDTO.getSortOrder();
//	    String keyword = sortingRequestDTO.getKeyword();
//	    Integer pageNo = sortingRequestDTO.getPageNumber();
//	    Integer pageSize = sortingRequestDTO.getPageSize();
//
//	    // ✅ Map frontend field names to entity field names
//	    if (sortField != null) {
//	        if (sortField.equalsIgnoreCase("Name") || sortField.equalsIgnoreCase("fullName")) {
//	            sortField = "firstName";
//	        } else if (sortField.equalsIgnoreCase("Email")) {
//	            sortField = "email";
//	        } else if (sortField.equalsIgnoreCase("Role")) {
//	            sortField = "roleName";
//	        } else if (sortField.equalsIgnoreCase("AddedBy")) {
//	            sortField = "addedByName";
//	        } else if (sortField.equalsIgnoreCase("UpdatedBy")) {
//	            sortField = "updatedByName";
//	        } else if (sortField.equalsIgnoreCase("CompanyName")) {
//	            sortField = "companyName";
//	        } else if (sortField.equalsIgnoreCase("MobileNumber")) {
//	            sortField = "mobileNumber";
//	        }
//	    } else {
//	        sortField = "id"; // Default sort field
//	    }
//
//	    // ✅ Determine sort direction
//	    Sort.Direction sortDirection = Sort.Direction.ASC;
//	    if (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) {
//	        sortDirection = Sort.Direction.DESC;
//	    }
//
//	    // ✅ Create sort and pageable
//	    Sort sort = Sort.by(sortDirection, sortField);
//	    Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
//
//	    Page<ManageUsers> manageUsersPage;
//
//	    // ✅ Check if keyword is provided
//	    if (keyword == null || keyword.trim().isEmpty() || keyword.equalsIgnoreCase("empty")) {
//	        manageUsersPage = manageUserRepository.getAllManageUsersForSort(pageable);
//	    } else {
//	        manageUsersPage = manageUserRepository.searchManageUsers(keyword.trim(), pageable);
//	    }
//
//	    // ✅ Convert to DTO
//	    return manageUsersPage.map(this::convertToDTO);
//	}

	
	
	@Override
	public Page<ManageUserDTO> getAllManageUsersWithSorting(SortingRequestDTO sortingRequestDTO, String loggedInEmail) {
	    
	    String sortField = sortingRequestDTO.getSortField();
	    String sortOrder = sortingRequestDTO.getSortOrder();
	    String keyword = sortingRequestDTO.getKeyword();
	    Integer pageNo = sortingRequestDTO.getPageNumber();
	    Integer pageSize = sortingRequestDTO.getPageSize();

	    // ✅ Get current user and their role
	    User currentUser = getCurrentLoggedInUser(loggedInEmail);
	    String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;
	    String domain = extractDomain(currentUser.getEmail());

	    // ✅ Default values and validation
	    if (pageNo == null || pageNo < 0) {
	        pageNo = 0; // Default to first page
	    }
	    // ✅ If pageNo is 1 or greater, subtract 1 (convert to 0-based index)
	    // If pageNo is already 0, keep it as 0
	    int zeroBasedPageNo = (pageNo > 0) ? pageNo - 1 : pageNo;
	    
	    if (pageSize == null || pageSize < 1) {
	        pageSize = 10; // Default page size
	    }
	    if (sortField == null || sortField.isEmpty()) {
	        sortField = "id";
	    }

	    // ✅ Map frontend field names to entity field names
	    switch (sortField.toLowerCase()) {
	        case "name":
	        case "fullname":
	            sortField = "firstName";
	            break;
	        case "email":
	            sortField = "email";
	            break;
	        case "role":
	            sortField = "roleName";
	            break;
	        case "addedby":
	            sortField = "addedByName";
	            break;
	        case "updatedby":
	            sortField = "updatedByName";
	            break;
	        case "companyname":
	            sortField = "companyName";
	            break;
	        case "mobilenumber":
	            sortField = "mobileNumber";
	            break;
	        default:
	            sortField = "id";
	            break;
	    }

	    // ✅ Determine sort direction
	    Sort.Direction sortDirection = Sort.Direction.ASC;
	    if (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) {
	        sortDirection = Sort.Direction.DESC;
	    }

	    // ✅ Create sort and pageable (using 0-based index)
	    Sort sort = Sort.by(sortDirection, sortField);
	    Pageable pageable = PageRequest.of(zeroBasedPageNo, pageSize, sort);

	    Page<ManageUsers> manageUsersPage;

	    // ✅ Check if keyword is provided
	    boolean hasKeyword = keyword != null && !keyword.trim().isEmpty() && !keyword.equalsIgnoreCase("empty");

	    // ✅ Filter based on role
	    if ("SUPERADMIN".equalsIgnoreCase(roleName)) {
	        // SUPERADMIN sees ALL users
	        if (hasKeyword) {
	            manageUsersPage = manageUserRepository.searchManageUsers(keyword.trim(), pageable);
	        } else {
	            manageUsersPage = manageUserRepository.findAll(pageable);
	        }
	        
	    } else if ("ADMIN".equalsIgnoreCase(roleName)) {
	        // ADMIN sees ONLY their domain users
	        if (hasKeyword) {
	            manageUsersPage = manageUserRepository.searchManageUsersByDomain(keyword.trim(), domain, pageable);
	        } else {
	            manageUsersPage = manageUserRepository.getAllManageUsersByDomain(domain, pageable);
	        }
	        
	    } else {
	        // Regular user sees ONLY themselves
	        manageUsersPage = manageUserRepository.getManageUserByEmail(currentUser.getEmail(), pageable);
	    }

	    // ✅ Convert to DTO
	    return manageUsersPage.map(this::convertToDTO);
	}
	
	
/* Addedby Bhargav */
	

}