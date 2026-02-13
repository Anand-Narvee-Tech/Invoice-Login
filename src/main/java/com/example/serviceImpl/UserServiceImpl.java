package com.example.serviceImpl;

import java.awt.print.Pageable;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.HtmlUtils;

import com.example.DTO.LoginRequest;
import com.example.DTO.ManageUserDTO;
import com.example.DTO.RegisterRequest;
import com.example.DTO.SortingRequestDTO;
import com.example.DTO.UserProfileResponse;
import com.example.commons.RestAPIResponse;
import com.example.config.MailConfig;
import com.example.entity.ManageUsers;
import com.example.entity.OTP;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.entity.VerifyOtpRequest;
import com.example.entity.VerifyOtpRequest;
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
		return ManageUserDTO.builder().id(user.getId()).fullName(user.getFullName()).firstName(user.getFirstName())
				.middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail())
				.primaryEmail(user.getPrimaryEmail()).mobileNumber(user.getMobileNumber())
				.companyName(user.getCompanyName()).roleName(user.getRoleName())
				.addedBy(user.getAddedBy() != null ? user.getAddedBy().getId().toString() : null)
				.addedByName(user.getAddedByName()).updatedByName(user.getUpdatedByName())
				// ✅ ADD THESE
				.state(user.getState()).country(user.getCountry()).pincode(user.getPincode()).city(user.getCity())
				.telephone(user.getTelephone()).ein(user.getEin()).gstin(user.getGstin()).website(user.getWebsite())
				.address(user.getAddress()).build();
	}

	//

	private String extractDomain(String email) {
		return email.substring(email.indexOf("@") + 1).toLowerCase();
	}

	// Bhargav
	public boolean isEmailDuplicate(String email) {
		return userRepository.existsByEmail(email);
	}

	// Bhargav

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

//Bhargav working
	@Transactional
	public ManageUserDTO registerCompanyUser(ManageUsers manageUsers) {

		final String ADMIN_ROLE = "ADMIN";

		// 🔥 preserve incoming values
		String mobileNumber = manageUsers.getMobileNumber();
		String companyName = manageUsers.getCompanyName();

		String State = manageUsers.getState();
		String Country = manageUsers.getCountry();
		String City = manageUsers.getCity();
		String Pincode = manageUsers.getPincode();
		String Telephone = manageUsers.getTelephone();
		String Ein = manageUsers.getEin();
		String Gstin = manageUsers.getGstin();
		String Website = manageUsers.getWebsite();
		String Address = manageUsers.getAddress();

		// 1️⃣ Normalize email
		String email = manageUsers.getEmail().trim().toLowerCase();
		manageUsers.setEmail(email);
		manageUsers.setPrimaryEmail(email);

		// 2️⃣ Extract domain
		String domain = extractDomain(email);
		manageUsers.setCompanyDomain(domain);

		// 3️⃣ Check if ADMIN already exists
		if (manageUserRepository.existsByCompanyDomainAndRole_RoleNameIgnoreCase(domain, ADMIN_ROLE)) {
			throw new BusinessException("Company already registered. Please contact your company administrator.");
		}

		// 4️⃣ Fetch ADMIN role
		Role adminRole = roleRepository.findByRoleNameIgnoreCase(ADMIN_ROLE).orElseThrow(() -> new BusinessException(
				"Required role ADMIN is not configured. Please contact system administrator."));

		// 5️⃣ Create USER
		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
			User u = new User();
			u.setEmail(email);
			u.setFirstName(manageUsers.getFirstName());
			u.setCompanyName(companyName);
			u.setMobileNumber(mobileNumber);
			// 🔽 newly added fields
			u.setState(State);
			u.setCountry(Country);
			u.setCity(City);
			u.setPincode(Pincode);
			u.setTelephone(Telephone);
			u.setEin(Ein);
			u.setGstin(Gstin);
			u.setWebsite(Website);
			u.setAddress(Address);
			u.setApproved(true);
			u.setActive(true);
			u.setRole(adminRole);

			return userRepository.save(u);
		});

		// 6️⃣ Restore preserved fields 🔥
		manageUsers.setMobileNumber(mobileNumber);
		manageUsers.setCompanyName(companyName);
		manageUsers.setState(State);
		manageUsers.setCountry(Country);
		manageUsers.setCity(City);
		manageUsers.setPincode(Pincode);
		manageUsers.setTelephone(Telephone);
		manageUsers.setEin(Ein);
		manageUsers.setGstin(Gstin);
		manageUsers.setWebsite(Website);
		manageUsers.setAddress(Address);
		manageUsers.setApproved(true);
		manageUsers.setActive(true);
		manageUsers.setRole(adminRole);

		// 7️⃣ Create ManageUsers
		manageUsers.setRole(adminRole);
		manageUsers.setRoleName(adminRole.getRoleName());
		manageUsers.setApproved(true);
		manageUsers.setActive(true);
		manageUsers.setAddedByName("SELF-REGISTERED");
		manageUsers.setCreatedBy(user);
		manageUsers.setAddedBy(user);
		ManageUsers saved = manageUserRepository.save(manageUsers);
		return convertToDTO(saved);
	}

