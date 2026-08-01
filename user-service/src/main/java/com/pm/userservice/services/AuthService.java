package com.pm.userservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.userservice.DTO.AuthResponseDTO;
import com.pm.userservice.DTO.LoginRequestDTO;
import com.pm.userservice.DTO.RefreshRequestDTO;
import com.pm.userservice.DTO.RegisterRequestDTO;
import com.pm.userservice.Exceptions.EmailAlreadyExistsException;
import com.pm.userservice.model.OutboxEvent;
import com.pm.userservice.model.RefreshToken;
import com.pm.userservice.model.Users;
import com.pm.userservice.model.Wallet;
import com.pm.userservice.repository.OutboxEventRepository;
import com.pm.userservice.repository.RefreshTokenRepository;
import com.pm.userservice.repository.UserRepository;
import com.pm.userservice.repository.WalletRepository;
import com.pm.userservice.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setKycStatus("PENDING");
        user.setRole("USER");
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setReserveBalance(BigDecimal.ZERO);
        wallet.setCurrency("USD");
        walletRepository.save(wallet);

        emitUserRegistered(user);

        return generateAuthResponse(user);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        Users user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return generateAuthResponse(user);
    }

    private AuthResponseDTO generateAuthResponse(Users user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setUserId(user.getId());
        rt.setToken(refreshTokenValue);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);

        return new AuthResponseDTO(accessToken, refreshTokenValue, 900000L, "Bearer");
    }

    private void emitUserRegistered(Users user) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "registeredAt", LocalDateTime.now().toString()
            );

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("USER");
            outbox.setAggregateId(user.getId().toString());
            outbox.setEventType("USER_REGISTERED");
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(outbox);

            log.info("Queued USER_REGISTERED outbox event for userId={}", user.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize USER_REGISTERED event for userId={}",
                    user.getId(), e);
        }
    }
}
