package com.vaultx.userservice.services;

import com.vaultx.userservice.DTO.WalletDepositRequest;
import com.vaultx.userservice.DTO.WalletResponse;
import com.vaultx.userservice.Exceptions.UserNotFoundException;
import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.model.WalletTransaction;
import com.vaultx.userservice.repository.WalletRepository;
import com.vaultx.userservice.repository.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletResponse getByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse deposit(UUID userId, WalletDepositRequest request) {
        WalletResult result = walletMutation(userId, "DEPOSIT",
                request.getAmount().doubleValue(), request.getIdempotencyKey(), "Wallet deposit");
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));
        return toResponse(result, wallet);
    }

    /**
     * Applies an idempotent, guarded wallet mutation. Reserved-money model:
     * <ul>
     *   <li>RESERVE: reserved += amount (guard: reserved + amount <= balance)</li>
     *   <li>RELEASE: reserved -= amount (guard: reserved >= amount)</li>
     *   <li>DEBIT (settle): reserved -= amount AND balance -= amount (guard: reserved >= amount)</li>
     *   <li>CREDIT / REFUND / DEPOSIT: balance += amount</li>
     * </ul>
     * Replaying the same idempotency key returns the originally recorded result
     * without re-applying the mutation.
     */
    @Transactional
    public WalletResult walletMutation(UUID userId, String transactionType, double amount,
                                       String idempotencyKey, String description) {
        Optional<WalletTransaction> existing =
                walletTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            WalletTransaction t = existing.get();
            // The API contract is SUCCESS/FAILED; the ledger stores SUCCEEDED/FAILED.
            String status = "SUCCEEDED".equals(t.getStatus()) ? "SUCCESS" : t.getStatus();
            return new WalletResult(t.getId(), t.getNewBalance(), t.getNewReservedBalance(),
                    status, t.getFailureReason());
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));

        BigDecimal amt = BigDecimal.valueOf(amount);
        BigDecimal abs = amt.abs();
        BigDecimal balance = wallet.getBalance();
        BigDecimal reserved = wallet.getReserveBalance();

        String op = transactionType.toUpperCase();
        String failure = null;

        switch (op) {
            case "RESERVE" -> {
                if (reserved.add(abs).compareTo(balance) > 0) {
                    failure = "Insufficient funds to reserve " + abs;
                } else {
                    reserved = reserved.add(abs);
                }
            }
            case "RELEASE" -> {
                if (reserved.compareTo(abs) < 0) {
                    failure = "Cannot release " + abs + ", reserved balance is " + reserved;
                } else {
                    reserved = reserved.subtract(abs);
                }
            }
            case "DEBIT" -> {
                if (reserved.compareTo(abs) < 0) {
                    failure = "Insufficient reserved funds, reserved balance is " + reserved;
                } else {
                    reserved = reserved.subtract(abs);
                    balance = balance.subtract(abs);
                }
            }
            case "PURCHASE" -> {
                if(balance.compareTo(abs) < 0){
                    failure = "Insufficient Balance for purchase " + abs;
                }else {
                    balance = balance.subtract(abs);
                }
            }
            case "CREDIT", "REFUND", "DEPOSIT" -> balance = balance.add(amt);
            default -> failure = "Unknown transaction type: " + transactionType;
        }

        if (failure != null) {
            WalletTransaction tx = persist(userId, transactionType, amt, idempotencyKey,
                    "FAILED", failure, balance, reserved, description);
            return new WalletResult(tx.getId(), balance, reserved, "FAILED", failure);
        }

        wallet.setBalance(balance);
        wallet.setReserveBalance(reserved);
        walletRepository.save(wallet);

        WalletTransaction tx = persist(userId, transactionType, amt, idempotencyKey,
                "SUCCEEDED", "", balance, reserved, description);
        return new WalletResult(tx.getId(), balance, reserved, "SUCCESS", "");
    }

    private WalletTransaction persist(UUID userId, String type, BigDecimal amount,
                                      String key, String status, String failure,
                                      BigDecimal balance, BigDecimal reserved,
                                      String description) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setIdempotencyKey(key);
        tx.setStatus(status);
        tx.setFailureReason(failure);
        tx.setNewBalance(balance);
        tx.setNewReservedBalance(reserved);
        tx.setDescription(description);
        return walletTransactionRepository.save(tx);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return toResponse(new WalletResult(wallet.getId(), wallet.getBalance(),
                wallet.getReserveBalance(), "SUCCESS", ""), wallet);
    }

    private WalletResponse toResponse(WalletResult r, Wallet wallet) {
        WalletResponse resp = new WalletResponse();
        resp.setId(wallet.getId());
        resp.setUserId(wallet.getUserId());
        resp.setBalance(r.newBalance());
        resp.setReservedBalance(r.newReservedBalance());
        resp.setAvailableBalance(r.newBalance().subtract(r.newReservedBalance()));
        resp.setCurrency(wallet.getCurrency());
        return resp;
    }

    public record WalletResult(UUID transactionId, BigDecimal newBalance,
                               BigDecimal newReservedBalance, String status,
                               String failureReason) {
    }
}
