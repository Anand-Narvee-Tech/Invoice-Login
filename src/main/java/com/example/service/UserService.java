package com.example.service;

import java.util.Map;
import java.util.Optional;

import com.example.DTO.LoginRequest;
import com.example.entity.User;

public interface UserService {
 
	public String register(User user) ;
	public Map<String, Object> loginWithOtp(LoginRequest request);
    public void sendOtp(String email);//request OTP
//    public boolean isOTPValid(String email, String otp);
    
    public Optional<User> getUserById(Long id);
    public User updateUserProfile(Long id, User updatedProfile);
    
    Optional<User> getUserByEmail(String email);
    
}
