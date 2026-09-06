package com.daam.reclamation.service;

import com.daam.reclamation.client.UserClient;
import com.daam.reclamation.client.UserDto;
import com.daam.reclamation.dto.ReclamationDtos.*;
import com.daam.reclamation.entity.Reclamation;
import com.daam.reclamation.enumeration.ReclamationStatus;
import com.daam.reclamation.repository.ReclamationRepository;
import com.daam.reclamation.response.ApiResponse;
import com.daam.reclamation.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final UserClient userClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Transactional
    public ReclamationResponse create(ReclamationRequest request, AuthUser authUser) {
        if (!authUser.isCandidate() && !authUser.isAdmin() && !authUser.isRh()) {
            throw new IllegalArgumentException("Only authenticated users can create a reclamation");
        }
        Reclamation saved = reclamationRepository.save(Reclamation.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .createdByUserId(authUser.getUserId())
                .status(ReclamationStatus.OPEN)
                .build());
        return toResponse(saved, new HashMap<>());
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponse> getMine(AuthUser authUser) {
        Map<String, String> nameCache = new HashMap<>();
        return reclamationRepository.findByCreatedByUserIdOrderByCreatedAtDesc(authUser.getUserId()).stream()
                .map(r -> toResponse(r, nameCache))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponse> getAll(AuthUser authUser) {
        if (!authUser.isAdmin() && !authUser.isRh()) {
            throw new IllegalArgumentException("Only RH or Admin can list all reclamations");
        }
        Map<String, String> nameCache = new HashMap<>();
        return reclamationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toResponse(r, nameCache))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReclamationResponse getById(String reclamationId, AuthUser authUser) {
        Reclamation reclamation = getOrThrow(reclamationId);
        ensureCanView(reclamation, authUser);
        return toResponse(reclamation, new HashMap<>());
    }

    @Transactional
    public ReclamationResponse update(String reclamationId, ReclamationUpdateRequest request, AuthUser authUser) {
        Reclamation reclamation = getOrThrow(reclamationId);

        if (authUser.isCandidate()) {
            if (!authUser.getUserId().equals(reclamation.getCreatedByUserId())) {
                throw new IllegalArgumentException("Not your reclamation");
            }
            if (reclamation.getStatus() != ReclamationStatus.OPEN) {
                throw new IllegalArgumentException("Only open reclamations can be edited by the author");
            }
            if (StringUtils.hasText(request.getTitle())) {
                reclamation.setTitle(request.getTitle().trim());
            }
            if (StringUtils.hasText(request.getDescription())) {
                reclamation.setDescription(request.getDescription().trim());
            }
        } else if (authUser.isRh() || authUser.isAdmin()) {
            if (StringUtils.hasText(request.getTitle())) {
                reclamation.setTitle(request.getTitle().trim());
            }
            if (StringUtils.hasText(request.getDescription())) {
                reclamation.setDescription(request.getDescription().trim());
            }
            if (request.getRhResponse() != null) {
                reclamation.setRhResponse(blankToNull(request.getRhResponse()));
                reclamation.setHandledByUserId(authUser.getUserId());
            }
            if (request.getStatus() != null) {
                reclamation.setStatus(request.getStatus());
                reclamation.setHandledByUserId(authUser.getUserId());
            }
        } else {
            throw new IllegalArgumentException("Not allowed to update this reclamation");
        }

        return toResponse(reclamationRepository.save(reclamation), new HashMap<>());
    }

    @Transactional
    public ReclamationResponse updateStatus(String reclamationId, StatusUpdateRequest request, AuthUser authUser) {
        if (!authUser.isRh() && !authUser.isAdmin()) {
            throw new IllegalArgumentException("Only RH or Admin can update status");
        }
        Reclamation reclamation = getOrThrow(reclamationId);
        reclamation.setStatus(request.getStatus());
        reclamation.setHandledByUserId(authUser.getUserId());
        if (request.getRhResponse() != null) {
            reclamation.setRhResponse(blankToNull(request.getRhResponse()));
        }
        return toResponse(reclamationRepository.save(reclamation), new HashMap<>());
    }

    @Transactional
    public void delete(String reclamationId, AuthUser authUser) {
        Reclamation reclamation = getOrThrow(reclamationId);
        if (authUser.isAdmin()) {
            reclamationRepository.delete(reclamation);
            return;
        }
        if (authUser.isCandidate()
                && authUser.getUserId().equals(reclamation.getCreatedByUserId())
                && reclamation.getStatus() == ReclamationStatus.OPEN) {
            reclamationRepository.delete(reclamation);
            return;
        }
        throw new IllegalArgumentException("Not allowed to delete this reclamation");
    }

    private Reclamation getOrThrow(String reclamationId) {
        return reclamationRepository.findByReclamationId(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Reclamation not found"));
    }

    private void ensureCanView(Reclamation reclamation, AuthUser authUser) {
        if (authUser.isAdmin() || authUser.isRh()) {
            return;
        }
        if (authUser.getUserId().equals(reclamation.getCreatedByUserId())) {
            return;
        }
        throw new IllegalArgumentException("Not allowed to view this reclamation");
    }

    private ReclamationResponse toResponse(Reclamation r, Map<String, String> nameCache) {
        return ReclamationResponse.builder()
                .reclamationId(r.getReclamationId())
                .title(r.getTitle())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdByUserId(r.getCreatedByUserId())
                .createdByName(resolveUserName(r.getCreatedByUserId(), nameCache))
                .handledByUserId(r.getHandledByUserId())
                .handledByName(resolveUserName(r.getHandledByUserId(), nameCache))
                .rhResponse(r.getRhResponse())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String resolveUserName(String userId, Map<String, String> cache) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        try {
            ApiResponse<UserDto> resp = userClient.getUserById(internalApiKey, userId);
            if (resp.isSuccess() && resp.getData() != null) {
                UserDto u = resp.getData();
                String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                if (!StringUtils.hasText(name)) {
                    name = u.getUsername();
                }
                cache.put(userId, name);
                return name;
            }
        } catch (Exception ignored) {
            // USER unavailable
        }
        cache.put(userId, userId);
        return userId;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
