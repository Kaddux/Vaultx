package com.vaultx.userservice.services;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KycSubmissionRepository kycSubmissionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;

    private Users createTestUser() {
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPhone("1234567890");
        user.setKycStatus("PENDING");
        user.setRole("USER");
        return user;
    }

    @Test
    void getById_shouldReturnUserResponseDTO_whenUserExists() {
        Users user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getById(user.getId());

        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("1234567890", response.getPhone());
        assertEquals("PENDING", response.getKycStatus());
        assertEquals("USER", response.getRole());

        verify(userRepository).findById(user.getId());
    }

    @Test
    void getById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getById(id));

        assertTrue(exception.getMessage().contains(id.toString()));
        verify(userRepository).findById(id);
    }

    @Test
    void getByEmail_shouldReturnUserResponseDTO_whenUserExists() {
        Users user = createTestUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getByEmail("test@example.com");

        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getFullName());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getByEmail_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getByEmail("nonexistent@example.com"));

        assertTrue(exception.getMessage().contains("nonexistent@example.com"));
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void getUserEntityById_shouldReturnUsersEntity_whenUserExists() {
        Users user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Users result = userService.getUserEntityById(user.getId());

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findById(user.getId());
    }

    @Test
    void getUserEntityById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUserEntityById(id));

        assertTrue(exception.getMessage().contains(id.toString()));
        verify(userRepository).findById(id);
    }

    @Test
    void update_shouldReturnUserResponseDTO_whenFullNameAndPhoneUpdated() {
        Users user = createTestUser();
        UUID userId = user.getId();

        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setFullName("Updated Name");
        request.setPhone("0987654321");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(Users.class))).thenReturn(user);

        UserResponseDTO response = userService.update(userId, request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("0987654321", response.getPhone());

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldReturnUserResponseDTO_whenOnlyFullNameUpdated() {
        Users user = createTestUser();
        user.setPhone("1234567890");
        UUID userId = user.getId();

        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setFullName("New Full Name");
        request.setPhone(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(Users.class))).thenReturn(user);

        UserResponseDTO response = userService.update(userId, request);

        assertNotNull(response);
        assertEquals("New Full Name", response.getFullName());
        assertEquals("1234567890", response.getPhone());

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setFullName("New Name");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.update(id, request));

        assertTrue(exception.getMessage().contains(id.toString()));
        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any(Users.class));
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.delete(id);

        verify(userRepository).existsById(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.delete(id));

        assertTrue(exception.getMessage().contains(id.toString()));
        verify(userRepository).existsById(id);
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void submitKyc_shouldPersistSubmissionSetVerifiedAndEmitEvent() throws Exception {
        Users user = createTestUser();
        UUID userId = user.getId();

        KycRequestDTO request = new KycRequestDTO();
        request.setDocType("Passport");
        request.setFullName("Alex Morgan");
        request.setAddress("123 Bidding Ave, Austin, TX");
        request.setDocumentRef("doc_scan.jpg");
        request.setSelfieRef("selfie.png");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(kycSubmissionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(kycSubmissionRepository.save(any(KycSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(Users.class))).thenReturn(user);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        UserResponseDTO response = userService.submitKyc(userId, request);

        assertEquals("VERIFIED", response.getKycStatus());
        assertEquals("Alex Morgan", response.getFullName());

        ArgumentCaptor<KycSubmission> submissionCaptor = ArgumentCaptor.forClass(KycSubmission.class);
        verify(kycSubmissionRepository).save(submissionCaptor.capture());
        KycSubmission saved = submissionCaptor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("Passport", saved.getDocType());
        assertEquals("VERIFIED", saved.getStatus());

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertEquals("KYC_SUBMITTED", outboxCaptor.getValue().getEventType());
    }

    @Test
    void submitKyc_existingSubmission_shouldUpdateInPlace() throws Exception {
        Users user = createTestUser();
        UUID userId = user.getId();

        KycSubmission existing = new KycSubmission();
        existing.setUserId(userId);
        existing.setDocType("Driver License");
        existing.setStatus("VERIFIED");

        KycRequestDTO request = new KycRequestDTO();
        request.setDocType("National ID");
        request.setFullName("Alex Morgan");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(kycSubmissionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(kycSubmissionRepository.save(any(KycSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(Users.class))).thenReturn(user);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        UserResponseDTO response = userService.submitKyc(userId, request);

        assertEquals("VERIFIED", response.getKycStatus());
        verify(kycSubmissionRepository).save(existing);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
