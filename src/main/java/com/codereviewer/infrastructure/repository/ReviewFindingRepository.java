package com.codereviewer.infrastructure.repository;

import com.codereviewer.domain.models.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {
}
