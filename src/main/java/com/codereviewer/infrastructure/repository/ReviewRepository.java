package com.codereviewer.infrastructure.repository;

import com.codereviewer.domain.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRepositoryOrderByCreatedAtDesc(String repository);

    Optional<Review> findByRepositoryAndPullRequestNumberOrderByCreatedAtDesc(
            String repository, Integer pullRequestNumber);

    @Query("select r from Review r where r.repository = :repository and r.pullRequestNumber = :pr")
    List<Review> findAllForPullRequest(@Param("repository") String repository,
                                       @Param("pr") Integer pullRequestNumber);
}
