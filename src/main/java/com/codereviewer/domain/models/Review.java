package com.codereviewer.domain.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A review run over one pull request.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "findings")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repository;

    @Column(nullable = false)
    private Integer pullRequestNumber;

    @Column(nullable = false)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    /** GitHub review id, once the findings have been submitted as a review. */
    private Long githubReviewId;

    /** Human-readable reason when status is PARTIAL or FAILED. */
    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<ReviewFinding> findings = new ArrayList<>();

    public Review(String repository, Integer pullRequestNumber, String commitSha) {
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
    }

    public void addFinding(ReviewFinding finding) {
        findings.add(finding);
        finding.setReview(this);
    }

    public void addAllFindings(List<ReviewFinding> newFindings) {
        newFindings.forEach(this::addFinding);
    }
}
