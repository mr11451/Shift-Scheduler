package com.shiftscheduler.server.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.dto.PasswordResetConfirmRequest;
import com.shiftscheduler.server.dto.PasswordResetRequestResponse;
import com.shiftscheduler.server.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthenticationApiController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    /**
     * POST /api/login - Authenticate staff with staff code and password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authenticationService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: " + e.getMessage());
        }
    }

    @PostMapping("/password-reset-requests")
    public ResponseEntity<?> requestPasswordReset(HttpServletRequest request) {
        try {
            Long staffId = (Long) request.getAttribute("staffId");
            PasswordResetRequestResponse response = authenticationService.requestPasswordReset(staffId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
        }
    }

    @PostMapping("/password-resets/{staffId}/{token}")
    public ResponseEntity<?> confirmPasswordReset(
            @org.springframework.web.bind.annotation.PathVariable Long staffId,
            @org.springframework.web.bind.annotation.PathVariable String token,
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            authenticationService.resetPassword(staffId, token, request.verificationCode(), request.newPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
        }
    }
}
