package com.daam.recruitment.repository;

import com.daam.recruitment.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByZoneId(String zoneId);
    List<Zone> findByActiveTrue();
    boolean existsByName(String name);
}
