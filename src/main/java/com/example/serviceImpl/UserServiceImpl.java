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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.example.DTO.LoginRequest;
import com.example.config.MailConfig;
import com.example.entity.ManageUsers;
import com.example.entity.OTP;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.ManageUserRepository;
import com.example.repository.PrivilegeRepository;
import com.example.repository.RoleRepository;
import com.example.repository.TokenRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
	
	 private static final Set<String> DEFAULT_SUPERUSERS = Set.of(
	            "japhanya@narveetech.com",
	            "wasim@narveetech.com"
	    );


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

   
    /** ===================== Initialize default super admins ===================== **/
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultSuperAdmins() {
        log.info("Initializing default super admins...");

        // 0️⃣ Ensure SYSTEM user exists (used as addedBy)
        User systemUser = userRepository.findByEmailIgnoreCase("system@narveetech.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("system@narveetech.com");
                    u.setFirstName("SYSTEM");
                    u.setApproved(true);
                    u.setActive(true);
                    return userRepository.saveAndFlush(u);
                });

        // 1️⃣ Ensure SUPERADMIN role exists
        Role superAdminRole = roleRepository.findByRoleName("SUPERADMIN")
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .roleName("SUPERADMIN")
                            .description("Default super admin role with full privileges")
                            .status("Active")
                            .createdDate(LocalDateTime.now())
                            .build();
                    return roleRepository.saveAndFlush(role);
                });

        // 2️⃣ Loop through default superusers
        for (String email : DEFAULT_SUPERUSERS) {
            String lowerEmail = email.trim().toLowerCase();

            // 2a️⃣ Ensure User exists
            User user = userRepository.findByEmailIgnoreCase(lowerEmail)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setEmail(lowerEmail);
                        u.setFirstName(lowerEmail.split("@")[0]);
                        u.setApproved(true);
                        u.setActive(true);
                        u.setRole(superAdminRole); // set role
                        return userRepository.saveAndFlush(u);
                    });

            // 2b️⃣ Ensure ManageUsers entry exists
            if (!manageUserRepository.existsByEmailIgnoreCase(lowerEmail)) {
                ManageUsers mu = ManageUsers.builder()
                        .email(lowerEmail)
                        .firstName(user.getFirstName())
                        .roleName("SUPERADMIN")
                        .addedBy(systemUser)          // ✅ pass SYSTEM user object
                        .createdBy(user)              // ✅ set createdBy as the user themselves
                        .build();
                manageUserRepository.saveAndFlush(mu);
            }
        }

        log.info("✅ Default super admins initialized successfully");
    }

    /** ===================== Register new user ===================== **/
    @Override
    public String register(User user) {
        if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered!");
        }

        boolean isFirstUser = userRepository.count() == 0;

        if (isFirstUser) {
            Role superAdminRole = roleRepository.findByRoleName("SUPERADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPERADMIN role not found in DB!"));
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
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    if (DEFAULT_SUPERUSERS.contains(email)) {
                        User u = new User();
                        u.setEmail(email);
                        u.setFirstName(email.split("@")[0]);
                        u.setApproved(true);
                        u.setActive(true);

                        // Unwrap Optional<Role>
                        Role superAdminRole = roleRepository.findByRoleName("SUPERADMIN")
                                .orElseThrow(() -> new RuntimeException("SUPERADMIN role not found"));
                        u.setRole(superAdminRole);

                        return u;
                    } else {
                        throw new RuntimeException("Invalid credentials: email not registered");
                    }
                });

        // Build full name
        String fullName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
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

            String htmlContent = "<!DOCTYPE html>"
                    + "<html>"
                    + "<head><meta charset='UTF-8'></head>"
                    + "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
                    + "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
                    + "<tr>"
                    + "<td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
                    + "<h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>"
                    + "</td>"
                    + "</tr>"
                    + "<tr>"
                    + "<td style='padding:30px;'>"
                    + "<h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
                    + "<p style='font-size:15px; color:#333;'>"
                    + "Hello " + safeFullname + ",<br><br>"
                    + "Thank you for choosing <b>Invoicing Application</b>. Use the following OTP to complete your Sign-In:"
                    + "</p>"
                    + "<div style='text-align:center; margin:25px 0;'>"
                    + "<span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9;'>"
                    + otp
                    + "</span>"
                    + "</div>"
                    + "<p style='font-size:14px; color:#555;'>"
                    + "This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone."
                    + "</p>"
                    + "<p style='font-size:14px; color:#333; margin-top:30px;'>"
                    + "Best Regards,<br><b>Invoicing Team</b>"
                    + "</p>"
                    + "</td>"
                    + "</tr>"
                    + "<tr>"
                    + "<td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
                    + "2025 Invoicing Team. All rights reserved."
                    + "</td>"
                    + "</tr>"
                    + "</table>"
                    + "</body>"
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

        // Fetch ManageUsers or fallback SUPERADMIN
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

        // Generate JWT
        String jwtToken = jwtServiceImpl.generateToken(user);

        // Determine privileges
        String roleName = manageUser.getRoleName();
        Set<Privilege> privileges;
        if ("SUPERADMIN".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
            privileges = new HashSet<>(privilegeRepository.findAll());
        } else {
            Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not assigned properly"));
            privileges = role.getPrivileges();
        }

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtToken);
        data.put("userId", manageUser.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("middleName", user.getMiddleName());
        data.put("lastName", user.getLastName());
        data.put("userRole", roleName);
        data.put("rolePrivileges", privileges.stream().map(Privilege::getName).toList());

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

        return Map.of(
                "status", "success",
                "message", "User logged in successfully",
                "data", data,
                "pagesize", 0,
                "timeStamp", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        );
    }

    /** ===================== Other Methods ===================== **/
    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User updateUserProfile(Long id, User updatedProfile) {
        return userRepository.findById(id).map(existingUser -> {
            if (updatedProfile.getFullName() != null) existingUser.setFullName(updatedProfile.getFullName());
            if (updatedProfile.getPrimaryEmail() != null) existingUser.setPrimaryEmail(updatedProfile.getPrimaryEmail());
            if (updatedProfile.getAlternativeEmail() != null) existingUser.setAlternativeEmail(updatedProfile.getAlternativeEmail());
            if (updatedProfile.getMobileNumber() != null) existingUser.setMobileNumber(updatedProfile.getMobileNumber());
            if (updatedProfile.getAlternativeMobileNumber() != null) existingUser.setAlternativeMobileNumber(updatedProfile.getAlternativeMobileNumber());
            if (updatedProfile.getCompanyName() != null) existingUser.setCompanyName(updatedProfile.getCompanyName());
            if (updatedProfile.getTaxId() != null) existingUser.setTaxId(updatedProfile.getTaxId());
            if (updatedProfile.getBusinessId() != null) existingUser.setBusinessId(updatedProfile.getBusinessId());
            if (updatedProfile.getPreferredCurrency() != null) existingUser.setPreferredCurrency(updatedProfile.getPreferredCurrency());
            if (updatedProfile.getInvoicePrefix() != null) existingUser.setInvoicePrefix(updatedProfile.getInvoicePrefix());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Map<String, Object> getPrivilegesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = user.getRole();
        if (role == null) throw new RuntimeException("User has no role assigned");

        Set<Privilege> privileges;
        if ("ADMIN".equalsIgnoreCase(role.getRoleName())) {
            privileges = new HashSet<>(privilegeRepository.findAll());
        } else {
            privileges = role.getPrivileges();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("role", role.getRoleName());
        result.put("privileges", privileges.stream()
                .map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "cardType", p.getCardType(),
                        "selected", true
                )).toList());
        return result;
    }

    private String getSafeFullName(User user) {
        String name = String.join(" ",
                Optional.ofNullable(user.getFirstName()).orElse(""),
                Optional.ofNullable(user.getMiddleName()).orElse(""),
                Optional.ofNullable(user.getLastName()).orElse("")
        ).trim();
        return name.isEmpty() ? user.getEmail().split("@")[0] : name;
    }
}
