package com.example.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.LoginRequest;
import com.example.DTO.ManageUserDTO;
import com.example.DTO.RegisterRequest;
import com.example.DTO.UserProfileResponse;
import com.example.commons.RestAPIResponse;
import com.example.entity.ManageUsers;
import com.example.entity.Privilege;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.entity.VerifyOtpRequest;
import com.example.repository.ManageUserRepository;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.example.serviceImpl.JwtServiceImpl;
import com.example.serviceImpl.UserServiceImpl;
import com.example.utils.JwtUtil;

import jakarta.persistence.Column;

//@CrossOrigin("*")
@RestController
@RequestMapping("/auth")
public class UserController {

	@Autowired
	private UserServiceImpl userServiceImpl;

	@Autowired
	private JwtServiceImpl jwtService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ManageUserRepository manageUserRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JwtUtil jwtUtil;

//Bhargav working

	@PostMapping("/register")
	public ResponseEntity<RestAPIResponse> register(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody  RegisterRequest request) {

		try {

			// Step 1: Build entity from request
			ManageUsers manageUsers = userServiceImpl.buildManageUsersFromRequest(request);

			// Step 2: Register user
			ManageUserDTO response = userServiceImpl.registerCompanyUser(manageUsers);

			// Step 3: Fetch saved user and manageUser
			User user = userRepository.findByEmailIgnoreCase(response.getEmail())
					.orElseThrow(() -> new RuntimeException("User not found"));

			ManageUsers savedUser = manageUserRepository.findByEmailIgnoreCase(response.getEmail())
					.orElseThrow(() -> new RuntimeException("ManageUser not found"));

			// Step 4: Get Role Name
			String roleName = savedUser.getRoleName();

			// Step 5: Fetch Privileges from Role
			Set<String> privilegeNames = new HashSet<>();

			if (roleName != null) {
				Role roleEntity = roleRepository.findByRoleNameIgnoreCase(roleName).orElse(null);

				if (roleEntity != null && roleEntity.getPrivileges() != null) {
					privilegeNames = roleEntity.getPrivileges().stream().map(Privilege::getName)
							.collect(Collectors.toSet());
				}
			}

			// Step 6: Generate Token with Role + Privileges
			String token = jwtService.generateToken(user, roleName, privilegeNames);

			// Step 7: Build Final Response (DO NOT REMOVE ANY EXISTING FIELD)

			Map<String, Object> finalResponse = new LinkedHashMap<>();

			finalResponse.put("id", savedUser.getId());
			finalResponse.put("fullName", savedUser.getFullName());
			finalResponse.put("firstName", savedUser.getFirstName());
			finalResponse.put("middleName", savedUser.getMiddleName());
			finalResponse.put("lastName", savedUser.getLastName());
			finalResponse.put("email", savedUser.getEmail());
			finalResponse.put("primaryEmail", savedUser.getPrimaryEmail());
			finalResponse.put("mobileNumber", savedUser.getMobileNumber());
			finalResponse.put("companyName", savedUser.getCompanyName());
			finalResponse.put("state", savedUser.getState());
			finalResponse.put("city", savedUser.getCity());
			finalResponse.put("country", savedUser.getCountry());
			finalResponse.put("pincode", savedUser.getPincode());
			finalResponse.put("telephone", savedUser.getTelephone());
			finalResponse.put("ein", savedUser.getEin());
			finalResponse.put("gstin", savedUser.getGstin());
			finalResponse.put("website", savedUser.getWebsite());
			finalResponse.put("address", savedUser.getAddress());
			finalResponse.put("loginurl", savedUser.getLoginUrl());

			// ---------- ADDED PART (AS YOU REQUESTED) ----------
			finalResponse.put("roleName", roleName);
			finalResponse.put("privileges", privilegeNames);
			finalResponse.put("token", token);
			// ---------------------------------------------------

			return ResponseEntity.status(HttpStatus.CREATED).body(
					new RestAPIResponse("success", "Company registered successfully. ADMIN created.", finalResponse));

		} catch (DataIntegrityViolationException e) {

			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RestAPIResponse("failed", "Email or mobile number already exists.", null));

		} catch (Exception e) {
			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestAPIResponse("failed", "Registration failed: " + e.getMessage(), null));
		}

	}

	/** Send OTP */
	@PostMapping("/login/send-otp")
	public ResponseEntity<RestAPIResponse> sendOTP(@RequestBody Map<String, String> body) {
		try {
			String email = body.get("email");
			userServiceImpl.sendOtp(email);
			return ResponseEntity.ok(new RestAPIResponse("success", "OTP sent successfully", email));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

	@PostMapping("/register/send-otp")
	public ResponseEntity<RestAPIResponse> sendRegisterOtp(@RequestBody Map<String, String> body) {
		try {
			String email = body.get("email");
			userServiceImpl.sendOtpForRegister(email);
			return ResponseEntity.ok(new RestAPIResponse("success", "OTP sent successfully for registration", email));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

	// Bhargav

//	//@PostMapping("/otp/send")
//	@PostMapping("/login/send-otp")
//	public ResponseEntity<RestAPIResponse> sendOTP(@RequestBody Map<String, String> body) {
//	    try {
//	        String email = body.get("email");
//	        String purpose = body.get("purpose");   // LOGIN or REGISTER
//
//	        if (email == null || purpose == null) {
//	            throw new RuntimeException("Email and purpose are required");
//	        }
//
//	        userServiceImpl.sendOtp(email, purpose.toUpperCase());
//
//	        return ResponseEntity.ok(
//	            new RestAPIResponse("success", "OTP sent successfully", email)
//	        );
//
//	    } catch (Exception e) {
//	        return ResponseEntity.badRequest()
//	            .body(new RestAPIResponse("error", e.getMessage(), null));
//	    }
//	}

	// Bhargav

	// Bhargav
	@GetMapping("/check-email/{email}")
	public ResponseEntity<RestAPIResponse> checkDuplicateEmail(@PathVariable String email) {
		// logger.info("!!! inside class: CustomersController,!! method:
		// checkDuplicateEmail() ");
		boolean isDuplicate = userServiceImpl.isEmailDuplicate(email);

		if (isDuplicate) {
			return new ResponseEntity<RestAPIResponse>(new RestAPIResponse("fail", "Email already exists", isDuplicate),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<RestAPIResponse>(
					new RestAPIResponse("success", "Email is available", isDuplicate), HttpStatus.OK);
		}
	}
	// Bhargav

	/** Login → OTP & return JWT */
	@PostMapping("/login")
	public ResponseEntity<RestAPIResponse> login(@RequestBody LoginRequest request) {
		try {
			Map<String, Object> jwtToken = userServiceImpl.loginWithOtp(request);
			return ResponseEntity.ok(new RestAPIResponse("success", "Login Successfully", jwtToken));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

//Bhargav
	/** Verify OTP */
	@PostMapping("/login/verify-otp")
	public ResponseEntity<RestAPIResponse> verifyOTP(@RequestBody VerifyOtpRequest request) {
		try {
			boolean isValid = userServiceImpl.verifyOtp(request.getEmail(), request.getOtp());

			if (isValid) {
				return ResponseEntity.ok(new RestAPIResponse("success", "OTP verified successfully", null));
			} else {
				return ResponseEntity.badRequest().body(new RestAPIResponse("error", "Invalid or expired OTP", null));
			}

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

	// Bhargav

	/** Check token validity */
	@GetMapping("/check-token")
	public ResponseEntity<RestAPIResponse> checkToken(@RequestParam String token) {
		boolean isValid = jwtService.validateToken(token);
		String username = isValid ? jwtService.extractUsername(token) : null;

		return ResponseEntity.ok(new RestAPIResponse(isValid ? "success" : "error",
				isValid ? "Token is valid" : "Token is invalid", username));
	}

//	@GetMapping("/updated/{email}")
//	public ResponseEntity<RestAPIResponse> getUserProfile(@PathVariable String email) {
//
//		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
//		Optional<ManageUsers> muOpt = manageUserRepository.findByEmailIgnoreCase(email);
//
//		if (userOpt.isEmpty() && muOpt.isEmpty()) {
//			return ResponseEntity.ok(new RestAPIResponse("Fail", "No user found with this email: " + email, null));
//		}
//
//		User user = userOpt.orElse(null);
//		ManageUsers mu = muOpt.orElse(null);
//
//		// ---------------- Determine fullName ----------------
//		String fullName = null;
//		if (mu != null && mu.getFullName() != null && !mu.getFullName().isBlank()) {
//			fullName = mu.getFullName();
//		} else if (user != null && user.getFullName() != null) {
//			fullName = user.getFullName();
//		}
//
//		// ---------------- Build response ----------------
//		Map<String, Object> responseData = new HashMap<>();
//
//		if (user != null) {
//			responseData.put("id", user.getId());
//			responseData.put("mobileNumber", user.getMobileNumber());
//			responseData.put("alternativeEmail", user.getAlternativeEmail());
//			responseData.put("alternativeMobileNumber", user.getAlternativeMobileNumber());
//			responseData.put("companyName", user.getCompanyName());
//			responseData.put("taxId", user.getTaxId());
//			responseData.put("businessId", user.getBusinessId());
//			responseData.put("preferredCurrency", user.getPreferredCurrency());
//			responseData.put("invoicePrefix", user.getInvoicePrefix());
//			responseData.put("profilePicPath", user.getProfilePicPath());
//		}
//
//		if (mu != null) {
//			responseData.put("primaryEmail", mu.getEmail());
//
//			// ✅ FIXED ROLE ACCESS
//			responseData.put("role", mu.getRole() != null ? mu.getRole().getRoleName() : null);
//		}
//
//		responseData.put("fullName", fullName);
//
//		return ResponseEntity.ok(new RestAPIResponse("Success", "Profile retrieved successfully", responseData));
//	}

	@GetMapping("/updated/email/{email}")
	public ResponseEntity<RestAPIResponse> getUserProfileByEmail(@PathVariable("email") String email) {

		UserProfileResponse response = userServiceImpl.getUserProfileByEmail(email);

		if (response == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestAPIResponse("Fail", "No user found with this email: " + email, Map.of()));
		}

		return ResponseEntity.ok(new RestAPIResponse("Success", "Profile retrieved successfully", response));
	}

//    @PutMapping("/updated/{id}")
//    public ResponseEntity<RestAPIResponse> updateProfile(@PathVariable Long id, @RequestBody User user){
//    	try {
//    		return new ResponseEntity<>(new RestAPIResponse("Success" ,"Profile is  Updated Sucessfully" , userServiceImpl.updateUserProfile(id, user)) , HttpStatus.OK);
//    	}catch (Exception e) {
//    		return new ResponseEntity<>(new RestAPIResponse("Success" ,"Profile is  Not Updated please check Id and User" +id +" " +user) , HttpStatus.OK);
//		}	
//    }
	@GetMapping("/me")
	public ResponseEntity<RestAPIResponse> getMyProfile(@RequestHeader("Authorization") String token) {
		try {
			String jwtToken = token.replace("Bearer", "");
			String email = jwtService.extractUsername(jwtToken);

			User user = userServiceImpl.getUserByEmail(email).orElseThrow(() -> new RuntimeException("user not found"));

			return ResponseEntity.ok(new RestAPIResponse("Success", "Profile Fetched Successfully", user));
		} catch (Exception e) {
			return ResponseEntity.ok(new RestAPIResponse("Error", e.getMessage(), null));
		}
	}

	@GetMapping("/getall/privileges")
	public ResponseEntity<RestAPIResponse> getMyPrivileges(@RequestHeader("Authorization") String token) {
		try {
			String jwtToken = token.replace("Bearer ", "").trim();
			String email = jwtService.extractUsername(jwtToken);
			User user = userServiceImpl.getUserByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

			Map<String, Object> privileges = userServiceImpl.getPrivilegesForUser(user.getId());
			return ResponseEntity.ok(new RestAPIResponse("success", "Privileges fetched successfully", privileges));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

	@PutMapping("/me")
	public ResponseEntity<RestAPIResponse> updateMyProfile(@RequestHeader("Authorization") String token,
			@RequestBody User updatedProfile) {
		try {
			String jwtToken = token.replace("Bearer", "");
			String email = jwtService.extractUsername(jwtToken);

			User existingUser = userServiceImpl.getUserByEmail(email)
					.orElseThrow(() -> new RuntimeException("user not found"));

			User updated = userServiceImpl.updateUserProfile(existingUser.getId(), updatedProfile);
			return ResponseEntity.ok(new RestAPIResponse("Success", "Profile Updated Successfully", updated));
		} catch (Exception e) {
			return ResponseEntity.ok(new RestAPIResponse("Error", e.getMessage(), null));
		}
	}

	/**
	 * Generate registration token
	 */
	@GetMapping("/get-registration-token")
	public ResponseEntity<RestAPIResponse> getRegistrationToken() {

		String token = jwtUtil.generateToken("REGISTRATION_SERVICE", "REGISTRATION");

		Map<String, String> tokenData = new HashMap<>();
		tokenData.put("token", token);
		tokenData.put("type", "Bearer");
		tokenData.put("expiresIn", "24 hours");

		return ResponseEntity.ok(new RestAPIResponse("success", "Registration token generated", tokenData));
	}
}
