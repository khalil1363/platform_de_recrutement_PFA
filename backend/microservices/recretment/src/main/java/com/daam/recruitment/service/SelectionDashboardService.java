package com.daam.recruitment.service;

import com.daam.recruitment.dto.RecruitmentDtos.*;
import com.daam.recruitment.entity.Company;
import com.daam.recruitment.entity.JobApplication;
import com.daam.recruitment.entity.Recruitment;
import com.daam.recruitment.entity.Zone;
import com.daam.recruitment.enumeration.ApplicationStatus;
import com.daam.recruitment.repository.CompanyRepository;
import com.daam.recruitment.repository.JobApplicationRepository;
import com.daam.recruitment.repository.RecruitmentRepository;
import com.daam.recruitment.repository.RhZoneAssignmentRepository;
import com.daam.recruitment.repository.ZoneRepository;
import com.daam.recruitment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionDashboardService {

    private final RecruitmentRepository recruitmentRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRepository companyRepository;
    private final ZoneRepository zoneRepository;
    private final RhZoneAssignmentRepository rhZoneAssignmentRepository;

    @Transactional(readOnly = true)
    public SelectionDashboardResponse getDashboard(AuthUser authUser, Integer year, Integer month) {
        List<String> zoneIds = authUser.isAdmin()
                ? zoneRepository.findAll().stream().map(Zone::getZoneId).toList()
                : rhZoneAssignmentRepository.findByRhUserId(authUser.getUserId()).stream()
                        .map(a -> a.getZoneId())
                        .distinct()
                        .toList();

        if (zoneIds.isEmpty()) {
            return emptyDashboard(year, month);
        }

        LocalDate filterMonth = resolveFilterMonth(year, month);
        List<Recruitment> coworkings = recruitmentRepository.findByCoworkingTrueAndZoneIdIn(zoneIds).stream()
                .filter(r -> filterMonth == null
                        || (r.getCoworkingMonth() != null
                        && r.getCoworkingMonth().getYear() == filterMonth.getYear()
                        && r.getCoworkingMonth().getMonthValue() == filterMonth.getMonthValue()))
                .toList();

        Map<String, Recruitment> recruitmentById = coworkings.stream()
                .collect(Collectors.toMap(Recruitment::getRecruitmentId, r -> r, (a, b) -> a));
        Map<String, String> companyNames = resolveCompanyNames(coworkings);

        List<String> recruitmentIds = coworkings.stream().map(Recruitment::getRecruitmentId).toList();
        List<JobApplication> applications = recruitmentIds.isEmpty()
                ? List.of()
                : jobApplicationRepository.findByRecruitmentIdIn(recruitmentIds);

        List<String> responsibleNames = coworkings.stream()
                .map(Recruitment::getResponsibleName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();

        long total = applications.size();
        long retenus = count(applications, this::isRetenu);
        long desistes = count(applications, a -> a.getStatus() == ApplicationStatus.DESISTED);
        long nonPresentes = count(applications, this::isNonPresente);
        long nonRetenus = count(applications, a ->
                a.getStatus() == ApplicationStatus.REJECTED && !isNonPresente(a));
        double selectionRate = total == 0 ? 0.0 : round1(retenus * 100.0 / total);
        long integresMonth = countIntegrated(applications, filterMonth);
        long heberges = count(applications, a -> StringUtils.hasText(a.getHebergement()));

        return SelectionDashboardResponse.builder()
                .totalCampaigns(coworkings.size())
                .filterYear(year != null ? year.longValue() : null)
                .filterMonth(month)
                .filterMonthLabel(monthLabel(filterMonth))
                .responsibleNames(responsibleNames)
                .totalCandidates(total)
                .retenus(retenus)
                .nonRetenus(nonRetenus)
                .nonPresentes(nonPresentes)
                .desistes(desistes)
                .selectionRate(selectionRate)
                .integresMonth(integresMonth)
                .heberges(heberges)
                .statusStats(buildStatusStats(applications, total))
                .bySource(buildSourceStats(applications))
                .byResponsible(buildResponsibleStats(applications, recruitmentById))
                .topAgencies(buildAgencyStats(applications, recruitmentById, companyNames))
                .topPosts(buildPostStats(applications, recruitmentById))
                .build();
    }

    private SelectionDashboardResponse emptyDashboard(Integer year, Integer month) {
        LocalDate filterMonth = resolveFilterMonth(year, month);
        return SelectionDashboardResponse.builder()
                .totalCampaigns(0)
                .filterYear(year != null ? year.longValue() : null)
                .filterMonth(month)
                .filterMonthLabel(monthLabel(filterMonth))
                .responsibleNames(List.of())
                .totalCandidates(0)
                .retenus(0)
                .nonRetenus(0)
                .nonPresentes(0)
                .desistes(0)
                .selectionRate(0)
                .integresMonth(0)
                .heberges(0)
                .statusStats(List.of())
                .bySource(List.of())
                .byResponsible(List.of())
                .topAgencies(List.of())
                .topPosts(List.of())
                .build();
    }

    private LocalDate resolveFilterMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        return LocalDate.of(year, month, 1);
    }

    private String monthLabel(LocalDate filterMonth) {
        if (filterMonth == null) {
            return LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase(Locale.FRENCH);
        }
        return filterMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase(Locale.FRENCH);
    }

    private Map<String, String> resolveCompanyNames(List<Recruitment> coworkings) {
        Map<String, String> companyNames = new HashMap<>();
        for (Recruitment r : coworkings) {
            companyNames.computeIfAbsent(r.getCompanyId(), id ->
                    companyRepository.findByCompanyId(id).map(Company::getName).orElse("Agence"));
        }
        return companyNames;
    }

    private boolean isRetenu(JobApplication app) {
        ApplicationStatus status = app.getStatus();
        return status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.HIRED;
    }

    private boolean isNonPresente(JobApplication app) {
        String combined = ((app.getDesistement() != null ? app.getDesistement() : "") + " "
                + (app.getObservation() != null ? app.getObservation() : "")).toLowerCase(Locale.FRENCH);
        return combined.contains("non present")
                || combined.contains("non présent")
                || combined.contains("non-present")
                || combined.contains("absent");
    }

    private long count(List<JobApplication> applications, java.util.function.Predicate<JobApplication> predicate) {
        return applications.stream().filter(predicate).count();
    }

    private long countIntegrated(List<JobApplication> applications, LocalDate filterMonth) {
        LocalDate monthRef = filterMonth != null ? filterMonth : LocalDate.now().withDayOfMonth(1);
        return applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .filter(a -> {
                    if (a.getHireStartDate() != null) {
                        return a.getHireStartDate().getYear() == monthRef.getYear()
                                && a.getHireStartDate().getMonth() == monthRef.getMonth();
                    }
                    if (a.getHiredAt() != null) {
                        return a.getHiredAt().getYear() == monthRef.getYear()
                                && a.getHiredAt().getMonth() == monthRef.getMonth();
                    }
                    return false;
                })
                .count();
    }

    private List<DashboardStatusStat> buildStatusStats(List<JobApplication> applications, long total) {
        record Bucket(String key, String label, java.util.function.Predicate<JobApplication> match) {}
        List<Bucket> buckets = List.of(
                new Bucket("RETENU", "Retenus", this::isRetenu),
                new Bucket("NON_RETENU", "Non retenus", a ->
                        a.getStatus() == ApplicationStatus.REJECTED && !isNonPresente(a)),
                new Bucket("NON_PRESENTE", "Non présentés", this::isNonPresente),
                new Bucket("DESISTE", "Désistés", a -> a.getStatus() == ApplicationStatus.DESISTED),
                new Bucket("EN_COURS", "En cours", a ->
                        a.getStatus() == ApplicationStatus.SUBMITTED
                                || a.getStatus() == ApplicationStatus.UNDER_REVIEW)
        );

        return buckets.stream()
                .map(bucket -> {
                    long count = count(applications, bucket.match());
                    return DashboardStatusStat.builder()
                            .key(bucket.key())
                            .label(bucket.label())
                            .count(count)
                            .percent(total == 0 ? 0.0 : round1(count * 100.0 / total))
                            .build();
                })
                .filter(s -> s.getCount() > 0)
                .toList();
    }

    private List<DashboardSourceStat> buildSourceStats(List<JobApplication> applications) {
        Map<String, List<JobApplication>> grouped = applications.stream()
                .collect(Collectors.groupingBy(
                        a -> normalizeSource(a.getProvenance()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> toSourceStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(DashboardSourceStat::getTotal).reversed()
                        .thenComparing(DashboardSourceStat::getSource))
                .toList();
    }

    private DashboardSourceStat toSourceStat(String source, List<JobApplication> apps) {
        long total = apps.size();
        long retenus = count(apps, this::isRetenu);
        long nonRetenus = count(apps, a -> a.getStatus() == ApplicationStatus.REJECTED && !isNonPresente(a));
        long nonPresentes = count(apps, this::isNonPresente);
        return DashboardSourceStat.builder()
                .source(source)
                .total(total)
                .retenus(retenus)
                .nonRetenus(nonRetenus)
                .nonPresentes(nonPresentes)
                .selectionRate(total == 0 ? 0.0 : round1(retenus * 100.0 / total))
                .build();
    }

    private List<DashboardResponsibleStat> buildResponsibleStats(
            List<JobApplication> applications,
            Map<String, Recruitment> recruitmentById) {
        Map<String, List<JobApplication>> grouped = new LinkedHashMap<>();
        for (JobApplication app : applications) {
            Recruitment recruitment = recruitmentById.get(app.getRecruitmentId());
            String name = recruitment != null && StringUtils.hasText(recruitment.getResponsibleName())
                    ? recruitment.getResponsibleName().trim()
                    : "Non renseigné";
            grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(app);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<JobApplication> apps = entry.getValue();
                    long total = apps.size();
                    long retenus = count(apps, this::isRetenu);
                    return DashboardResponsibleStat.builder()
                            .name(entry.getKey())
                            .total(total)
                            .retenus(retenus)
                            .selectionRate(total == 0 ? 0.0 : round1(retenus * 100.0 / total))
                            .build();
                })
                .sorted(Comparator.comparingLong(DashboardResponsibleStat::getTotal).reversed()
                        .thenComparing(DashboardResponsibleStat::getName))
                .toList();
    }

    private List<DashboardAgencyStat> buildAgencyStats(
            List<JobApplication> applications,
            Map<String, Recruitment> recruitmentById,
            Map<String, String> companyNames) {
        Map<String, List<JobApplication>> grouped = new LinkedHashMap<>();
        for (JobApplication app : applications) {
            Recruitment recruitment = recruitmentById.get(app.getRecruitmentId());
            String agency = recruitment != null
                    ? companyNames.getOrDefault(recruitment.getCompanyId(), "Agence")
                    : "Agence";
            grouped.computeIfAbsent(agency, k -> new ArrayList<>()).add(app);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<JobApplication> apps = entry.getValue();
                    long total = apps.size();
                    long retenus = count(apps, this::isRetenu);
                    long nonRetenus = count(apps, a ->
                            a.getStatus() == ApplicationStatus.REJECTED && !isNonPresente(a));
                    double rate = total == 0 ? 0.0 : round1(retenus * 100.0 / total);
                    return DashboardAgencyStat.builder()
                            .agencyName(entry.getKey())
                            .candidates(total)
                            .retenus(retenus)
                            .nonRetenus(nonRetenus)
                            .selectionRate(rate)
                            .indicatorLevel(indicatorLevel(rate))
                            .build();
                })
                .sorted(Comparator.comparingLong(DashboardAgencyStat::getCandidates).reversed()
                        .thenComparing(DashboardAgencyStat::getAgencyName))
                .toList();
    }

    private List<DashboardPostStat> buildPostStats(
            List<JobApplication> applications,
            Map<String, Recruitment> recruitmentById) {
        Map<String, List<JobApplication>> grouped = new LinkedHashMap<>();
        for (JobApplication app : applications) {
            Recruitment recruitment = recruitmentById.get(app.getRecruitmentId());
            String title = recruitment != null && StringUtils.hasText(recruitment.getTitle())
                    ? recruitment.getTitle().trim()
                    : "Sans titre";
            grouped.computeIfAbsent(title, k -> new ArrayList<>()).add(app);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<JobApplication> apps = entry.getValue();
                    long total = apps.size();
                    long retenus = count(apps, this::isRetenu);
                    long nonRetenus = count(apps, a ->
                            a.getStatus() == ApplicationStatus.REJECTED && !isNonPresente(a));
                    double rate = total == 0 ? 0.0 : round1(retenus * 100.0 / total);
                    long civp = count(apps, a -> isContractType(a, "CIVP"));
                    long cdi = count(apps, a -> isContractType(a, "CDI"));
                    return DashboardPostStat.builder()
                            .title(entry.getKey())
                            .total(total)
                            .retenus(retenus)
                            .nonRetenus(nonRetenus)
                            .selectionRate(rate)
                            .civpCount(civp)
                            .cdiCount(cdi)
                            .build();
                })
                .sorted(Comparator.comparingLong(DashboardPostStat::getTotal).reversed()
                        .thenComparing(DashboardPostStat::getTitle))
                .toList();
    }

    private boolean isContractType(JobApplication app, String type) {
        if (!StringUtils.hasText(app.getHireContractType())) {
            return false;
        }
        return app.getHireContractType().trim().equalsIgnoreCase(type);
    }

    private String normalizeSource(String provenance) {
        if (!StringUtils.hasText(provenance)) {
            return "Non renseigné";
        }
        return provenance.trim();
    }

    private int indicatorLevel(double rate) {
        return Math.min(10, Math.max(0, (int) Math.round(rate / 10.0)));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
