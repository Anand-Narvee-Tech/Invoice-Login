package com.example.serviceImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.example.DTO.LoginRequest;
import com.example.config.MailConfig;
import com.example.entity.OTP;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
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
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    UserServiceImpl(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    } 
    
    private static final Set<String> DEFAULT_SUPERUSERS = Set.of( "japhanya@narveetech.com","wasim@narveetech.com");
       
    /** Register new user */
    @Override
    public String register(User user) {
        if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered!");
        }

        boolean isFirstUser = userRepository.count() == 0;

        if (isFirstUser) {
            Role superAdminRole = roleRepository.findByRoleName("SUPERADMIN");
            if (superAdminRole == null) throw new RuntimeException("SUPERADMIN role not found in DB!");
            user.setRole(superAdminRole);
            user.setUserRole("SUPERADMIN");
            user.setApproved(true);
            user.setActive(true);
        } else {
            // Role will be assigned later by admin
            user.setUserRole(null);
            user.setRole(null);
            user.setApproved(false);
            user.setActive(false);
        }

        userRepository.save(user);
        return "User registered successfully!";
    }

    /** Send OTP to email */
    @Transactional
    @Override
    public void sendOtp(String email) {
        email = email.trim();

        // 1. Check if user exists
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid credentials: email not registered");
        }
        User user = optionalUser.get();
         String fullName;
         
         if(user.getFullName() != null && !user.getFullName().isBlank()) {
        	 fullName = user.getFullName();
         } else {
        	 StringBuilder sb = new StringBuilder();
        	 if(user.getFirstName() != null && !user.getFullName().isBlank()) {
        		 sb.append(user.getFirstName().trim());
        	 }
        	 if(user.getMiddleName() != null && !user.getMiddleName().isBlank()) {
        		 if(sb.length() > 0 ) sb.append(" ");
        		 sb.append(user.getMiddleName().trim());
        	 }
        	 if(user.getLastName() != null && !user.getLastName().isBlank()) {
        		 if(sb.length() > 0) sb.append( " '");
        		 sb.append(user.getLastName().trim());
        	 }
        	 fullName = sb.length() > 0 ? sb.toString() : email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
         }
         String safeFullname = HtmlUtils.htmlEscape(fullName);

        // 2. Remove old OTPs
        tokenRepository.deleteByEmail(email);

        // 3. Generate new OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit
        long expiryTime = System.currentTimeMillis() + 120000; // 2 minutes
              
        // 4. Save OTP in DB first
        OTP otpEntity = new OTP(null, email, otp, expiryTime);
        tokenRepository.save(otpEntity);

        // 5. Send email (don’t rollback DB if fails)
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Login Verification Code - Invoicing Team");

            // ===== HTML Email Design =====
            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head>"
                    + "<body style='margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f9f9f9;'>"
                    + "<table align='center' width='600' cellpadding='0' cellspacing='0' style='background:#ffffff; border-radius:8px; box-shadow:0 4px 8px rgba(0,0,0,0.1);'>"
                    + "  <tr>"
                    + "    <td align='center' bgcolor='#004b6e' style='padding:20px; border-top-left-radius:8px; border-top-right-radius:8px;'>"
                    + "      <h2 style='color:#ffffff; margin:0;'>Verify Your Login</h2>"
                    + "    </td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td style='padding:30px;'>"
                    + "      <h3 style='color:#004b6e; margin-top:0;'>Invoicing Team</h3>"
                    + "      <p style='font-size:15px; color:#333;'>"
                    + "        Hello " + safeFullname + ",<br> <br>"
                    + "        Thank you for choosing <b>Invoicing Team</b>. Use the following OTP to complete your Sign-In:"
                    + "      </p>"
                    + "      <div style='text-align:center; margin:25px 0;'>"
                    + "        <span style='display:inline-block; background:#f4f4f4; padding:20px 40px; border-radius:6px; font-size:28px; font-weight:bold; color:#6c2bd9;'>"
                    +              otp
                    + "        </span>"
                    + "      </div>"
                    + "      <p style='font-size:14px; color:#555;'>"
                    + "        This OTP is valid for <b>2 minutes</b>. Please do not share this code with anyone."
                    + "      </p>"
                    + "      <p style='font-size:14px; color:#333; margin-top:30px;'>"
                    + "        Best Regards,<br><b>Invoicing Team</b>"
                    + "      </p>"
                    + "    </td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td align='center' bgcolor='#f1f1f1' style='padding:10px; border-bottom-left-radius:8px; border-bottom-right-radius:8px; font-size:12px; color:#888;'>"
                    + "      © 2025 Invoicing Team. All rights reserved."
                    + "    </td>"
                    + "  </tr>"
                    + "</table>"
                    + "</body></html>";

            helper.setText(htmlContent, true); // true = HTML

            javaMailSender.send(mimeMessage);
            log.info("OTP sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            // Do not throw exception here; OTP is already saved in DB
        }
    }