//Bhargav working 

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

//	comment by Bhargav 
//it working fine Generate integer OTP

//	@Transactional
//	@Override
//	public void sendOtp(String emailInput) {
//		final String email = emailInput.trim().toLowerCase();
//
//		// Fetch user or allow default super admin
//		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
//			if (DEFAULT_SUPERUSERS.contains(email)) {
//				User u = new User();
//				u.setEmail(email);
//				u.setFirstName(email.split("@")[0]);
//				u.setApproved(true);
//				u.setActive(true);
//
//				// Unwrap Optional<Role>
//				Role superAdminRole = roleRepository.findByRoleName("ADMIN")
//						.orElseThrow(() -> new RuntimeException("ADMIN role not found"));
//				u.setRole(superAdminRole);
//
//				return u;
//			} else {
//				throw new RuntimeException("Invalid credentials: email not registered");
//			}
//		});
//
//		// Build full name
//		String fullName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName()
//				: (user.getFirstName() != null ? user.getFirstName() : email.split("@")[0]);
//		String safeFullname = HtmlUtils.htmlEscape(fullName);
//
//		// Remove old OTPs
//		tokenRepository.deleteByEmail(email);
//
//		// Generate new OTP
//		String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
//		long expiryTime = System.currentTimeMillis() + 2 * 60_000; // 2 minutes
//
//		OTP otpEntity = new OTP(null, email, otp, expiryTime);
//		tokenRepository.save(otpEntity);
//
//		// Send email with designed HTML
//		try {
//			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
//			helper.setFrom(fromEmail);
//			helper.setTo(email);
//			helper.setSubject("Login Verification Code - Invoicing Team");
//
//			String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
//					+ "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
//					+ "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
//					+ "<tr>"
//					+ "<td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
//					+ "<h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>" + "</td>" + "</tr>" + "<tr>"
//					+ "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
//					+ "<p style='font-size:15px; color:#333;'>" + "Hello " + safeFullname + ",<br><br>"
//					+ "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Sign-In:"
//					+ "</p>" + "<div style='text-align:center; margin:25px 0;'>"
//					+ "<span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9;'>"
//					+ otp + "</span>" + "</div>" + "<p style='font-size:14px; color:#555;'>"
//					+ "This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone." + "</p>"
//					+ "<p style='font-size:14px; color:#333; margin-top:30px;'>"
//					+ "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
//					+ "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
//					+ "2025 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
//					+ "</html>";
//
//			helper.setText(htmlContent, true);
//			javaMailSender.send(mimeMessage);
//			log.info("OTP sent successfully to {}", email);
//		} catch (Exception e) {
//			log.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
//		}
//	}
//	
//	@Transactional
//	@Override
//	public void sendOtpForRegister(String emailInput) {
//
//	    final String email = emailInput.trim().toLowerCase();
//
//	    // ❌ Block if email already exists
//	    if (userRepository.existsByEmailIgnoreCase(email)) {
//	        throw new RuntimeException("Email already registered. Please login.");
//	    }
//
//	    // Clean old OTPs
//	    tokenRepository.deleteByEmail(email);
//
//	    // Generate OTP
//	    String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
//	    long expiryTime = System.currentTimeMillis() + 2 * 60_000;
//
//	    OTP otpEntity = new OTP(null, email, otp, expiryTime);
//	    tokenRepository.save(otpEntity);
//
//	    // Send email (reuse SAME template)
//	    sendOtpEmail(email, email.split("@")[0], otp);
//
//	    log.info("Registration OTP sent to {}", email);
//	}
//	private void sendOtpEmail(String email, String fullName, String otp) {
//	    try {
//	        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
//
//	        helper.setFrom(fromEmail);
//	        helper.setTo(email);
//	        helper.setSubject("Verification Code - Invoicing Team");
//
//	        String safeFullname = HtmlUtils.htmlEscape(fullName);
//
//	        String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
//					+ "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
//					+ "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
//					+ "<tr>"
//					+ "<td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
//					+ "<h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>" + "</td>" + "</tr>" + "<tr>"
//					+ "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
//					+ "<p style='font-size:15px; color:#333;'>" + "Hello " + safeFullname + ",<br><br>"
//					+ "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Sign-In:"
//					+ "</p>" + "<div style='text-align:center; margin:25px 0;'>"
//					+ "<span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9;'>"
//					+ otp + "</span>" + "</div>" + "<p style='font-size:14px; color:#555;'>"
//					+ "This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone." + "</p>"
//					+ "<p style='font-size:14px; color:#333; margin-top:30px;'>"
//					+ "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
//					+ "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
//					+ "2025 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
//					+ "</html>";
//	        helper.setText(htmlContent, true);
//	        javaMailSender.send(mimeMessage);
//
//	    } catch (Exception e) {
//	        log.error("Failed to send OTP email to {}", email, e);
//	    }
//	}
//comment by Bhargav 	

