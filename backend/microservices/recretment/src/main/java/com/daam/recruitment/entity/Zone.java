package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "zones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Zone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String zoneId;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    @Builder.Default private boolean active = true;
    @CreationTimestamp private LocalDateTime createdAt;

    @PrePersist void prePersist() {
        if (zoneId == null) zoneId = UUID.randomUUID().toString();
    }
}
