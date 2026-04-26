package com.beratcan.app.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void validPassword_meetsAllRules_returnsTrue() {
        assertTrue(validator.isValid("Abcdef1!", null));
        assertTrue(validator.isValid("Test123$", null));
        assertTrue(validator.isValid("Strong#Pwd9", null));
    }

    @Test
    void shortPassword_under8Chars_returnsFalse() {
        assertFalse(validator.isValid("Ab1!", null));
        assertFalse(validator.isValid("Abc1!", null));
        assertFalse(validator.isValid("Abcde1!", null));
    }

    @Test
    void noUppercase_returnsFalse() {
        assertFalse(validator.isValid("abcdef1!", null));
    }

    @Test
    void noLowercase_returnsFalse() {
        assertFalse(validator.isValid("ABCDEF1!", null));
    }

    @Test
    void noDigit_returnsFalse() {
        assertFalse(validator.isValid("Abcdefg!", null));
    }

    @Test
    void noSpecialChar_returnsFalse() {
        assertFalse(validator.isValid("Abcdefg1", null));
    }

    @Test
    void exactly100Chars_isAccepted() {
        String pwd = "Abcdef1!" + "a".repeat(92); // 100 char total
        assertEquals(100, pwd.length());
        assertTrue(validator.isValid(pwd, null));
    }

    @Test
    void over100Chars_returnsFalse() {
        String pwd = "Abcdef1!" + "a".repeat(93); // 101 char total
        assertFalse(validator.isValid(pwd, null));
    }

    @Test
    void nullValue_returnsTrue_letsNotBlankHandleIt() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void emptyString_returnsFalse_doesNotMatchPattern() {
        assertFalse(validator.isValid("", null));
    }
}
