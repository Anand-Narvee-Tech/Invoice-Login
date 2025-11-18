package com.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.entity.ManageUsers;
import com.example.entity.User;

@Repository
public interface ManageUserRepository extends JpaRepository<ManageUsers, Long> , JpaSpecificationExecutor<ManageUsers>{
	 boolean existsByEmail(String email);
	 Optional<ManageUsers> findByEmailIgnoreCase(String email);
	 List<ManageUsers> findByCreatedBy(User createdBy);
	 boolean  existsByEmailIgnoreCase(String email);
	 
	 Optional<ManageUsers> findByEmail(String email);

	    List<ManageUsers> findByAddedBy(User addedBy);
	    List<ManageUsers> findByAddedBy_Id(Long addedById);
	    
	    @Query("SELECT m FROM ManageUsers m WHERE " +
	            "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	            "LOWER(m.middleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	            "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	            "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	            "LOWER(m.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	            "LOWER(m.updatedByName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	     Page<ManageUsers> search(@Param("keyword") String keyword, Pageable pageable);
}
