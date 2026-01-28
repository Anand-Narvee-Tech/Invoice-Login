package com.example.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.example.DTO.LoginRequest;
import com.example.DTO.ManageUserDTO;
import com.example.DTO.UserProfileResponse;
import com.example.config.MailConfig;
import com.example.entity.ManageUsers;
import com.example.entity.OTP;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.exception.BusinessException;
import com.example.repository.ManageUserRepository;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.repository.TokenRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

	private static final Set<String> DEFAULT_SUPERUSERS = Set.of("japhanya@narveetech.com", "wasim@narveetech.com");

	private final MailConfig mailConfig;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtServiceImpl jwtServiceImpl;

	@Autowired
	private TokenRepository tokenRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PrivilegeRepository privilegeRepository;

	@Autowired
	private ManageUserRepository manageUserRepository;

	@Autowired
	private JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	UserServiceImpl(MailConfig mailConfig) {
		this.mailConfig = mailConfig;
	}

	private ManageUserDTO convertToDTO(ManageUsers user) {
		return ManageUserDTO.builder().id(user.getId()).fullName(user.getFullName())
				.primaryEmail(user.getPrimaryEmail()).firstName(user.getFirstName()).middleName(user.getMiddleName())
				.lastName(user.getLastName()).email(user.getEmail()).roleName(user.getRoleName())
				.addedBy(user.getAddedBy() != null ? user.getAddedBy().getId().toString() : null)
				.updatedBy(user.getUpdatedBy()).addedByName(user.getAddedByName())
				.updatedByName(user.getUpdatedByName()).build();
	}

	//

	private String extractDomain(String email) {
		return email.substring(email.indexOf("@") + 1).toLowerCase();
	}

	// Bhargav

//    @Transactional
//    public ManageUserDTO registerCompanyUser(ManageUsers manageUsers) {
//
//        String email = manageUsers.getEmail().trim().toLowerCase();
//        String domain = extractDomain(email);
//
//        manageUsers.setCompanyDomain(domain);
//
//        boolean superAdminExists =
//            manageUserRepository.existsByCompanyDomainAndRoleNameIgnoreCase(domain, "SUPERADMIN");
//
//        if (superAdminExists) {
//            throw new BusinessException(
//                "Company already registered. Please contact your company administrator."
//            );
//        }
//
//        Role superAdminRole = roleRepository.findByRoleNameIgnoreCase("SUPERADMIN")
//            .orElseThrow(() -> new RuntimeException("SUPERADMIN role not found"));
//
//        manageUsers.setRole(superAdminRole);
//        manageUsers.setRoleName("SUPERADMIN");
//
//        manageUsers.setCreatedBy(null);
//        manageUsers.setAddedBy(null);
//        manageUsers.setAddedByName("SELF-REGISTERED");
//
//        ManageUsers saved = manageUserRepository.save(manageUsers);
//
//        User user = new User();
//        user.setEmail(saved.getEmail());
//        user.setFirstName(saved.getFirstName());
//        user.setMiddleName(saved.getMiddleName());
//        user.setLastName(saved.getLastName());
//        user.setFullName(saved.getFullName());
//        user.setPrimaryEmail(saved.getPrimaryEmail());
//        user.setApproved(true);
//        user.setActive(true);
//        user.setRole(superAdminRole);
//
//        userRepository.save(user);
//
//        return convertToDTO(saved);
//    }
//Bhargav

