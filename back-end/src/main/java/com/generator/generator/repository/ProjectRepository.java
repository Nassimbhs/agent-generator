package com.generator.generator.repository;

import com.generator.generator.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);
    List<Project> findByUserIdAndIsActiveTrue(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT p FROM Project p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Project> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}

