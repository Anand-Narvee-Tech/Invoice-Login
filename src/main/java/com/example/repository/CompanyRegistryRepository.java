package com.example.repository;

import com.example.entity.CompanyRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRegistryRepository extends JpaRepository<CompanyRegistry, Long> {
    Optional<CompanyRegistry> findByCompanyDomain(String companyDomain);
    List<CompanyRegistry> findAllByActiveTrue();
    boolean existsByCompanyDomain(String companyDomain);
}
