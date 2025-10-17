package com.example.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.DTO.LoginRequest;
import com.example.DTO.RegisterRequest;
import com.example.commons.RestAPIResponse;
import com.example.entity.User;
import com.example.serviceImpl.JwtServiceImpl;
import com.example.serviceImpl.UserServiceImpl;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private JwtServiceImpl jwtService;

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
    
    @PostMapping("/register")
    public ResponseEntity<RestAPIResponse> register(@RequestBody RegisterRequest request) {
        try {
            //  Validate required email
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new RestAPIResponse("error", "Email is required", null));
            }

            // Map DTO → Entity
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setMiddleName(request.getMiddleName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPrimaryEmail(request.getEmail()); // always set
            user.setFullName(String.join(" ",
                    request.getFirstName() != null ? request.getFirstName() : "",
                    request.getMiddleName() != null ? request.getMiddleName() : "",
                    request.getLastName() != null ? request.getLastName() : ""
            ).trim());
            user.setMobileNumber(request.getMobileNumber());
            user.setCompanyName(request.getCompanyName());

            // Call service
            userServiceImpl.register(user);

            return ResponseEntity.ok(new RestAPIResponse("success", "Registered Successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new RestAPIResponse("error", e.getMessage(), null)
            );
        }
    }



    /** Send OTP */
    @PostMapping("/login/send-otp")
    public ResponseEntity<RestAPIResponse> sendOTP(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            userServiceImpl.sendOtp(email);
            return ResponseEntity.ok(
                    new RestAPIResponse("success", "OTP sent successfully", email)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new RestAPIResponse("error", e.getMessage(), null)
            );
        }
    }

    /** Login → verify OTP & return JWT */
    @PostMapping("/login")
    public ResponseEntity<RestAPIResponse> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> jwtToken = userServiceImpl.loginWithOtp(request);
            return ResponseEntity.ok(
                    new RestAPIResponse("success", "Login Successfully", jwtToken)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new RestAPIResponse("error", e.getMessage(), null)
            );
        }
    }

    /** Check token validity */
    @GetMapping("/check-token")
    public ResponseEntity<RestAPIResponse> checkToken(@RequestParam String token) {
        boolean isValid = jwtService.validateToken(token);
        String username = isValid ? jwtService.extractUsername(token) : null;

        return ResponseEntity.ok( new RestAPIResponse( isValid ? "success" : "error", isValid ? "Token is valid" : "Token is invalid", username));
    }
    
    @GetMapping("/updated/{id}")
    public ResponseEntity<RestAPIResponse> getUser(@PathVariable Long id) {
    	try {
    		return new ResponseEntity<>(new RestAPIResponse("Success" ,"Profile is  getting Sucessfully" , userServiceImpl.getUserById(id)) , HttpStatus.OK);
    	} catch (Exception e) {
    		return new ResponseEntity<>(new RestAPIResponse("Fail" , "No User found with this Id" + id), HttpStatus.OK);
    	}
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
    public ResponseEntity<RestAPIResponse> getMyProfile(@RequestHeader("Authorization") String token){
    	try {
    	                     String jwtToken = token.replace("Bearer", "");
    	                     String email = jwtService.extractUsername(jwtToken);
    	                     
    	                     User user = userServiceImpl.getUserByEmail(email)
    	                    		 .orElseThrow(() -> new RuntimeException("user not found"));
    	                     
    	                     return ResponseEntity.ok(new RestAPIResponse("Success", "Profile Fetched Successfully", user));
    	}catch (Exception e) {
			     return ResponseEntity.ok(new RestAPIResponse("Error", e.getMessage(),null));
		}
    }
    
    @GetMapping("/getall/privileges")
    public ResponseEntity<RestAPIResponse> getMyPrivileges(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "").trim();
            String email = jwtService.extractUsername(jwtToken);
            User user = userServiceImpl.getUserByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> privileges = userServiceImpl.getPrivilegesForUser(user.getId());
            return ResponseEntity.ok(new RestAPIResponse("success", "Privileges fetched successfully", privileges));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("error", e.getMessage(), null));
        }
    }
    
    @PutMapping("/me")
    public ResponseEntity<RestAPIResponse> updateMyProfile(@RequestHeader ("Authorization") String token,
    		                                                                                                        @RequestBody User updatedProfile){
    	try {
    		String jwtToken = token.replace("Bearer", "");
    		String email = jwtService.extractUsername(jwtToken);
    		
    		User existingUser = userServiceImpl.getUserByEmail(email)
    				                            .orElseThrow(() -> new RuntimeException("user not found"));
    		
    		User updated = userServiceImpl.updateUserProfile(existingUser.getId(), updatedProfile);
    		return ResponseEntity.ok( new RestAPIResponse("Success", "Profile Updated Successfully",updated));
    	} catch (Exception e) {
			return ResponseEntity.ok(new RestAPIResponse("Error",e.getMessage(),null));
		}
    }
}
