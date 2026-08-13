package com.shiftscheduler.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank(message = "確認コードは必須です。") String verificationCode,
    @NotBlank(message = "新しいパスワードは必須です。") @Size(min = 8, message = "新しいパスワードは8文字以上で入力してください。") String newPassword
) {}