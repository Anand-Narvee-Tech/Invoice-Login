package com.example.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.example.DTO.LoginRequest;
import com.example.DTO.ManageUserDTO;
import com.example.DTO.SortingRequestDTO;
import com.example.DTO.UserProfileResponse;
import com.example.entity.ManageUsers;
import com.example.entity.Role;
import com.example.entity.User;

public interface UserService {
	
	public String register(User user);

	public Map<String, Object> loginWithOtp(LoginRequest request);

	public void sendOtp(String email);// request OTP
//    public boolean isOTPValid(String email, String otp);

	public Optional<User> getUserById(Long id);

	public User updateUserProfile(Long id, User updatedProfile);

	Optional<User> getUserByEmail(String email);

	Map<String, Object> getPrivilegesForUser(Long userId);

	UserProfileResponse getUserProfileByEmail(String email);
	
	public boolean verifyOtp(String emailInput, String otpInput);

	//public void sendOtp(String emailInput, String purpose);

	public void sendOtpForRegister(String emailInput);

	public  ManageUserDTO registerCompanyUser(ManageUsers manageUsers);

}