//Added by Bhargav Generate alphanumeric OTP 11-02-26 new 

	/**
	 * Generate alphanumeric OTP
	 * 
	 * @param length - length of OTP (default: 6)
	 * @return alphanumeric OTP string
	 */
	private String generateAlphanumericOTP(int length) {
		if (length != 6) {
			throw new IllegalArgumentException("OTP length must be 6 for this pattern");
		}

		String alphabets = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String numbers = "0123456789";

		Random random = new Random();

		StringBuilder otp = new StringBuilder();

		// Generate 3 random alphabets
		for (int i = 0; i < 3; i++) {
			otp.append(alphabets.charAt(random.nextInt(alphabets.length())));
		}

		// Generate 3 random numbers
		for (int i = 0; i < 3; i++) {
			otp.append(numbers.charAt(random.nextInt(numbers.length())));
		}

		// Now shuffle them so pattern is mixed like A7K9M2
		List<Character> otpChars = new ArrayList<>();
		for (char c : otp.toString().toCharArray()) {
			otpChars.add(c);
		}

		Collections.shuffle(otpChars);

		StringBuilder finalOtp = new StringBuilder();
		for (char c : otpChars) {
			finalOtp.append(c);
		}

		return finalOtp.toString();
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

		// ✅ Generate new ALPHANUMERIC OTP
		String otp = generateAlphanumericOTP(6); // 6-character alphanumeric OTP
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

//	        String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
//	                + "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
//	                + "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
//	                + "<tr>"
//	                + "<td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
//	                + "<h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>" + "</td>" + "</tr>" + "<tr>"
//	                + "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
//	                + "<p style='font-size:15px; color:#333;'>" + "Hello " + safeFullname + ",<br><br>"
//	                + "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Sign-In:"
//	                + "</p>" + "<div style='text-align:center; margin:25px 0;'>"
//	                + "<span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9; letter-spacing:3px;'>"
//	                + otp + "</span>" + "</div>" + "<p style='font-size:14px; color:#555;'>"
//	                + "This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone." + "</p>"
//	                + "<p style='font-size:14px; color:#333; margin-top:30px;'>"
//	                + "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
//	                + "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
//	                + "2025 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
//	                + "</html>";

			String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
					+ "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
					+ "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
					+ "<tr>"
					+ "<td align='center' bgcolor='#2563eb' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
					+ "<h2 style='color:#ffffff; margin:0;'> Invoice </h2>" + "</td>" + "</tr>" + "<tr>"
					+ "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
					+ "<p style='font-size:16px; color:#4b5563;'>" + "Hello <strong>" + safeFullname
					+ "</strong>,<br><br>"

					+ "Thank you for choosing <b>Invoicing Application</b>. Your verification code is:" + "</p>"
					+ "<div style='text-align:center; margin:32px 0;'>"
					+ "<div style='display:inline-block; padding:18px 32px; border-radius:12px; border:2px dashed #2563eb; background:#eff6ff; font-size:36px; font-weight:700; letter-spacing:8px; color:#1e3a8a;'>"
					+ otp + "</div>" + "</div>" + "<p style='text-align:center; font-size:15px; color:#6b7280;'>"
					+ "This OTP is valid for <strong>2 minutes</strong>. Please do not share this code with anyone."
					+ "</p>" + "<p style='font-size:14px; color:#333; margin-top:30px;'>"
					+ "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
					+ "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
					+ "2026 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
					+ "</html>";

			helper.setText(htmlContent, true);
			javaMailSender.send(mimeMessage);
			log.info("OTP sent successfully to {}", email);
		} catch (Exception e) {
			log.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
		}
	}

	@Transactional
	@Override
	public void sendOtpForRegister(String emailInput) {

		final String email = emailInput.trim().toLowerCase();

		// ❌ Block if email already exists
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new RuntimeException("Email already registered. Please login.");
		}

		// Clean old OTPs
		tokenRepository.deleteByEmail(email);

		// ✅ Generate ALPHANUMERIC OTP
		String otp = generateAlphanumericOTP(6); // 6-character alphanumeric OTP
		long expiryTime = System.currentTimeMillis() + 2 * 60_000;

		OTP otpEntity = new OTP(null, email, otp, expiryTime);
		tokenRepository.save(otpEntity);

		// Send email (reuse SAME template)
		sendOtpEmail(email, email.split("@")[0], otp);

		log.info("Registration OTP sent to {}", email);
	}

	private void sendOtpEmail(String email, String fullName, String otp) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

			helper.setFrom(fromEmail);
			helper.setTo(email);
			helper.setSubject("Verification Code - Invoicing Team");

			String safeFullname = HtmlUtils.htmlEscape(fullName);

			String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head><meta charset='UTF-8'></head>"
					+ "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
					+ "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
					
					+ "<tr>"
					+ "<td align='center' bgcolor='#2563eb' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
					+ "<h2 style='color:#ffffff; margin:0;'>Verify Your Registration</h2>" + "</td>" + "</tr>" + "<tr>"
					+ "<td style='padding:30px;'>" + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
					+ "<p style='font-size:16px; color:#4b5563;'>" + "Hello <strong>" + safeFullname
					+ "</strong>,<br><br>"
					
					+ "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Registration:"
					+ "</p>" + "<div style='text-align:center; margin:32px 0;'>"
					+ "<div style='display:inline-block; padding:18px 32px; border-radius:12px; border:2px dashed #2563eb; background:#eff6ff; font-size:36px; font-weight:700; letter-spacing:8px; color:#1e3a8a;'>"
					+ otp + "</div>" + "</div>" + "<p style='text-align:center; font-size:15px; color:#6b7280;'>"
					+ "This OTP is valid for <strong>2 minutes</strong>. Please do not share this code with anyone."

					+ "</p>" + "<p style='font-size:14px; color:#333; margin-top:30px;'>"
					+ "Best Regards,<br><b>Invoicing Team</b>" + "</p>" + "</td>" + "</tr>" + "<tr>"
					+ "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
					+ "2026 Invoicing Team. All rights reserved." + "</td>" + "</tr>" + "</table>" + "</body>"
					+ "</html>";

			helper.setText(htmlContent, true);
			javaMailSender.send(mimeMessage);

		} catch (Exception e) {
			log.error("Failed to send OTP email to {}", email, e);
		}
	}

	// Added by Bhargav Generate alphanumeric OTP 11-02-26 new

