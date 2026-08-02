package com.vaultx.userservice.services;

import com.vaultx.userservice.DTO.WalletDepositRequest;
import com.vaultx.userservice.DTO.WalletResponse;
import com.vaultx.userservice.Exceptions.UserNotFoundException;
import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet createTestWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setReserveBalance(new BigDecimal("20.00"));
        wallet.setCurrency("USD");
        return wallet;
    }

    @Test
    void getByUserId_shouldReturnWalletResponse_whenWalletExists() {
        Wallet wallet = createTestWallet();
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getByUserId(wallet.getUserId());

        assertNotNull(response);
        assertEquals(wallet.getId(), response.getId());
        assertEquals(wallet.getUserId(), response.getUserId());
        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals(new BigDecimal("20.00"), response.getReservedBalance());
        assertEquals(new BigDecimal("80.00"), response.getAvailableBalance());
        assertEquals("USD", response.getCurrency());

        verify(walletRepository).findByUserId(wallet.getUserId());
    }

    @Test
    void getByUserId_shouldThrowUserNotFoundException_whenWalletDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> walletService.getByUserId(userId));

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void deposit_shouldAddAmountToBalanceAndReturnWalletResponse() {
        Wallet wallet = createTestWallet();
        BigDecimal initialBalance = wallet.getBalance();

        WalletDepositRequest request = new WalletDepositRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey("idem-key-123");

        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletResponse response = walletService.deposit(wallet.getUserId(), request);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.00"), response.getBalance());
        assertEquals(new BigDecimal("20.00"), response.getReservedBalance());
        assertEquals(new BigDecimal("130.00"), response.getAvailableBalance());

        assertEquals(new BigDecimal("150.00"), wallet.getBalance());

        verify(walletRepository).findByUserId(wallet.getUserId());
        verify(walletRepository).save(wallet);
    }

    @Test
    void deposit_shouldThrowUserNotFoundException_whenWalletDoesNotExist() {
        UUID userId = UUID.randomUUID();
        WalletDepositRequest request = new WalletDepositRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey("idem-key-456");

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> walletService.deposit(userId, request));

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(walletRepository).findByUserId(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
