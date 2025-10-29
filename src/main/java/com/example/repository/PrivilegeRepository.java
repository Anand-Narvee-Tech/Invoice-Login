package com.example.repository;



import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Privilege;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Privilege findByName(String name);
    
    Set<Privilege> findByCategory(String category);

    List<Privilege> getPrivilegesByCategory(String category);
    
 // Fetch privileges by category (case-insensitive)
    List<Privilege> findByCategoryIgnoreCase(String category);

    // Optional: fetch by name (if needed)
    Optional<Privilege> findByNameIgnoreCase(String name);

}
