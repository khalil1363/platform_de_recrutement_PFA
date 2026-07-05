package com.daam.recruitment.repository;

import com.daam.recruitment.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCompanyId(String companyId);
    List<Company> findByZoneIdAndActiveTrue(String zoneId);
    List<Company> findByActiveTrue();
}
