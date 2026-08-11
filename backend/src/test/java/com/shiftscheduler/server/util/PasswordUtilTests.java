package com.shiftscheduler.server.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordUtilTests {

    @Test
    void hashPassword_andVerifyPassword_roundTrip() {
        String hash = PasswordUtil.hashPassword("secret-password");

        assertThat(hash).isNotBlank();
        assertThat(PasswordUtil.verifyPassword("secret-password", hash)).isTrue();
        assertThat(PasswordUtil.verifyPassword("different-password", hash)).isFalse();
    }

    @Test
    void verifyPassword_returnsFalseForInvalidHash() {
        assertThat(PasswordUtil.verifyPassword("secret-password", "not-base64")).isFalse();
    }
}
