package com.beratcan.app.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InviteCodeGeneratorTest {

    private final InviteCodeGenerator generator = new InviteCodeGenerator();

    @Test
    void generate_isExactly6Characters() {
        for (int i = 0; i < 100; i++) {
            assertEquals(6, generator.generate().length());
        }
    }

    @Test
    void generate_excludesConfusingCharacters() {
        Set<Character> banned = Set.of('0', 'O', '1', 'I', 'L');
        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            for (char c : code.toCharArray()) {
                assertFalse(banned.contains(c),
                        "Code '" + code + "' contains confusing character: " + c);
            }
        }
    }

    @Test
    void generate_onlyUppercaseAndDigits() {
        for (int i = 0; i < 100; i++) {
            String code = generator.generate();
            assertTrue(code.matches("[A-Z2-9]{6}"),
                    "Code '" + code + "' has invalid characters");
        }
    }

    @Test
    void generateUnique_returnsCodeWhenExistsCheckAlwaysFalse() {
        String code = generator.generateUnique(c -> false);
        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    void generateUnique_retriesUntilFindingUnusedCode() {
        AtomicInteger attempts = new AtomicInteger();
        Set<String> usedCodes = new HashSet<>();

        // İlk 3 deneme "kullanımda" döner, 4. unused
        String code = generator.generateUnique(candidate -> {
            int attempt = attempts.incrementAndGet();
            if (attempt <= 3) {
                usedCodes.add(candidate);
                return true; // exists
            }
            return false; // unused
        });

        assertNotNull(code);
        assertFalse(usedCodes.contains(code));
        assertEquals(4, attempts.get());
    }

    @Test
    void generateUnique_throwsAfter10Attempts() {
        assertThrows(IllegalStateException.class,
                () -> generator.generateUnique(c -> true));
    }

    @Test
    void generate_producesDifferentCodesAcrossCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        // 31^6 = ~887M olası kod; 1000 örnekte duplicate ihtimali ihmal edilebilir
        // Toleranslı test: en az 990 farklı kod beklenir
        assertTrue(codes.size() >= 990,
                "Only " + codes.size() + " unique codes out of 1000 — RNG may be broken");
    }
}
