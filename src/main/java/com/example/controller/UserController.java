package com.example.controller;

import java.util.HashMap;
import java.util.Map;

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
import com.example.entity.User;
import com.example.entity.VerifyOtpRequest;
import com.example.repository.ManageUserRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.example.serviceImpl.JwtServiceImpl;
import com.example.serviceImpl.UserServiceImpl;
import com.example.utils.JwtUtil;

import jakarta.validation.Valid;

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
	    private JwtUtil jwtUtil;


	
//Bhargav working
	
//	@PostMapping("/register")
//	public ResponseEntity<RestAPIResponse> register(@RequestBody RegisterRequest request) {
//
//		ManageUsers manageUsers = new ManageUsers();
//		manageUsers.setFirstName(request.getFirstName());
//		manageUsers.setMiddleName(request.getMiddleName());
//		manageUsers.setLastName(request.getLastName());
//		manageUsers.setEmail(request.getEmail());
//		manageUsers.setPrimaryEmail(request.getEmail());
//		manageUsers.setMobileNumber(request.getMobileNumber());
//		manageUsers.setCompanyName(request.getCompanyName());
//		
//		manageUsers.setState(request.getState());
//		manageUsers.setCountry(request.getCountry());
//		manageUsers.setPincode(request.getPincode());
//		manageUsers.setTelephone(request.getTelephone());
//		manageUsers.setEin(request.getEin());
//		manageUsers.setGstin(request.getGstin());
//		manageUsers.setWebsite(request.getWebsite());
//		manageUsers.setAddress(request.getAddress());
//		
//		ManageUserDTO response = userService.registerCompanyUser(manageUsers);
//
//		return ResponseEntity
//				.ok(new RestAPIResponse("success", "Company registered successfully. ADMIN created.", response));
//	}
	 @PostMapping("/register")
	 public ResponseEntity<RestAPIResponse> register(
	         @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
	         @RequestBody @Valid RegisterRequest request) {

	     try {
	         // ✅ Step 1: Validate Authorization header
	         String validationError = validateAuthorizationHeader(authorizationHeader);
	         if (validationError != null) {
	             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                     .body(new RestAPIResponse("failed", validationError, null));
	         }

	         // ✅ Step 2: Extract and validate token
	         String token = authorizationHeader.substring(7).trim();
	         
	         if (token.isEmpty()) {
	             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                     .body(new RestAPIResponse("failed", "Bearer token is empty.", null));
	         }

	         // ✅ Step 3: Validate token
	         if (!jwtUtil.validateToken(token)) {
	             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                     .body(new RestAPIResponse("failed", "Invalid or malformed Bearer token.", null));
	         }

	         // ✅ Step 4: Check token expiration
	         if (jwtUtil.isTokenExpired(token)) {
	             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                     .body(new RestAPIResponse("failed", "Bearer token has expired.", null));
	         }

	         // ✅ Step 5: Optional role-based authorization
	         // Uncomment if you need role verification
	         /*
	         String tokenRole = jwtUtil.getRoleFromToken(token);
	         if (!isAuthorizedRole(tokenRole)) {
	             return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                     .body(new RestAPIResponse("failed", "Insufficient permissions for company registration.", null));
	         }
	         */

	         // ✅ Step 6: Build ManageUsers entity
	         ManageUsers manageUsers = buildManageUsersFromRequest(request);

	         // ✅ Step 7: Register company user
	         ManageUserDTO response = userServiceImpl.registerCompanyUser(manageUsers);

	         // ✅ Step 8: Generate JWT token for the newly registered user
	         String newUserToken = jwtUtil.generateToken(response.getEmail(), response.getRoleName());
	         response.setToken(newUserToken);

	         // ✅ Step 9: Return success response with token
	         return ResponseEntity.status(HttpStatus.CREATED)
	                 .body(new RestAPIResponse("success", "Company registered successfully. ADMIN created.", response));

	     } catch (io.jsonwebtoken.ExpiredJwtException e) {
	         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                 .body(new RestAPIResponse("failed", "Token has expired.", null));
	                 
	     } catch (io.jsonwebtoken.MalformedJwtException e) {
	         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                 .body(new RestAPIResponse("failed", "Malformed JWT token.", null));
	                 
	     } catch (io.jsonwebtoken.security.SignatureException e) {
	         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                 .body(new RestAPIResponse("failed", "Invalid JWT signature.", null));
	                 
	     } catch (IllegalArgumentException e) {
	         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                 .body(new RestAPIResponse("failed", "Invalid request data: " + e.getMessage(), null));
	                 
	     } catch (DataIntegrityViolationException e) {
	         return ResponseEntity.status(HttpStatus.CONFLICT)
	                 .body(new RestAPIResponse("failed", "Email or mobile number already exists.", null));
	                 
	     } catch (Exception e) {
	         // Log the exception for debugging
	         e.printStackTrace();
	         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                 .body(new RestAPIResponse("failed", "Registration failed: " + e.getMessage(), null));
	     }
	 }

	 // ✅ Helper method: Validate Authorization header
	 private String validateAuthorizationHeader(String authorizationHeader) {
	     if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
	         return "Authorization header is missing. Bearer token is required.";
	     }
	     
	     if (!authorizationHeader.startsWith("Bearer ")) {
	         return "Invalid Authorization format. Use 'Bearer <token>'";
	     }
	     
	     return null; // No error
	 }

	 // ✅ Helper method: Check authorized roles
	 private boolean isAuthorizedRole(String role) {
	     return "REGISTRATION".equals(role) || 
	            "ADMIN".equals(role) || 
	            "SUPERADMIN".equals(role);
	 }

	 // ✅ Helper method: Build ManageUsers from request
	 private ManageUsers buildManageUsersFromRequest(RegisterRequest request) {
	     ManageUsers manageUsers = new ManageUsers();
	     manageUsers.setFirstName(request.getFirstName());
	     manageUsers.setMiddleName(request.getMiddleName());
	     manageUsers.setLastName(request.getLastName());
	     manageUsers.setEmail(request.getEmail());
	     manageUsers.setPrimaryEmail(request.getEmail());
	     manageUsers.setMobileNumber(request.getMobileNumber());
	     manageUsers.setCompanyName(request.getCompanyName());
	     manageUsers.setState(request.getState());
	     manageUsers.setCountry(request.getCountry());
	     manageUsers.setPincode(request.getPincode());
	     manageUsers.setTelephone(request.getTelephone());
	     manageUsers.setEin(request.getEin());
	     manageUsers.setGstin(request.getGstin());
	     manageUsers.setWebsite(request.getWebsite());
	     manageUsers.setAddress(request.getAddress());
	     return manageUsers;
	 }

	 //Bhargav working
	
