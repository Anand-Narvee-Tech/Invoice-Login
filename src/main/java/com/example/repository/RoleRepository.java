package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{
	
	Optional<Role> findByRoleName(String roleName);
    Optional<Role> findByRoleNameIgnoreCase(String roleName);
}
