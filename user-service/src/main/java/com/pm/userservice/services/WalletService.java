package com.pm.userservice.services;

import com.pm.userservice.DTO.WalletDepositRequest;
import com.pm.userservice.DTO.WalletResponse;
import com.pm.userservice.Exceptions.UserNotFoundException;
import com.pm.userservice.model.Wallet;
import com.pm.userservice.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletResponse getByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse deposit(UUID userId, WalletDepositRequest request) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        return toResponse(walletRepository.save(wallet));
    }

    private WalletResponse toResponse(Wallet wallet) {
        WalletResponse r = new WalletResponse();
        r.setId(wallet.getId());
        r.setUserId(wallet.getUserId());
        r.setBalance(wallet.getBalance());
        r.setReservedBalance(wallet.getReserveBalance());
        r.setAvailableBalance(wallet.getBalance().subtract(wallet.getReserveBalance()));
        r.setCurrency(wallet.getCurrency());
        return r;
    }
}
