package com.example.repository;



import com.example.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Privilege findByName(String name);

    List<Privilege> getPrivilegesByCategory(String category);

}