//    @Transactional
//    public ManageUserDTO registerCompanyUser(ManageUsers manageUsers) {
//
//        String email = manageUsers.getEmail().trim().toLowerCase();
//        String domain = extractDomain(email);
//
//        manageUsers.setCompanyDomain(domain);
//
//        // 🔒 Check if company already registered
//        boolean adminExists =
//            manageUserRepository.existsByCompanyDomainAndRoleNameIgnoreCase(domain, "ADMIN");
//
//        if (adminExists) {
//            throw new BusinessException(
//                "Company already registered. Please contact your company administrator."
//            );
//        }
//
//        // ⭐ FIRST USER → ADMIN (⬅️ HERE)
//        Role adminRole = roleRepository.findByRoleNameIgnoreCase("ADMIN")
//                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
//
//        manageUsers.setRole(adminRole);
//        manageUsers.setRoleName("ADMIN");
//
//        manageUsers.setApproved(true);
//        manageUsers.setActive(true);
//
//        manageUsers.setAddedByName("SELF-REGISTERED");
//
//        ManageUsers saved = manageUserRepository.save(manageUsers);
//
//        return convertToDTO(saved);
//    }

	@Transactional
	public ManageUserDTO registerCompanyUser(ManageUsers manageUsers) {

		final String ADMIN_ROLE = "ADMIN";

		// 1️⃣ Normalize email
		String email = manageUsers.getEmail().trim().toLowerCase();
		manageUsers.setEmail(email);
		manageUsers.setPrimaryEmail(email);

		// 2️⃣ Extract domain
		String domain = extractDomain(email);
		manageUsers.setCompanyDomain(domain);

		// 3️⃣ Check if ADMIN already exists
		boolean adminExists = manageUserRepository.existsByCompanyDomainAndRole_RoleNameIgnoreCase(domain, ADMIN_ROLE);

		if (adminExists) {
			throw new BusinessException("Company already registered. Please contact your company administrator.");
		}

		// 4️⃣ Fetch ADMIN role
		Role adminRole = roleRepository.findByRoleNameIgnoreCase(ADMIN_ROLE).orElseThrow(() -> new BusinessException(
				"Required role ADMIN is not configured. Please contact system administrator."));

		// 5️⃣ Create USER (🔥 REQUIRED FOR LOGIN)
		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
			User u = new User();
			u.setEmail(email);
			u.setFirstName(manageUsers.getFirstName());
			u.setApproved(true);
			u.setActive(true);
			u.setRole(adminRole);
			return userRepository.save(u);
		});

		// 6️⃣ Create ManageUsers
		manageUsers.setRole(adminRole);
		manageUsers.setRoleName(adminRole.getRoleName());
		manageUsers.setApproved(true);
		manageUsers.setActive(true);
		manageUsers.setAddedByName("SELF-REGISTERED");
		manageUsers.setCreatedBy(user); // important linkage
		manageUsers.setAddedBy(user);

		ManageUsers saved = manageUserRepository.save(manageUsers);

		return convertToDTO(saved);
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void initDefaultSuperAdmins() {

		log.info("Initializing default roles and super admins...");

		// 0️⃣ Ensure SYSTEM user exists
		User systemUser = userRepository.findByEmailIgnoreCase("system@narveetech.com").orElseGet(() -> {
			User u = new User();
			u.setEmail("system@narveetech.com");
			u.setFirstName("SYSTEM");
			u.setApproved(true);
			u.setActive(true);
			return userRepository.saveAndFlush(u);
		});

		// 1️⃣ Ensure ADMIN role exists ✅ (THIS WAS MISSING)
		Role adminRole = roleRepository.findByRoleNameIgnoreCase("ADMIN").orElseGet(() -> {
			Role role = new Role();
			role.setRoleName("ADMIN");
			role.setDescription("Company administrator role");
			role.setStatus("ACTIVE");
			role.setCreatedDate(LocalDateTime.now());
			return roleRepository.saveAndFlush(role);
		});

		// 2️⃣ Ensure SUPERADMIN role exists
		Role superAdminRole = roleRepository.findByRoleNameIgnoreCase("SUPERADMIN").orElseGet(() -> {
			Role role = new Role();
			role.setRoleName("SUPERADMIN");
			role.setDescription("Default super admin role with full privileges");
			role.setStatus("ACTIVE");
			role.setCreatedDate(LocalDateTime.now());
			return roleRepository.saveAndFlush(role);
		});

		// 3️⃣ Create default super admins
		for (String email : DEFAULT_SUPERUSERS) {

			String lowerEmail = email.trim().toLowerCase();
			String companyDomain = lowerEmail.substring(lowerEmail.indexOf("@") + 1);

			// Ensure User exists
			User user = userRepository.findByEmailIgnoreCase(lowerEmail).orElseGet(() -> {
				User u = new User();
				u.setEmail(lowerEmail);
				u.setFirstName(lowerEmail.split("@")[0]);
				u.setApproved(true);
				u.setActive(true);
				u.setRole(superAdminRole);
				return userRepository.saveAndFlush(u);
			});

			// Ensure ManageUsers entry exists
			if (!manageUserRepository.existsByEmailIgnoreCase(lowerEmail)) {

				ManageUsers mu = ManageUsers.builder().email(lowerEmail).firstName(user.getFirstName())
						.companyDomain(companyDomain).role(superAdminRole).roleName("SUPERADMIN").addedBy(systemUser)
						.createdBy(user).approved(true).active(true).build();

				manageUserRepository.saveAndFlush(mu);
			}
		}

		log.info("✅ Default roles and super admins initialized successfully");
	}

	/**
	 * ===================== Initialize default super admins =====================
	 **/
