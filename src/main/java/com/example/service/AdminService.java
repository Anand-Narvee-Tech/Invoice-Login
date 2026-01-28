package com.example.service;

import java.util.List;

import com.example.entity.Admin;
import com.example.entity.User;

public interface AdminService  {
            
	public Admin saveProfile(Admin admin);
	public List<Admin> getAll();
	public User getById(Long id);
	public Admin updateProfile(Long id, Admin updatedAdmin);
	public boolean deleteProfile(Long id);
}
