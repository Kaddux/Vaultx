package com.vaultx.userservice.config;

import com.vaultx.userservice.model.Users;
import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.repository.UserRepository;
import com.vaultx.userservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String DEMO_EMAIL = "demo@vaultx.io";
    public static final String DEMO_PASSWORD = "Demo1234!";
    public static final BigDecimal DEMO_BALANCE = new BigDecimal("10000.00");

    @Bean
    public CommandLineRunner seedDemoUser() {
        return args -> {
            if (userRepository.existsByEmail(DEMO_EMAIL)) {
                log.info("Demo user already exists, checking wallet");
                Users existing = userRepository.findByEmail(DEMO_EMAIL).orElse(null);
                if (existing != null
                        && walletRepository.findByUserId(existing.getId()).isEmpty()) {
                    createWallet(existing);
                }
                return;
            }

            Users user = new Users();
            user.setUsername("alexmorgan");
            user.setEmail(DEMO_EMAIL);
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            user.setFullName("Alex Morgan");
            user.setPhone("+15550001111");
            user.setKycStatus("VERIFIED");
            user.setRole("SELLER");
            user.setVersion(0L);
            userRepository.saveAndFlush(user);

            createWallet(user);

            log.info("Seeded demo user: email={}, userId={}, balance={}",
                    DEMO_EMAIL, user.getId(), DEMO_BALANCE);
        };
    }

    private void createWallet(Users user) {
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(DEMO_BALANCE);
        wallet.setReserveBalance(BigDecimal.ZERO);
        wallet.setCurrency("USD");
        wallet.setVersion(0L);
        walletRepository.saveAndFlush(wallet);
        log.info("Seeded wallet for userId={}: balance={}", user.getId(), DEMO_BALANCE);
    }
}
