package com.shiftscheduler.server.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtTokenUtilTests {

    @Test
    void generateTokenAndReadClaims() {
        String token = JwtTokenUtil.generateToken(123L, "STF-00001", "MASTER");

        Claims claims = JwtTokenUtil.validateToken(token);

        assertThat(claims.get("staffId", Long.class)).isEqualTo(123L);
        assertThat(claims.get("staffCode", String.class)).isEqualTo("STF-00001");
        assertThat(claims.get("roleLevel", String.class)).isEqualTo("MASTER");
        assertThat(JwtTokenUtil.getStaffIdFromToken(token)).isEqualTo(123L);
        assertThat(JwtTokenUtil.getStaffCodeFromToken(token)).isEqualTo("STF-00001");
        assertThat(JwtTokenUtil.getRoleLevelFromToken(token)).isEqualTo("MASTER");
    }

    @Test
    void validateToken_rejectsBrokenToken() {
        assertThatThrownBy(() -> JwtTokenUtil.validateToken("invalid-token"))
                .isInstanceOf(RuntimeException.class);
    }
}
