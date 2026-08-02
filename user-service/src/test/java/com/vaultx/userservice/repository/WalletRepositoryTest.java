package com.vaultx.userservice.repository;

import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private UUID testUserId;
    private Wallet savedWallet;

    private Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setReserveBalance(BigDecimal.ZERO);
        wallet.setCurrency("USD");
        return wallet;
    }

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        testUserId = UUID.randomUUID();
        savedWallet = walletRepository.save(createWallet(testUserId));
    }

    @Test
    void findByUserId_ShouldReturnWallet_WhenUserIdExists() {
        Optional<Wallet> found = walletRepository.findByUserId(testUserId);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(testUserId);
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenUserIdDoesNotExist() {
        Optional<Wallet> found = walletRepository.findByUserId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldPersistNewWallet() {
        UUID newUserId = UUID.randomUUID();
        Wallet newWallet = createWallet(newUserId);

        Wallet persisted = walletRepository.save(newWallet);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getUserId()).isEqualTo(newUserId);
        assertThat(persisted.getCreatedAt()).isNotNull();

        Optional<Wallet> retrieved = walletRepository.findById(persisted.getId());
        assertThat(retrieved).isPresent();
    }

    @Test
    void updateBalanceAndSave_ShouldPersistChanges() {
        BigDecimal newBalance = new BigDecimal("100.00");
        savedWallet.setBalance(newBalance);

        Wallet updated = walletRepository.save(savedWallet);

        assertThat(updated.getBalance()).isEqualByComparingTo(newBalance);

        Optional<Wallet> retrieved = walletRepository.findById(savedWallet.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getBalance()).isEqualByComparingTo(newBalance);
    }
}