//Bhargav working 
	
	

	
	

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
	        return ResponseEntity.ok(
	            new RestAPIResponse("success", "OTP sent successfully for registration", email)
	        );
	    } catch (Exception e) {
	        return ResponseEntity.badRequest()
	                .body(new RestAPIResponse("error", e.getMessage(), null));
	    }
	}

	
	//Bhargav
	
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

	//Bhargav
	
	
	
	//Bhargav
	@GetMapping("/check-email/{email}")
	public ResponseEntity<RestAPIResponse> checkDuplicateEmail(@PathVariable String email) {
		//logger.info("!!! inside class: CustomersController,!! method: checkDuplicateEmail() ");
		boolean isDuplicate = userServiceImpl.isEmailDuplicate(email);

		if (isDuplicate) {
			return new ResponseEntity<RestAPIResponse>(
					new RestAPIResponse("fail", "Email already exists", isDuplicate), HttpStatus.OK);
		} else {
			return new ResponseEntity<RestAPIResponse>(
					new RestAPIResponse("success", "Email is available", isDuplicate), HttpStatus.OK);
		}
	}
	//Bhargav
	

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
	            return ResponseEntity.badRequest()
	                    .body(new RestAPIResponse("error", "Invalid or expired OTP", null));
	        }

	    } catch (Exception e) {
	        return ResponseEntity.badRequest()
	                .body(new RestAPIResponse("error", e.getMessage(), null));
	    }
	}
	
	//Bhargav
	
	
	
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
	    
	    return ResponseEntity.ok(
	            new RestAPIResponse("success", "Registration token generated", tokenData));
	}
}