//    /** Login with OTP and return JWT */
//    @Override
//    public Map<String, Object> loginWithOtp(LoginRequest request) {
//        String email = request.getEmail().trim();
//
//        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
//        if (optionalUser.isEmpty()) {
//            throw new RuntimeException("Invalid credentials: email not registered");
//        }
//        User user = optionalUser.get();
//
//        // 1. Fetch OTP from DB
//        Optional<OTP> optionalOtp = tokenRepository.findByEmailAndOtp(email, request.getOtp());
//        if (optionalOtp.isEmpty()) {
//            throw new RuntimeException("Invalid OTP or email. Please try again.");
//        }
//
//        OTP otpEntity = optionalOtp.get();
//
//        // 2. Check OTP expiry
//        if (System.currentTimeMillis() > otpEntity.getExpiryTime()) {
//            tokenRepository.deleteByEmail(email);
//            throw new RuntimeException("OTP has expired, please request a new one.");
//        }
//
//        // 3. OTP is valid → delete it
//        tokenRepository.deleteByEmail(email);
//
//        // 4. Generate JWT token
//        String jwtToken = jwtServiceImpl.generateToken(user);
//
//        // 5. Build response
//        String fullname = String.join(" ",
//                user.getFirstName() != null ? user.getFirstName() : "",
//                user.getMiddleName() != null ? user.getMiddleName() : "",
//                user.getLastName() != null ? user.getLastName() : ""
//        ).trim();
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("fullname", fullname);
//        response.put("userid", user.getId());
//        response.put("token", jwtToken);
//
//        return response;
//    }
    
    @Override
    @Transactional
    public Map<String, Object> loginWithOtp(LoginRequest request) {
        String email = request.getEmail().trim();
        String enteredOtp = request.getOtp();

        // 1. Check if user exists
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials: email not registered"));

        // 2. Check if user is DEFAULT_SUPERUSER
        boolean isDefaultSuperuser = DEFAULT_SUPERUSERS.contains(email.toLowerCase());

        // 3. For normal users, check approval and role
        if (!isDefaultSuperuser && (!Boolean.TRUE.equals(user.getApproved()) || user.getRole() == null)) {
            throw new RuntimeException("Account not approved yet or role not assigned");
        }

        // 4. Fetch OTP from DB
        OTP otpEntity = tokenRepository.findByEmailAndOtp(email, enteredOtp)
                .orElseThrow(() -> new RuntimeException("Invalid OTP or email. Please try again."));

        // 5. Check OTP expiry
        if (System.currentTimeMillis() > otpEntity.getExpiryTime()) {
            tokenRepository.deleteByEmail(email);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        // 6. OTP is valid → delete it
        tokenRepository.deleteByEmail(email);

        // 7. Generate JWT token
        String jwtToken = jwtServiceImpl.generateToken(user);

        // 8. Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("fullname", getSafeFullName(user));
        response.put("userid", user.getId());
        response.put("token", jwtToken);

        // 9. Set privileges
        Set<Privilege> privileges;
        if (isDefaultSuperuser || "SUPERADMIN".equalsIgnoreCase(user.getUserRole()) || "ADMIN".equalsIgnoreCase(user.getUserRole())) {
            privileges = new HashSet<>(privilegeRepository.findAll());
        } else if (user.getRole() != null) {
            privileges = user.getRole().getPrivileges();
        } else {
            privileges = new HashSet<>(); // no role assigned
        }

        response.put("role", user.getRole() != null ? user.getRole().getRoleName() : "DEFAULT_USER");
        response.put("privileges", privileges.stream()
            .map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "cardType", p.getCardType(),
                "selected", true
            ))
            .toList());

        return response;
    }


    /** Get user by ID */
    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /** Update user profile */
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
            if (updatedProfile.getPrefferedCurrency() != null) existingUser.setPrefferedCurrency(updatedProfile.getPrefferedCurrency());
            if (updatedProfile.getInvoicePrefix() != null) existingUser.setInvoicePrefix(updatedProfile.getInvoicePrefix());

            return userRepository.save(existingUser);

        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

	@Override
	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email);
	}
	/** ===================== ROLE & PRIVILEGES ===================== **/
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
                                               "selected", true // frontend can override
                                           ))
                                           .toList());
        return result;
    }


    /** ===================== HELPERS ===================== **/
    private String getSafeFullName(User user) {
        String name = String.join(" ",
                Optional.ofNullable(user.getFirstName()).orElse(""),
                Optional.ofNullable(user.getMiddleName()).orElse(""),
                Optional.ofNullable(user.getLastName()).orElse("")
        ).trim();
        return name.isEmpty() ? user.getEmail().split("@")[0] : name;
    }
}

