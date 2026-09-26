package com.codereviewer.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single persisted finding belonging to a {@link Review}.
 */
@Entity
@Table(name = "review_findings")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "review")
public class ReviewFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false, length = 100)
    private String rule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false)
    private String file;

    @Column(nullable = false)
    private Integer line;

    @Column(nullable = false, length = 2000)
    private String message;

    public ReviewFinding(String rule, Severity severity, String file, Integer line, String message) {
        this.rule = rule;
        this.severity = severity;
        this.file = file;
        this.line = line;
        this.message = message;
    }

    public static ReviewFinding from(Finding finding) {
        return new ReviewFinding(finding.rule(), finding.severity(), finding.file(),
                finding.line(), finding.message());
    }
}
