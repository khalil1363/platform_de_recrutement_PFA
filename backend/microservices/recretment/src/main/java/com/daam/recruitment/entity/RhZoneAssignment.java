package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rh_zone_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rh_user_id", "zone_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RhZoneAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rh_user_id", nullable = false) private String rhUserId;
    @Column(name = "zone_id", nullable = false) private String zoneId;
    @CreationTimestamp private LocalDateTime assignedAt;
}
