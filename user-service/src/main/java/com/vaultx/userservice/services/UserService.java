package com.vaultx.userservice.services;

import com.vaultx.userservice.DTO.UserResponseDTO;
import com.vaultx.userservice.DTO.UserUpdateRequestDTO;
import com.vaultx.userservice.Exceptions.UserNotFoundException;
import com.vaultx.userservice.model.Users;
import com.vaultx.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
