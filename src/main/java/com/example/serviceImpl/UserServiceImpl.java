package com.example.serviceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.DTO.LoginRequest;
import com.example.entity.OTP;
import com.example.entity.User;
import com.example.repository.TokenRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtServiceImpl jwtServiceImpl;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    /** Register new user */
//    @Override
//    public String register(User user) {
//        if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
//            throw new RuntimeException("Email is already registered!");
//        }
//        
//        if(user.getPrimaryEmail() == null || user.getPrimaryEmail().isBlank()) {
//        	user.setPrimaryEmail(user.getEmail());
//        }
//        
//        String fullName = String.join(" ",
//                user.getFirstName() != null ? user.getFirstName() : "",
//                user.getMiddleName() != null ? user.getMiddleName() : "",
//                user.getLastName() != null ? user.getLastName() : ""
//        ).trim();
//        	user.setFullName(fullName);
//        
//        userRepository.save(user);
//        return "User registered successfully!";
//    }
    public String register(User user) {
        if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered!");
        }

        userRepository.save(user);
        return "User registered successfully!";
    }
    
    /** Send OTP to email */
    @Transactional
    @Override
    public void sendOtp(String email) {
        email = email.trim();
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid credentials: email not registered");
        }

        // remove old OTP
        tokenRepository.deleteByEmail(email);

        // generate new OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        long expiryTime = System.currentTimeMillis() + 120000; // 2 min
        tokenRepository.save(new OTP(null, email, otp, expiryTime));

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom("no-reply@yourdomain.com");
            helper.setTo(email);
            helper.setSubject("Login Verification Code");
            helper.setText("Your OTP is: " + otp + " (valid for 2 mins)", false);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email");
        }
    }

    /** Login with OTP and return JWT */
    @Override
    public Map<String, Object> loginWithOtp(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid credentials: email not registered");
        }
        User user = optionalUser.get();

        // 1. Fetch OTP from DB (with email + otp)
        Optional<OTP> optionalOtp = tokenRepository.findByEmailAndOtp(request.getEmail(), request.getOtp());
        if (optionalOtp.isEmpty()) {
            throw new RuntimeException("Invalid OTP or email. Please try again.");
        }

        OTP otpEntity = optionalOtp.get();

        // 2. Check OTP expiry
        if (System.currentTimeMillis() > otpEntity.getExpiryTime()) {
            tokenRepository.deleteByEmail(request.getEmail()); // remove expired
            throw new RuntimeException("OTP has expired, please request a new one.");
        }

        // 3. OTP is valid → delete it
        tokenRepository.deleteByEmail(request.getEmail());

        // 4. Generate JWT token
        String jwtToken = jwtServiceImpl.generateToken(user);

        // build response
        String fullname = String.join(" ",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getMiddleName() != null ? user.getMiddleName() : "",
                user.getLastName() != null ? user.getLastName() : ""
        ).trim();

        Map<String, Object> response = new HashMap<>();
        response.put("fullname", fullname);
        response.put("userid", user.getId());
        response.put("token", jwtToken);

        return response;
    }

	@Override
	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
    public User updateUserProfile(Long id, User updatedProfile) {
        return userRepository.findById(id).map(existingUser -> {

            //  Only update non-null values (avoid overwriting with nulls)
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

}
