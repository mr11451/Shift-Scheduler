package com.shiftscheduler.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP-backed implementation, auto-selected as primary when spring.mail.host is configured.
 */
@Service
@Primary
@ConditionalOnExpression("'${spring.mail.host:}'.trim().length() > 0")
public class SmtpPasswordResetEmailService implements PasswordResetEmailService {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetEmailService(JavaMailSender mailSender, @Value("${spring.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void send(String to, String staffName, String accessUrl, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("シフト管理システム パスワード変更のご案内");
        message.setText(String.format("%s 様%n%n以下のURLからパスワードを変更してください。%n%s%n%n確認コード: %s%n%nこのURLと確認コードの有効期限は1時間です。心当たりがない場合は、このメールを破棄してください。", staffName, accessUrl, verificationCode));
        mailSender.send(message);
    }

    @Override
    public void sendInitialLogin(String to, String staffName, String accessUrl, String loginCode, String initialPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("シフト管理システム 初回ログイン情報のご案内");
        message.setText(String.format("%s 様%n%n以下の情報でログインしてください。%n%nアクセスURL: %s%nログインコード: %s%n初期パスワード: %s%n%n初回ログイン後はパスワードの変更をおすすめします。", staffName, accessUrl, loginCode, initialPassword));
        mailSender.send(message);
    }
}