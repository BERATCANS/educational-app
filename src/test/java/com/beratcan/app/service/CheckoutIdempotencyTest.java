package com.beratcan.app.service;

import com.beratcan.app.dto.response.PurchaseResponse;
import com.beratcan.app.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@SpringBootTest
@Transactional
public class CheckoutIdempotencyTest {

    private static final Long STUDENT_PROFILE_ID = 1L;
    private static final Long USER_ID = 32L;

    @Autowired private PurchaseService purchaseService;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    @Test
    void rejectsMalformedIdempotencyKey() {
        Assertions.assertThrows(BadRequestException.class,
                () -> purchaseService.checkoutCart(USER_ID, "  "));
        Assertions.assertThrows(BadRequestException.class,
                () -> purchaseService.checkoutCart(USER_ID, "bad/space"));
        Assertions.assertThrows(BadRequestException.class,
                () -> purchaseService.checkoutCart(USER_ID, "x".repeat(81)));
    }

    @Test
    void replayReturnsExistingPurchaseAndCreatesNoNewRow() {
        String key = "test-replay-key-1";
        Long existingId = insertPurchase(STUDENT_PROFILE_ID, key);
        em.flush();

        Optional<PurchaseResponse> via = purchaseService.findByIdempotencyKey(USER_ID, key);
        Assertions.assertTrue(via.isPresent(), "findByIdempotencyKey must locate the seeded row");
        Assertions.assertEquals(existingId, via.get().getId());

        PurchaseResponse replay = purchaseService.checkoutCart(USER_ID, key);
        Assertions.assertEquals(existingId, replay.getId(),
                "Replay must return the same Purchase id; no new row");

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM purchases WHERE idempotency_key = ?",
                Integer.class, key);
        Assertions.assertEquals(1, count, "No second purchase row may exist for this key");
    }

    @Test
    void partialUniqueIndexRejectsDuplicateKeyForSameStudent() {
        String key = "test-race-key";
        insertPurchase(STUDENT_PROFILE_ID, key);

        Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                insertPurchase(STUDENT_PROFILE_ID, key),
                "Second insert for same (student_profile_id, idempotency_key) must violate uk_purchases_student_idempotency");
    }

    @Test
    void partialUniqueIndexAllowsMultipleNullKeysForSameStudent() {
        Long a = insertPurchase(STUDENT_PROFILE_ID, null);
        Long b = insertPurchase(STUDENT_PROFILE_ID, null);
        Assertions.assertNotNull(a);
        Assertions.assertNotNull(b);
        Assertions.assertNotEquals(a, b);
    }

    private Long insertPurchase(Long studentProfileId, String idempotencyKey) {
        Long id = jdbc.queryForObject(
                "INSERT INTO purchases (student_profile_id, total_price, final_price, " +
                        "item_count, purchase_date, status, idempotency_key) " +
                        "VALUES (?, ?, ?, ?, NOW(), 'COMPLETED', ?) RETURNING id",
                Long.class,
                studentProfileId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                idempotencyKey);
        return id;
    }
}
