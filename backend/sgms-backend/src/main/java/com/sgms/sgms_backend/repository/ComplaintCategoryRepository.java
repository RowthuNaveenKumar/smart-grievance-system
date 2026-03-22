package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory,Long> {
    Optional<ComplaintCategory> findByName(String name);
}