//	@EventListener(ApplicationReadyEvent.class)
//	@Transactional
//	public void initDefaultSuperAdmins() {
//
//		log.info("Initializing default super admins...");
//
//		// 0️⃣ Ensure SYSTEM user exists
//		User systemUser = userRepository.findByEmailIgnoreCase("system@narveetech.com").orElseGet(() -> {
//			User u = new User();
//			u.setEmail("system@narveetech.com");
//			u.setFirstName("SYSTEM");
//			u.setApproved(true);
//			u.setActive(true);
//			return userRepository.saveAndFlush(u);
//		});
//
//		// 1️⃣ Ensure SUPERADMIN role exists
//		Role superAdminRole = roleRepository.findByRoleName("SUPERADMIN").orElseGet(() -> {
//			Role role = Role.builder().roleName("SUPERADMIN")
//					.description("Default super admin role with full privileges").status("Active")
//					.createdDate(LocalDateTime.now()).build();
//			return roleRepository.saveAndFlush(role);
//		});
//
//		// 2️⃣ Loop through default superusers
//		for (String email : DEFAULT_SUPERUSERS) {
//
//			String lowerEmail = email.trim().toLowerCase();
//
//			// 🔥 Derive companyDomain safely
//			String companyDomain = lowerEmail.substring(lowerEmail.indexOf("@") + 1);
//
//			// 2a️⃣ Ensure User exists
//			User user = userRepository.findByEmailIgnoreCase(lowerEmail).orElseGet(() -> {
//				User u = new User();
//				u.setEmail(lowerEmail);
//				u.setFirstName(lowerEmail.split("@")[0]);
//				u.setApproved(true);
//				u.setActive(true);
//				u.setRole(superAdminRole);
//				return userRepository.saveAndFlush(u);
//			});
//
//			// 2b️⃣ Ensure ManageUsers entry exists
//			if (!manageUserRepository.existsByEmailIgnoreCase(lowerEmail)) {
//
//				ManageUsers mu = ManageUsers.builder().email(lowerEmail).firstName(user.getFirstName())
//						.companyDomain(companyDomain) // ✅ FIX (mandatory)
//						.roleName("SUPERADMIN").addedBy(systemUser).createdBy(user).approved(true).active(true).build();
//
//				manageUserRepository.saveAndFlush(mu);
//			}
//		}
//
//		log.info("✅ Default super admins initialized successfully");
//	}

	/** ===================== Register new user ===================== **/
	@Override
	public String register(User user) {
		if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
			throw new RuntimeException("Email is already registered!");
		}

		boolean isFirstUser = userRepository.count() == 0;

		if (isFirstUser) {
			Role superAdminRole = roleRepository.findByRoleName("ADMIN")
					.orElseThrow(() -> new RuntimeException("ADMIN role not found in DB!"));
			user.setRole(superAdminRole);
			user.setApproved(true);
			user.setActive(true);
		} else {
			user.setRole(null);
			user.setApproved(false);
			user.setActive(false);
		}

		userRepository.save(user);
		return "User registered successfully!";
	}

	@Transactional
	@Override
	public void sendOtp(String emailInput) {
		final String email = emailInput.trim().toLowerCase();

		// Fetch user or allow default super admin
		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
			if (DEFAULT_SUPERUSERS.contains(email)) {
				User u = new User();
				u.setEmail(email);
				u.setFirstName(email.split("@")[0]);
				u.setApproved(true);
				u.setActive(true);

				// Unwrap Optional<Role>
				Role superAdminRole = roleRepository.findByRoleName("ADMIN")
						.orElseThrow(() -> new RuntimeException("ADMIN role not found"));
				u.setRole(superAdminRole);

				return u;
			} else {
				throw new RuntimeException("Invalid credentials: email not registered");
			}
		});

		// Build full name
		String fullName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName()
				: (user.getFirstName() != null ? user.getFirstName() : email.split("@")[0]);
		String safeFullname = HtmlUtils.htmlEscape(fullName);

		// Remove old OTPs
		tokenRepository.deleteByEmail(email);

		// Generate new OTP
		String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
		long expiryTime = System.currentTimeMillis() + 2 * 60_000; // 2 minutes

		OTP otpEntity = new OTP(null, email, otp, expiryTime);
		tokenRepository.save(otpEntity);

		// Send email with designed HTML
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			helper.setFrom(fromEmail);
			helper.setTo(email);
			helper.setSubject("Login Verification Code - Invoicing Team");

			String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
					+ "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
					+ "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
					+ "<tr>"
					+ "<td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
					+ "<h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>" + "</td>" + "</tr>" + "<tr>"
					+ "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
					+ "<p style='font-size:15px; color:#333;'>" + "Hello " + safeFullname + ",<br><br>"
					+ "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Sign-In:"
					+ "</p>" + "<div style='text-align:center; margin:25px 0;'>"
					+ "<span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9;'>"
					+ otp + "</span>" + "</div>" + "<p style='font-size:14px; color:#555;'>"
					+ "This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone." + "</p>"
					+ "<p style='font-size:14px; color:#333; margin-top:30px;'>"
					+ "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
					+ "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
					+ "2025 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
					+ "</html>";

			helper.setText(htmlContent, true);
			javaMailSender.send(mimeMessage);
			log.info("OTP sent successfully to {}", email);
		} catch (Exception e) {
			log.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
		}
	}

	/** ===================== Login with OTP ===================== **/
	@Override
	@Transactional
	public Map<String, Object> loginWithOtp(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		String enteredOtp = request.getOtp();

		// Fetch ManageUsers
		ManageUsers manageUser = manageUserRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new RuntimeException("Invalid credentials: email not registered"));

		// Validate OTP
		OTP otpEntity = tokenRepository.findByEmailAndOtp(email, enteredOtp)
				.orElseThrow(() -> new RuntimeException("Invalid OTP or email"));
		if (System.currentTimeMillis() > otpEntity.getExpiryTime()) {
			tokenRepository.deleteByEmail(email);
			throw new RuntimeException("OTP has expired");
		}
		tokenRepository.deleteByEmail(email);

		// Fetch linked User
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new RuntimeException("User not found in system"));

		if (!user.getActive()) {
			throw new RuntimeException("User is inactive. Contact admin.");
		}

		// Determine role and selected privileges
		String roleName = manageUser.getRoleName();
		Set<String> privilegeNames = new HashSet<>();
		if (roleName != null) {
			Role roleEntity = roleRepository.findByRoleNameIgnoreCase(roleName).orElse(null);
			if (roleEntity != null && roleEntity.getPrivileges() != null) {
				// Include only selected privileges
				privilegeNames = roleEntity.getPrivileges().stream().map(Privilege::getName)
						.collect(Collectors.toSet());
			}
		}

		// Generate JWT with selected privileges
		String jwtToken = jwtServiceImpl.generateToken(user, roleName, privilegeNames);

		// Prepare response
		Map<String, Object> data = new HashMap<>();
		data.put("token", jwtToken);
		data.put("userId", manageUser.getId());
		data.put("email", user.getEmail());
		data.put("firstName", user.getFirstName());
		data.put("middleName", user.getMiddleName());
		data.put("lastName", user.getLastName());
		data.put("userRole", roleName);
		data.put("rolePrivileges", privilegeNames);

		// Admin info
		User admin = manageUser.getAddedBy();
		data.put("adminId", admin != null ? admin.getId() : null);
		data.put("adminName", admin != null ? admin.getFullName() : "SYSTEM");
		data.put("adminEmail", admin != null ? admin.getEmail() : null);

		// Creator info
		User creator = manageUser.getCreatedBy();
		if (creator != null) {
			data.put("createdById", creator.getId());
			data.put("createdByName", creator.getFullName());
			data.put("createdByEmail", creator.getEmail());
		}

		return Map.of("status", "success", "message", "User logged in successfully", "data", data, "pagesize", 0,
				"timeStamp", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
	}

	/** ===================== Other Methods ===================== **/
	@Override
	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	public User updateUserProfile(Long id, User updatedProfile) {
		return userRepository.findById(id).map(existingUser -> {
			if (updatedProfile.getFullName() != null)
				existingUser.setFullName(updatedProfile.getFullName());
			if (updatedProfile.getPrimaryEmail() != null)
				existingUser.setPrimaryEmail(updatedProfile.getPrimaryEmail());
			if (updatedProfile.getAlternativeEmail() != null)
				existingUser.setAlternativeEmail(updatedProfile.getAlternativeEmail());
			if (updatedProfile.getMobileNumber() != null)
				existingUser.setMobileNumber(updatedProfile.getMobileNumber());
			if (updatedProfile.getAlternativeMobileNumber() != null)
				existingUser.setAlternativeMobileNumber(updatedProfile.getAlternativeMobileNumber());
			if (updatedProfile.getCompanyName() != null)
				existingUser.setCompanyName(updatedProfile.getCompanyName());
			if (updatedProfile.getTaxId() != null)
				existingUser.setTaxId(updatedProfile.getTaxId());
			if (updatedProfile.getBusinessId() != null)
				existingUser.setBusinessId(updatedProfile.getBusinessId());
			if (updatedProfile.getPreferredCurrency() != null)
				existingUser.setPreferredCurrency(updatedProfile.getPreferredCurrency());
			if (updatedProfile.getInvoicePrefix() != null)
				existingUser.setInvoicePrefix(updatedProfile.getInvoicePrefix());
			return userRepository.save(existingUser);
		}).orElseThrow(() -> new RuntimeException("User not found with id " + id));
	}

	@Override
	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email);
	}

	@Override
	public Map<String, Object> getPrivilegesForUser(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		Role role = user.getRole();
		if (role == null)
			throw new RuntimeException("User has no role assigned");

		Set<Privilege> privileges;
		if ("ADMIN".equalsIgnoreCase(role.getRoleName())) {
			privileges = new HashSet<>(privilegeRepository.findAll());
		} else {
			privileges = role.getPrivileges();
		}

		Map<String, Object> result = new HashMap<>();
		result.put("role", role.getRoleName());
		result.put("privileges", privileges.stream()
				.map(p -> Map.of("id", p.getId(), "name", p.getName(), "cardType", p.getCardType(), "selected", true))
				.toList());
		return result;
	}

	private String getSafeFullName(User user) {
		String name = String.join(" ", Optional.ofNullable(user.getFirstName()).orElse(""),
				Optional.ofNullable(user.getMiddleName()).orElse(""),
				Optional.ofNullable(user.getLastName()).orElse("")).trim();
		return name.isEmpty() ? user.getEmail().split("@")[0] : name;
	}

	@Override
	public UserProfileResponse getUserProfileByEmail(String email) {

		String normalizedEmail = email.trim().toLowerCase();

		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
		Optional<ManageUsers> muOpt = manageUserRepository.findByEmailIgnoreCase(normalizedEmail);

		if (userOpt.isEmpty() && muOpt.isEmpty()) {
			return null;
		}

		User user = userOpt.orElse(null);
		ManageUsers mu = muOpt.orElse(null);

		return UserProfileResponse.builder().id(user != null ? user.getId() : 0L).fullName(resolveFullName(user, mu))
				.primaryEmail(mu != null ? mu.getEmail() : normalizedEmail)
				.mobileNumber(user != null ? safe(user.getMobileNumber()) : "")
				.alternativeEmail(user != null ? safe(user.getAlternativeEmail()) : "")
				.alternativeMobileNumber(user != null ? safe(user.getAlternativeMobileNumber()) : "")
				.companyName(user != null ? safe(user.getCompanyName()) : "")
				.taxId(user != null ? safe(user.getTaxId()) : "")
				.businessId(user != null ? safe(user.getBusinessId()) : "")
				.preferredCurrency(user != null ? safe(user.getPreferredCurrency()) : "")
				.invoicePrefix(user != null ? safe(user.getInvoicePrefix()) : "")
				.profilePicPath(user != null ? safe(user.getProfilePicPath()) : "")
				.role(mu != null && mu.getRole() != null ? mu.getRole().getRoleName() : "").build();
	}

	// ---------- Helper Methods ----------

	private String resolveFullName(User user, ManageUsers mu) {

		if (mu != null && hasText(mu.getFullName())) {
			return mu.getFullName();
		}

		if (user != null && hasText(user.getFullName())) {
			return user.getFullName();
		}

		return "";
	}

	private String safe(String value) {
		return value != null ? value : "";
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}