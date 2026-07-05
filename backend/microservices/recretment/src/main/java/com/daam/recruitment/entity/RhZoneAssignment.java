package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "rh_zone_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RhZoneAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String rhUserId;
    @Column(nullable = false) private String zoneId;
    @CreationTimestamp private LocalDateTime assignedAt;
}
