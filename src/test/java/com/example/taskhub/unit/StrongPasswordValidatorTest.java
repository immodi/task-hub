package com.example.taskhub.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.taskhub.security.StrongPasswordValidator;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Password1", "abcdefg1", "12345678a", "correct horse 9 battery"})
    void acceptsLongEnoughPasswordsWithLetterAndDigit(String pw) {
        assertThat(validator.isValid(pw, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "onlyletters", "12345678", "        "})
    void rejectsWeakPasswords(String pw) {
        assertThat(validator.isValid(pw, null)).isFalse();
    }

    @Test
    void nullIsLeftToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
