package com.pm.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.userservice.DTO.AuthResponseDTO;
import com.pm.userservice.DTO.LoginRequestDTO;
import com.pm.userservice.DTO.RefreshRequestDTO;
import com.pm.userservice.DTO.RegisterRequestDTO;
import com.pm.userservice.Exceptions.EmailAlreadyExistsException;
import com.pm.userservice.security.JwtAuthenticationFilter;
import com.pm.userservice.security.JwtTokenProvider;
import com.pm.userservice.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void register_shouldReturnCreatedWithAuthResponse() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        AuthResponseDTO response = new AuthResponseDTO("access-token", "refresh-token", 900000L, "Bearer");

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already registered: existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturnBadRequestForInvalidData() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setEmail("");
        request.setPassword("password123");
        request.setFullName("Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnOkWithAuthResponse() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponseDTO response = new AuthResponseDTO("access-token", "refresh-token", 900000L, "Bearer");

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_shouldReturnUnauthorizedWhenBadCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturnOkWithNewTokens() throws Exception {
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("valid-refresh-token");

        AuthResponseDTO response = new AuthResponseDTO("new-access-token", "new-refresh-token", 900000L, "Bearer");

        when(authService.refresh(any(RefreshRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refresh_shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken("invalid-refresh-token");

        when(authService.refresh(any(RefreshRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
