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
import com.shiftscheduler.server.service.ActiveLoginException;

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
        } catch (ActiveLoginException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        try {
            authenticationService.logout((String) request.getAttribute("authToken"));
        } catch (RuntimeException ignored) {
            // The client clears its local session even if the server session is already invalid.
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/password-reset-requests - Issue a password reset link/code for the authenticated staff
     */
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

    /**
     * POST /api/password-resets/{staffId}/{token} - Confirm a password reset with the emailed token/code
     */
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
