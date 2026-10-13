package com.example.taskhub.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // let @NotBlank deal with nulls
        }
        return value.length() >= 8
                && value.chars().anyMatch(Character::isLetter)
                && value.chars().anyMatch(Character::isDigit);
    }
}
