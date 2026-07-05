package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String companyId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String zoneId;
    private String address;
    @Builder.Default private boolean active = true;
    @CreationTimestamp private LocalDateTime createdAt;

    @PrePersist void prePersist() {
        if (companyId == null) companyId = UUID.randomUUID().toString();
    }
}
