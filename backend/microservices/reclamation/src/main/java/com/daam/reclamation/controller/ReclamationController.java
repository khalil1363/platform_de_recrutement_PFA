package com.daam.reclamation.controller;

import com.daam.reclamation.dto.ReclamationDtos.*;
import com.daam.reclamation.response.ApiResponse;
import com.daam.reclamation.security.AuthUser;
import com.daam.reclamation.service.ReclamationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamation")
@RequiredArgsConstructor
public class ReclamationController {

    private final ReclamationService reclamationService;

    @PostMapping("/reclamations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReclamationResponse>> create(
            @Valid @RequestBody ReclamationRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Reclamation created",
                reclamationService.create(request, user),
                HttpStatus.CREATED.value()));
    }

    @GetMapping("/reclamations/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReclamationResponse>>> myReclamations(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "My reclamations",
                reclamationService.getMine(user),
                HttpStatus.OK.value()));
    }

    @GetMapping("/reclamations")
    @PreAuthorize("hasAnyAuthority('ROLE_RH','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ReclamationResponse>>> allReclamations(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Reclamations",
                reclamationService.getAll(user),
                HttpStatus.OK.value()));
    }

    @GetMapping("/reclamations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReclamationResponse>> getById(
            @PathVariable String id,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Reclamation",
                reclamationService.getById(id, user),
                HttpStatus.OK.value()));
    }

    @PutMapping("/reclamations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReclamationResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody ReclamationUpdateRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Reclamation updated",
                reclamationService.update(id, request, user),
                HttpStatus.OK.value()));
    }

    @PatchMapping("/reclamations/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_RH','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ReclamationResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Status updated",
                reclamationService.updateStatus(id, request, user),
                HttpStatus.OK.value()));
    }

    @DeleteMapping("/reclamations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthUser user) {
        reclamationService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Reclamation deleted", null, HttpStatus.OK.value()));
    }
}
