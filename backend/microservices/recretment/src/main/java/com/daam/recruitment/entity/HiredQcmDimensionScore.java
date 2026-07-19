package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hired_qcm_dimension_scores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HiredQcmDimensionScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assignmentId;

    @Column(nullable = false)
    private String dimensionCode;

    @Column(nullable = false)
    private String dimensionLabel;

    /** Score 0..1 */
    @Column(nullable = false)
    private Double score;

    /** Expected score for the target role 0..1 */
    @Column(nullable = false)
    private Double expectedScore;

    @Column(columnDefinition = "TEXT")
    private String commentText;

    private Integer sortOrder;
}
