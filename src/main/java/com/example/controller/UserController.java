package com.example.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.example.repository.ManageUserRepository;
import com.example.repository.UserRepository;
import com.example.serviceImpl.JwtServiceImpl;
import com.example.serviceImpl.UserServiceImpl;

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

	/** Register */
//    @PostMapping("/register")
//    public ResponseEntity<RestAPIResponse> register(@RequestBody User user) {
//        try {
//            String result = userServiceImpl.register(user);
//            return new ResponseEntity<>(
//                    new RestAPIResponse("success", "Registered Successfully", result),
//                    HttpStatus.OK
//            );
//        } catch (Exception e) {
//            return new ResponseEntity<>(
//                    new RestAPIResponse("error", e.getMessage(), null),
//                    HttpStatus.BAD_REQUEST
//            );
//        }
//    }

//    @PostMapping("/register")
//    public ResponseEntity<RestAPIResponse> register(@RequestBody RegisterRequest request) {
//
//        ManageUsers manageUsers = new ManageUsers();
//        manageUsers.setFirstName(request.getFirstName());
//        manageUsers.setMiddleName(request.getMiddleName());
//        manageUsers.setLastName(request.getLastName());
//        manageUsers.setEmail(request.getEmail());
//        manageUsers.setPrimaryEmail(null);
//
//        // fullName handled automatically by @PrePersist
//        ManageUserDTO response = userServiceImpl.registerCompanyUser(manageUsers);
//
//        return ResponseEntity.ok(
//                new RestAPIResponse(
//                        "success",
//                        "Company registered successfully. SUPERADMIN created.",
//                        response
//                )
//        );
//    }

	@PostMapping("/register")
	public ResponseEntity<RestAPIResponse> register(@RequestBody RegisterRequest request) {

		ManageUsers manageUsers = new ManageUsers();
		manageUsers.setFirstName(request.getFirstName());
		manageUsers.setMiddleName(request.getMiddleName());
		manageUsers.setLastName(request.getLastName());
		manageUsers.setEmail(request.getEmail());
		manageUsers.setPrimaryEmail(request.getEmail());
		manageUsers.setMobileNumber(request.getMobileNumber());
		manageUsers.setCompanyName(request.getCompanyName());

		ManageUserDTO response = userServiceImpl.registerCompanyUser(manageUsers);

		return ResponseEntity
				.ok(new RestAPIResponse("success", "Company registered successfully. ADMIN created.", response));
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

	/** Login → verify OTP & return JWT */
	@PostMapping("/login")
	public ResponseEntity<RestAPIResponse> login(@RequestBody LoginRequest request) {
		try {
			Map<String, Object> jwtToken = userServiceImpl.loginWithOtp(request);
			return ResponseEntity.ok(new RestAPIResponse("success", "Login Successfully", jwtToken));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new RestAPIResponse("error", e.getMessage(), null));
		}
	}

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
}
