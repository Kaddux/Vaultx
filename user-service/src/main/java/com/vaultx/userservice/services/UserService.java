package com.vaultx.userservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.userservice.DTO.KycRequestDTO;
import com.vaultx.userservice.DTO.UserResponseDTO;
import com.vaultx.userservice.DTO.UserUpdateRequestDTO;
import com.vaultx.userservice.Exceptions.UserNotFoundException;
import com.vaultx.userservice.model.KycSubmission;
import com.vaultx.userservice.model.OutboxEvent;
import com.vaultx.userservice.model.Users;
import com.vaultx.userservice.repository.KycSubmissionRepository;
import com.vaultx.userservice.repository.OutboxEventRepository;
import com.vaultx.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KycSubmissionRepository kycSubmissionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public UserResponseDTO getByEmail(String email) {
        return toResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email)));
    }
    public UserResponseDTO getById(UUID id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id)));
    }

    public Users getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Transactional
    public UserResponseDTO update(UUID id, UserUpdateRequestDTO request) {
        Users user = getUserEntityById(id);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDTO submitKyc(UUID userId, KycRequestDTO request) {
        Users user = getUserEntityById(userId);

        KycSubmission submission = kycSubmissionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    KycSubmission k = new KycSubmission();
                    k.setUserId(userId);
                    return k;
                });
        submission.setDocType(request.getDocType());
        submission.setFullName(request.getFullName());
        submission.setAddress(request.getAddress());
        submission.setDocumentRef(request.getDocumentRef());
        submission.setSelfieRef(request.getSelfieRef());
        submission.setStatus("VERIFIED");
        kycSubmissionRepository.save(submission);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        user.setKycStatus("VERIFIED");
        userRepository.save(user);

        emitKycSubmitted(user, submission);

        return toResponse(user);
    }

    private void emitKycSubmitted(Users user, KycSubmission submission) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername(),
                    "status", submission.getStatus(),
                    "docType", submission.getDocType(),
                    "submittedAt", submission.getSubmittedAt() != null
                            ? submission.getSubmittedAt().toString()
                            : LocalDateTime.now().toString()
            );

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("USER");
            outbox.setAggregateId(user.getId().toString());
            outbox.setEventType("KYC_SUBMITTED");
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize KYC_SUBMITTED event for userId={}",
                    user.getId(), e);
        }
    }

    //Mapper Method

    private UserResponseDTO toResponse(Users user) {
        UserResponseDTO r = new UserResponseDTO();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setEmail(user.getEmail());
        r.setFullName(user.getFullName());
        r.setPhone(user.getPhone());
        r.setKycStatus(user.getKycStatus());
        r.setRole(user.getRole());
        r.setCreatedAt(user.getCreated_at());
        return r;
    }
}
