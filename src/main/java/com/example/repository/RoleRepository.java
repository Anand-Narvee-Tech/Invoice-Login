package com.example.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{
	
	Optional<Role> findByRoleName(String roleName);
    Optional<Role> findByRoleNameIgnoreCase(String roleName);
    
    @Query("""
            SELECT r FROM Role r
            WHERE
                CAST(r.roleId AS string) LIKE %:keyword% OR
                LOWER(r.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(r.status) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                CAST(r.adminId AS string) LIKE %:keyword% OR
                CAST(r.addedBy AS string) LIKE %:keyword% OR
                CAST(r.updatedBy AS string) LIKE %:keyword% OR
                LOWER(r.addedByName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(r.updatedByName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                CAST(r.createdDate AS string) LIKE %:keyword% OR
                CAST(r.updatedDate AS string) LIKE %:keyword%
            """)
        Page<Role> searchAll(@Param("keyword") String keyword, Pageable pageable);

}

