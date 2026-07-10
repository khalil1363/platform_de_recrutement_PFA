package com.daam.recruitment.controller;

import com.daam.recruitment.dto.RecruitmentDtos.*;
import com.daam.recruitment.response.ApiResponse;
import com.daam.recruitment.security.AuthUser;
import com.daam.recruitment.service.CvStorageService;
import com.daam.recruitment.service.QcmService;
import com.daam.recruitment.service.RecruitmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final QcmService qcmService;
    private final CvStorageService cvStorageService;

    // ---- Public ----
    @GetMapping("/public/recruitments")
    public ResponseEntity<ApiResponse<List<RecruitmentResponse>>> getPublished() {
        return ResponseEntity.ok(ApiResponse.success("Published recruitments",
                recruitmentService.getPublishedRecruitments(), HttpStatus.OK.value()));
    }

    @GetMapping("/public/recruitments/{id}")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> getPublishedDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Recruitment details",
                recruitmentService.getRecruitmentForCandidate(id), HttpStatus.OK.value()));
    }

    // ---- Admin: Zones ----
    @PostMapping("/zones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Zone created",
                recruitmentService.createZone(request), HttpStatus.CREATED.value()));
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RH')")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones() {
        return ResponseEntity.ok(ApiResponse.success("Zones", recruitmentService.getAllZones(), HttpStatus.OK.value()));
    }

    @PostMapping("/rh-zone-assignments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<RhZoneAssignmentResponse>>> assignRh(@Valid @RequestBody RhZoneAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "RH assigned to zones",
                recruitmentService.assignRhToZone(request),
                HttpStatus.CREATED.value()));
    }

    @GetMapping("/rh-zone-assignments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<RhZoneAssignmentResponse>>> getRhAssignments() {
        return ResponseEntity.ok(ApiResponse.success(
                "RH assignments",
                recruitmentService.getRhZoneAssignments(),
                HttpStatus.OK.value()));
    }

    // ---- Companies ----
    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CompanyRequest request, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Company created",
                recruitmentService.createCompany(request, user), HttpStatus.CREATED.value()));
    }

    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanies(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Companies",
                recruitmentService.getCompanies(user), HttpStatus.OK.value()));
    }

    // ---- QCM bank ----
    @PostMapping("/qcm")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<QcmResponse>> createQcm(
            @Valid @RequestBody QcmRequest request, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "QCM created", qcmService.createQcm(request, user), HttpStatus.CREATED.value()));
    }

    @GetMapping("/qcm")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RH')")
    public ResponseEntity<ApiResponse<List<QcmResponse>>> listQcm(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("QCMs", qcmService.listQcms(user), HttpStatus.OK.value()));
    }

    @GetMapping("/qcm/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RH')")
    public ResponseEntity<ApiResponse<QcmResponse>> getQcm(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("QCM", qcmService.getQcm(id, true), HttpStatus.OK.value()));
    }

    @PutMapping("/qcm/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<QcmResponse>> updateQcm(
            @PathVariable String id,
            @Valid @RequestBody QcmRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "QCM updated", qcmService.updateQcm(id, request, user), HttpStatus.OK.value()));
    }

    @DeleteMapping("/qcm/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<Void>> deleteQcm(
            @PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        qcmService.deleteQcm(id, user);
        return ResponseEntity.ok(ApiResponse.success("QCM deleted", null, HttpStatus.OK.value()));
    }

    // ---- Recruitments ----
    @PostMapping("/recruitments")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> createRecruitment(
            @Valid @RequestBody RecruitmentRequest request, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Recruitment created",
                recruitmentService.createRecruitment(request, user), HttpStatus.CREATED.value()));
    }

    @PutMapping("/recruitments/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> updateRecruitment(
            @PathVariable String id, @Valid @RequestBody RecruitmentRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Recruitment updated",
                recruitmentService.updateRecruitment(id, request, user), HttpStatus.OK.value()));
    }

    @GetMapping("/recruitments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RecruitmentResponse>>> getRecruitments(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Recruitments",
                recruitmentService.getRecruitments(user), HttpStatus.OK.value()));
    }

    @GetMapping("/recruitments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> getRecruitment(
            @PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        boolean includeAnswers = user.isAdmin() || user.isRh();
        return ResponseEntity.ok(ApiResponse.success("Recruitment",
                recruitmentService.getRecruitment(id, includeAnswers), HttpStatus.OK.value()));
    }

    // ---- CV Upload ----
    @PostMapping("/upload/cv")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCv(@RequestParam("file") MultipartFile file) {
        String url = cvStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success("CV uploaded", Map.of("cvFileUrl", url), HttpStatus.OK.value()));
    }

    // ---- Applications ----
    @PostMapping("/applications")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @Valid @RequestBody ApplicationRequest request, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Application submitted",
                recruitmentService.apply(request, user), HttpStatus.CREATED.value()));
    }

    @GetMapping("/applications/my")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> myApplications(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("My applications",
                recruitmentService.getMyApplications(user), HttpStatus.OK.value()));
    }

    @GetMapping("/recruitments/{id}/applications")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RH')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplications(
            @PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Applications",
                recruitmentService.getApplicationsForRecruitment(id, user), HttpStatus.OK.value()));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getRhApplications(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Applications",
                recruitmentService.getApplicationsForRh(user), HttpStatus.OK.value()));
    }

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable String applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("Application status updated",
                recruitmentService.updateApplicationStatus(applicationId, request.getStatus(), request.getInterviewAt(), user),
                HttpStatus.OK.value()));
    }

    @PostMapping("/applications/{applicationId}/analyze-cv")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> analyzeApplicationCv(
            @PathVariable String applicationId,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success("CV analysis completed",
                recruitmentService.analyzeApplicationCv(applicationId, user), HttpStatus.OK.value()));
    }
}