//comment by Bhargav 	
//	@Transactional
//	@Override
//	public void sendOtp(String emailInput, String purpose) {
//
//	    final String email = emailInput.trim().toLowerCase();
//
//	    Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
//
//	    User user = null;
//
//	    if ("LOGIN".equals(purpose)) {
//
//	        // -------- LOGIN FLOW --------
//	        user = optionalUser.orElseGet(() -> {
//	            if (DEFAULT_SUPERUSERS.contains(email)) {
//	                User u = new User();
//	                u.setEmail(email);
//	                u.setFirstName(email.split("@")[0]);
//	                u.setApproved(true);
//	                u.setActive(true);
//
//	                Role superAdminRole = roleRepository.findByRoleName("ADMIN")
//	                        .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
//
//	                u.setRole(superAdminRole);
//
//	                return u;
//	            } else {
//	                throw new RuntimeException("Email not registered. Please register first.");
//	            }
//	        });
//
//	    } else if ("REGISTER".equals(purpose)) {
//
//	        // -------- REGISTER FLOW --------
//	        if (optionalUser.isPresent()) {
//	            throw new RuntimeException("Email already registered. Please login.");
//	        }
//
//	        // Create temporary user object for OTP only
//	        user = new User();
//	        user.setEmail(email);
//	        user.setFirstName(email.split("@")[0]);
//
//	    } else {
//	        throw new RuntimeException("Invalid purpose. Use LOGIN or REGISTER");
//	    }
//
//	    // Build name
//	    String fullName = (user.getFullName() != null && !user.getFullName().isBlank())
//	            ? user.getFullName()
//	            : (user.getFirstName() != null ? user.getFirstName() : email.split("@")[0]);
//
//	    String safeFullname = HtmlUtils.htmlEscape(fullName);
//
//	    // Delete old OTPs
//	    tokenRepository.deleteByEmail(email);
//
//	    // Generate OTP
//	    String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
//	    long expiryTime = System.currentTimeMillis() + 2 * 60_000;
//
//	    OTP otpEntity = new OTP(null, email, otp, expiryTime);
//	    tokenRepository.save(otpEntity);
//
//	    // Send Email
//	    try {
//	        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
//
//	        helper.setFrom(fromEmail);
//	        helper.setTo(email);
//
//	        if ("LOGIN".equals(purpose)) {
//	            helper.setSubject("Login OTP - Invoicing App");
//	        } else {
//	            helper.setSubject("Registration OTP - Invoicing App");
//	        }
//
//	        String htmlContent =
//	                "<html><body>" +
//	                "<h3>Hello " + safeFullname + ",</h3>" +
//	                "<p>Your OTP for <b>" + purpose + "</b> is:</p>" +
//	                "<h2>" + otp + "</h2>" +
//	                "<p>This OTP is valid for 2 minutes.</p>" +
//	                "</body></html>";
//
//	        helper.setText(htmlContent, true);
//
//	        javaMailSender.send(mimeMessage);
//
//	    } catch (Exception e) {
//	        log.error("Failed to send OTP: {}", e.getMessage());
//	        throw new RuntimeException("Failed to send OTP email");
//	    }
//	}
//
//comment by Bhargav 

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
				.primaryEmail(user != null && hasText(user.getPrimaryEmail()) ? user.getPrimaryEmail()
						: mu != null ? safe(mu.getEmail()) : normalizedEmail)

				.mobileNumber(user != null && hasText(user.getMobileNumber()) ? user.getMobileNumber()
						: mu != null ? safe(mu.getMobileNumber()) : "")

				.alternativeEmail(user != null && hasText(user.getAlternativeEmail()) ? user.getAlternativeEmail() : "")

				.alternativeMobileNumber(
						user != null && hasText(user.getAlternativeMobileNumber()) ? user.getAlternativeMobileNumber()
								: "")

				.companyName(user != null && hasText(user.getCompanyName()) ? user.getCompanyName()
						: mu != null ? safe(mu.getCompanyName()) : "")

				// ✅ Address & Company Details
				.state(user != null && hasText(user.getState()) ? user.getState() : "")
				.country(user != null && hasText(user.getCountry()) ? user.getCountry() : "")
				.city(user != null && hasText(user.getCity()) ? user.getCity() : "")
				.pincode(user != null && hasText(user.getPincode()) ? user.getPincode() : "")
				.telephone(user != null && hasText(user.getTelephone()) ? user.getTelephone() : "")
				.ein(user != null && hasText(user.getEin()) ? user.getEin() : "")
				.gstin(user != null && hasText(user.getGstin()) ? user.getGstin() : "")
				.website(user != null && hasText(user.getWebsite()) ? user.getWebsite() : "")
				.address(user != null && hasText(user.getAddress()) ? user.getAddress() : "")

				// ✅ Newly Added Fields
				.fid(user != null && hasText(user.getFid()) ? user.getFid() : "")
				.everifyId(user != null && hasText(user.getEverifyId()) ? user.getEverifyId() : "")
				.dunsNumber(user != null && hasText(user.getDunsNumber()) ? user.getDunsNumber() : "")
				.stateOfIncorporation(
						user != null && hasText(user.getStateOfIncorporation()) ? user.getStateOfIncorporation() : "")
				.naicsCode(user != null && hasText(user.getNaicsCode()) ? user.getNaicsCode() : "")
				.signingAuthorityName(
						user != null && hasText(user.getSigningAuthorityName()) ? user.getSigningAuthorityName() : "")
				.designation(user != null && hasText(user.getDesignation()) ? user.getDesignation() : "")
				.dateOfIncorporation(
						user != null && hasText(user.getDateOfIncorporation()) ? user.getDateOfIncorporation() : "")

				// ✅ Bank Details (Safe Handling)
				.bankDetails(
						user != null && user.getBankDetails() != null ? user.getBankDetails() : Collections.emptyList())

				.taxId(user != null && hasText(user.getTaxId()) ? user.getTaxId() : "")
				.businessId(user != null && hasText(user.getBusinessId()) ? user.getBusinessId() : "")
				.preferredCurrency(
						user != null && hasText(user.getPreferredCurrency()) ? user.getPreferredCurrency() : "")
				.invoicePrefix(user != null && hasText(user.getInvoicePrefix()) ? user.getInvoicePrefix() : "")
				.profilePicPath(user != null && hasText(user.getProfilePicPath()) ? user.getProfilePicPath() : "")

				.role(mu != null && mu.getRole() != null ? mu.getRole().getRoleName()
						: user != null && user.getRole() != null ? user.getRole().getRoleName() : "")
				.build();

	}

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

