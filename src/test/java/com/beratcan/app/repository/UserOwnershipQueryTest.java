package com.beratcan.app.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserOwnershipQueryTest {

    private static final Long USER_ID = 32L;
    private static final Long OWNED_BOOK_ID = 6L;
    private static final Long UNOWNED_BOOK_ID = 999_999L; // beyond current data

    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void returnsTrueWhenJoinTableHasMatchingRow() {
        jdbc.update(
                "INSERT INTO user_books (user_id, book_id) VALUES (?, ?) " +
                        "ON CONFLICT (user_id, book_id) DO NOTHING",
                USER_ID, OWNED_BOOK_ID);

        Assertions.assertTrue(
                userRepository.existsByIdAndOwnedBooks_Id(USER_ID, OWNED_BOOK_ID),
                "User must be reported as owner of seeded book");
    }

    @Test
    void returnsFalseWhenJoinTableHasNoMatchingRow() {
        Assertions.assertFalse(
                userRepository.existsByIdAndOwnedBooks_Id(USER_ID, UNOWNED_BOOK_ID),
                "User must not be reported as owner of an unseeded book");
    }
}
