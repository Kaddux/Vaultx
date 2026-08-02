package com.vaultx.userservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.userservice.DTO.AuthResponseDTO;
import com.vaultx.userservice.DTO.LoginRequestDTO;
import com.vaultx.userservice.DTO.RefreshRequestDTO;
import com.vaultx.userservice.DTO.RegisterRequestDTO;
import com.vaultx.userservice.Exceptions.EmailAlreadyExistsException;
import com.vaultx.userservice.model.OutboxEvent;
import com.vaultx.userservice.model.RefreshToken;
import com.vaultx.userservice.model.Users;
import com.vaultx.userservice.repository.OutboxEventRepository;
import com.vaultx.userservice.repository.RefreshTokenRepository;
import com.vaultx.userservice.repository.UserRepository;
import com.vaultx.userservice.repository.WalletRepository;
import com.vaultx.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldReturnAuthResponse_whenEmailDoesNotExist() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(Users.class))).thenAnswer(inv -> {
            Users saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(walletRepository.save(any(com.vaultx.userservice.model.Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), anyString())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class))).thenReturn("refresh_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(Users.class));
        verify(walletRepository).save(any(com.vaultx.userservice.model.Wallet.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_shouldThrowEmailAlreadyExistsException_whenEmailExists() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(request));

        assertTrue(exception.getMessage().contains("existing@example.com"));

        verify(userRepository, never()).save(any(Users.class));
        verify(walletRepository, never()).save(any(com.vaultx.userservice.model.Wallet.class));
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole("USER");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), anyString())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class))).thenReturn("refresh_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "hashed_password");
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenEmailNotFound() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenPasswordDoesNotMatch() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("wrong_password");

        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole("USER");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void refresh_shouldReturnNewAuthResponse_whenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("valid_refresh_token");

        RefreshToken stored = new RefreshToken();
        stored.setId(UUID.randomUUID());
        stored.setUserId(userId);
        stored.setToken("valid_refresh_token");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        stored.setRevoked(false);

        Users user = new Users();
        user.setId(userId);
        user.setRole("USER");

        when(refreshTokenRepository.findByToken("valid_refresh_token")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), anyString())).thenReturn("new_access_token");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class))).thenReturn("new_refresh_token");

        AuthResponseDTO response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());

        assertTrue(stored.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldThrowBadCredentialsException_whenTokenNotFound() {
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("unknown_token");

        when(refreshTokenRepository.findByToken("unknown_token")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.refresh(request));

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refresh_shouldThrowBadCredentialsException_whenTokenIsRevoked() {
        UUID userId = UUID.randomUUID();
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("revoked_token");

        RefreshToken stored = new RefreshToken();
        stored.setId(UUID.randomUUID());
        stored.setUserId(userId);
        stored.setToken("revoked_token");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        stored.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked_token")).thenReturn(Optional.of(stored));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.refresh(request));

        assertEquals("Refresh token expired or revoked", exception.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldThrowBadCredentialsException_whenTokenIsExpired() {
        UUID userId = UUID.randomUUID();
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("expired_token");

        RefreshToken stored = new RefreshToken();
        stored.setId(UUID.randomUUID());
        stored.setUserId(userId);
        stored.setToken("expired_token");
        stored.setExpiresAt(LocalDateTime.now().minusDays(1));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(stored));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.refresh(request));

        assertEquals("Refresh token expired or revoked", exception.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