//Bhargav

	@Override
	@Transactional
	public boolean verifyOtp(String emailInput, String otpInput) {

		final String email = emailInput.trim().toLowerCase();

		// Fetch OTP record
		OTP otpEntity = tokenRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("OTP not found for this email"));

		// Check expiry
		if (System.currentTimeMillis() > otpEntity.getExpiryTime()) {
			tokenRepository.deleteByEmail(email); // remove expired OTP
			throw new RuntimeException("OTP has expired");
		}

		// Validate OTP
		if (!otpEntity.getOtp().equals(otpInput)) {
			throw new RuntimeException("Invalid OTP");
		}

		// OTP is valid → delete it after successful verification
		tokenRepository.deleteByEmail(email);

		return true;
	}

	public ManageUsers buildManageUsersFromRequest(RegisterRequest request) {

		ManageUsers manageUsers = new ManageUsers();

		manageUsers.setFirstName(request.getFirstName());
		manageUsers.setMiddleName(request.getMiddleName());
		manageUsers.setLastName(request.getLastName());

		// Build full name if required
		String fullName = request.getFirstName() + " "
				+ (request.getMiddleName() != null ? request.getMiddleName() + " " : "") + request.getLastName();

		manageUsers.setFullName(fullName.trim());

		manageUsers.setEmail(request.getEmail());
		manageUsers.setPrimaryEmail(request.getEmail());

		manageUsers.setMobileNumber(request.getMobileNumber());

		manageUsers.setCompanyName(request.getCompanyName());

		manageUsers.setState(request.getState());
		manageUsers.setCity(request.getCity());
		manageUsers.setCountry(request.getCountry());

		manageUsers.setPincode(request.getPincode());
		manageUsers.setTelephone(request.getTelephone());

		manageUsers.setEin(request.getEin());
		manageUsers.setGstin(request.getGstin());

		manageUsers.setWebsite(request.getWebsite());
		manageUsers.setAddress(request.getAddress());

		// Optional fields (if available in RegisterRequest)
//		    manageUsers.setAlternativeEmail(request.getAlternativeEmail());
//		    manageUsers.setAlternativeMobileNumber(request.getAlternativeMobileNumber());

		// Default flags
		manageUsers.setActive(true);
		manageUsers.setApproved(true);

		return manageUsers;
	}

//Bhargav

}