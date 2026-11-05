package com.apexmotors.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.length() >= 12 && value.length() <= 64
            && value.getBytes(StandardCharsets.UTF_8).length <= 72 && value.indexOf('\0') < 0
            && value.codePoints().anyMatch(Character::isUpperCase)
            && value.codePoints().anyMatch(Character::isLowerCase)
            && value.codePoints().anyMatch(Character::isDigit);
    }
}
