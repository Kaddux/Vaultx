package com.pm.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.userservice.DTO.UserResponseDTO;
import com.pm.userservice.DTO.UserUpdateRequestDTO;
import com.pm.userservice.Exceptions.UserNotFoundException;
import com.pm.userservice.security.JwtAuthenticationFilter;
import com.pm.userservice.security.JwtTokenProvider;
import com.pm.userservice.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUserId, null, List.of()));
    }

    @Test
    void getMe_shouldReturnUserResponse() throws Exception {
        UserResponseDTO response = new UserResponseDTO();
        response.setId(testUserId);
        response.setUsername("testuser");
        response.setEmail("test@example.com");

        when(userService.getById(testUserId)).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getMe_shouldReturnNotFoundWhenUserNotFound() throws Exception {
        when(userService.getById(testUserId))
                .thenThrow(new UserNotFoundException("User not found: " + testUserId));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMe_shouldReturnUpdatedUserResponse() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setFullName("Updated Name");
        request.setPhone("1234567890");

        UserResponseDTO response = new UserResponseDTO();
        response.setId(testUserId);
        response.setUsername("testuser");
        response.setEmail("test@example.com");
        response.setFullName("Updated Name");
        response.setPhone("1234567890");

        when(userService.update(eq(testUserId), any(UserUpdateRequestDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("1234567890"));
    }

    @Test
    void updateMe_shouldReturnBadRequestWhenValidationError() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("malformed"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMe_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());
    }
}
