package com.vaultx.userservice.services;

import com.vaultx.userservice.DTO.WalletDepositRequest;
import com.vaultx.userservice.DTO.WalletResponse;
import com.vaultx.userservice.Exceptions.UserNotFoundException;
import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.model.WalletTransaction;
import com.vaultx.userservice.repository.WalletRepository;
import com.vaultx.userservice.repository.WalletTransactionRepository;
import com.vaultx.userservice.services.WalletService.WalletResult;
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

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

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

    private Wallet createTestWallet(BigDecimal balance, BigDecimal reserved) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(balance);
        wallet.setReserveBalance(reserved);
        wallet.setCurrency("USD");
        return wallet;
    }
    @Test
    void purchase_success_shouldDecreaseBalance() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);
        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "PURCHASE", 20.0, "pur-1", "payment");
        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("80.00"), result.newBalance());
        assertEquals(new BigDecimal("20.00"), result.newReservedBalance());
    }

    @Test
    void purchase_insufficient_shouldFail() {
        Wallet wallet = createTestWallet();
        wallet.setBalance(new BigDecimal("15.00"));
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "PURCHASE", 20.0, "pur-2", "payment");
        assertEquals("FAILED", result.status());
        assertEquals(new BigDecimal("15.00"), result.newBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getByUserId_shouldReturnWalletResponse_whenWalletExists() {
        Wallet wallet = createTestWallet();
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getByUserId(wallet.getUserId());

        assertNotNull(response);
        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals(new BigDecimal("20.00"), response.getReservedBalance());
        assertEquals(new BigDecimal("80.00"), response.getAvailableBalance());
        assertEquals("USD", response.getCurrency());

        verify(walletRepository).findByUserId(wallet.getUserId());
    }

    @Test
    void deposit_shouldAddAmountToBalanceAndReturnWalletResponse() {
        Wallet wallet = createTestWallet();
        WalletDepositRequest request = new WalletDepositRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey("idem-key-123");

        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.deposit(wallet.getUserId(), request);

        assertEquals(new BigDecimal("150.00"), response.getBalance());
        assertEquals(new BigDecimal("20.00"), response.getReservedBalance());
        assertEquals(new BigDecimal("130.00"), response.getAvailableBalance());
        verify(walletTransactionRepository).findByIdempotencyKey("idem-key-123");
    }

    @Test
    void reserve_shouldIncreaseReservedBalance() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "RESERVE", 10.0, "r-1", "reserve");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("30.00"), result.newReservedBalance());
        assertEquals(new BigDecimal("100.00"), result.newBalance());
    }

    @Test
    void reserve_insufficientFunds_shouldFail() {
        Wallet wallet = createTestWallet();
        wallet.setBalance(new BigDecimal("25.00"));
        wallet.setReserveBalance(new BigDecimal("20.00"));
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "RESERVE", 10.0, "r-2", "reserve");

        assertEquals("FAILED", result.status());
        assertEquals(new BigDecimal("20.00"), result.newReservedBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void release_shouldDecreaseReservedBalance() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "RELEASE", 5.0, "rel-1", "release");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("15.00"), result.newReservedBalance());
        assertEquals(new BigDecimal("100.00"), result.newBalance());
    }

    @Test
    void release_insufficientReserved_shouldFail() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "RELEASE", 30.0, "rel-2", "release");

        assertEquals("FAILED", result.status());
        assertEquals(new BigDecimal("20.00"), result.newReservedBalance());
    }

    @Test
    void debit_shouldMoveReservedToBalanceCharge() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "DEBIT", -20.0, "db-1", "settle");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("0.00"), result.newReservedBalance());
        assertEquals(new BigDecimal("80.00"), result.newBalance());
    }

    @Test
    void debit_insufficientReserved_shouldFail() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "DEBIT", -50.0, "db-2", "settle");

        assertEquals("FAILED", result.status());
        assertEquals(new BigDecimal("20.00"), result.newReservedBalance());
        assertEquals(new BigDecimal("100.00"), result.newBalance());
    }

    @Test
    void credit_shouldIncreaseBalance() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "CREDIT", 25.0, "cr-1", "release to seller");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("125.00"), result.newBalance());
        assertEquals(new BigDecimal("20.00"), result.newReservedBalance());
    }

    @Test
    void refund_shouldIncreaseBalance() {
        Wallet wallet = createTestWallet();
        stubWallet(wallet);

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "REFUND", 10.0, "rf-1", "refund");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("110.00"), result.newBalance());
    }

    @Test
    void idempotentReplay_shouldReturnRecordedResultWithoutReapplying() {
        Wallet wallet = createTestWallet();
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID());
        tx.setStatus("SUCCEEDED");
        tx.setNewBalance(new BigDecimal("100.00"));
        tx.setNewReservedBalance(new BigDecimal("40.00"));

        when(walletTransactionRepository.findByIdempotencyKey("dup-key"))
                .thenReturn(Optional.of(tx));

        WalletResult result = walletService.walletMutation(
                wallet.getUserId(), "RESERVE", 20.0, "dup-key", "reserve");

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("40.00"), result.newReservedBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    private void stubWallet(Wallet wallet) {
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }
}
