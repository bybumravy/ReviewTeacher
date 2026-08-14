package com.unireview.repository;

import com.unireview.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @Query("SELECT t FROM Teacher t WHERE " +
           "(:search IS NULL OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.faculty) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.department) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:faculty IS NULL OR t.faculty = :faculty) AND " +
           "(:minRating IS NULL OR t.avgRating >= :minRating)")
    Page<Teacher> findByFilters(
            @Param("search") String search,
            @Param("faculty") String faculty,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable
    );
}
