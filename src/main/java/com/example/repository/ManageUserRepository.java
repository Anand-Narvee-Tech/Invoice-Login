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
public interface ManageUserRepository extends JpaRepository<ManageUsers, Long>, JpaSpecificationExecutor<ManageUsers> {

    // === Existing methods ===
    boolean existsByEmail(String email);
    Optional<ManageUsers> findByEmailIgnoreCase(String email);
    List<ManageUsers> findByCreatedBy(User createdBy);
    boolean existsByEmailIgnoreCase(String email);
    List<ManageUsers> findByAddedBy(User addedBy);
    List<ManageUsers> findByAddedBy_Id(Long addedById);
    
    Optional<ManageUsers> findByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleId = :roleId")
    long countUsersWithRole(@Param("roleId") Long roleId);



    // === Company-based filters ===
//    List<ManageUsers> findByCompanyId(Long companyId);
//    Page<ManageUsers> findByCompanyId(Long companyId, Pageable pageable);

//    @Query("""
//        SELECT m FROM ManageUsers m 
//        WHERE m.companyId = :companyId
//        AND (
//            LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
//            LOWER(m.middleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
//            LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
//            LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
//            LOWER(m.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
//            LOWER(m.updatedByName) LIKE LOWER(CONCAT('%', :keyword, '%'))
//        )
//    """)
//    Page<ManageUsers> searchByCompany(@Param("keyword") String keyword,
//                                      @Param("companyId") Long companyId,
//                                      Pageable pageable);

    // === Global search (for SUPERADMIN) ===
    @Query("""
    	    SELECT m FROM ManageUsers m 
    	    WHERE LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
    	          LOWER(m.middleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
    	          LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
    	          LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
    	          LOWER(m.role.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
    	          LOWER(m.updatedByName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    	""")
    	Page<ManageUsers> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT u FROM ManageUsers u
            WHERE
                (:keyword IS NULL OR :keyword = '' OR
                 LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.middleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.primaryEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.addedByName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.updatedByName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
        Page<ManageUsers> searchUsers(
                @Param("keyword") String keyword,
                Pageable pageable
        );

}

