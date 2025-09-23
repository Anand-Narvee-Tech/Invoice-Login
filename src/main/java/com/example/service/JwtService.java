package com.example.service;

import com.example.entity.User;

public interface JwtService {
    
	 public String generateToken(User user);
	 public boolean validateToken(String token);
	 public String extractUsername(String token) ;
}
