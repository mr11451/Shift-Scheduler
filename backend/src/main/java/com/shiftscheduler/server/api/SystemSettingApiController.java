package com.shiftscheduler.server.api;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiftscheduler.server.service.SystemSettingService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingApiController {

  @Autowired
  private SystemSettingService systemSettingService;

  /**
   * GET /api/system-settings/{settingKey} - Retrieve setting by key
   */
  @GetMapping("/{settingKey}")
  public ResponseEntity<SystemSettingResponse> getSystemSettingByKey(@PathVariable String settingKey) {
    try {
      SystemSettingResponse setting = systemSettingService.getSystemSettingByKey(settingKey);
      return ResponseEntity.ok(setting);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * GET /api/system-settings - Retrieve all settings
   */
  @GetMapping
  public ResponseEntity<List<SystemSettingResponse>> getAllSystemSettings() {
    List<SystemSettingResponse> settings = systemSettingService.getAllSystemSettings();
    return ResponseEntity.ok(settings);
  }

  /**
   * GET /api/system-settings/confirmedShiftMonths/status - Determine whether a given month is confirmed
   */
  @GetMapping("/confirmedShiftMonths/status")
  public ResponseEntity<?> getConfirmedMonthStatus(
      @RequestParam int year,
      @RequestParam int month) {
    if (month < 1 || month > 12) {
      return ResponseEntity.badRequest().body("エラー: 月は1〜12の範囲で指定してください。");
    }

    boolean confirmed = systemSettingService.isMonthConfirmed(year, month);
    String monthKey = String.format("%04d-%02d", year, month);
    return ResponseEntity.ok(Map.of(
        "year", year,
        "month", month,
        "monthKey", monthKey,
        "confirmed", confirmed));
  }

  /**
   * GET /api/system-settings/shift-periods/status - Determine whether the period containing a
   * date is confirmed.
   */
  @GetMapping("/shift-periods/status")
  public ResponseEntity<?> getShiftPeriodStatus(@RequestParam String date) {
    try {
      SystemSettingService.ShiftPeriod period = systemSettingService.getShiftPeriod(LocalDate.parse(date));
      return ResponseEntity.ok(Map.of(
          "startDate", period.startDate().toString(),
          "endDate", period.endDate().toString(),
          "periodKey", period.key(),
          "closingDay", systemSettingService.getClosingDay(),
          "confirmed", systemSettingService.isShiftPeriodConfirmed(LocalDate.parse(date))));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: 日付は yyyy-MM-dd 形式で指定してください。");
    }
  }

  /**
   * POST /api/system-settings/confirmedShiftMonths/confirm - Confirm a month and reconcile
   * submitted shift requests for that month (APPLIED if matching the confirmed assignment,
   * REJECTED otherwise).
   * Updater staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/confirmedShiftMonths/confirm")
  public ResponseEntity<?> confirmShiftMonth(
      @RequestParam int year,
      @RequestParam int month,
      HttpServletRequest httpRequest) {
    if (month < 1 || month > 12) {
      return ResponseEntity.badRequest().body("エラー: 月は1〜12の範囲で指定してください。");
    }

    try {
      Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
      if (updaterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      systemSettingService.confirmMonth(updaterStaffId, year, month);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/system-settings/shift-periods/confirm - Confirm the period containing a date.
   */
  @PostMapping("/shift-periods/confirm")
  public ResponseEntity<?> confirmShiftPeriod(@RequestParam String date, HttpServletRequest httpRequest) {
    try {
      Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
      if (updaterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      systemSettingService.confirmShiftPeriod(updaterStaffId, LocalDate.parse(date));
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/system-settings/{settingKey}/boolean - Update boolean setting
   * Updater staff ID is resolved from JWT-authenticated request context.
   */
  @PutMapping("/{settingKey}/boolean")
  public ResponseEntity<?> updateSystemSettingBoolean(
      @PathVariable String settingKey,
      @RequestParam Boolean value,
      HttpServletRequest httpRequest) {
    try {
      Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
      if (updaterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      SystemSettingResponse setting = systemSettingService.updateSystemSettingBoolean(updaterStaffId, settingKey, value);
      return ResponseEntity.ok(setting);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/system-settings/{settingKey}/text - Update text setting
   * Updater staff ID is resolved from JWT-authenticated request context.
   */
  @PutMapping("/{settingKey}/text")
  public ResponseEntity<?> updateSystemSettingText(
      @PathVariable String settingKey,
      @RequestParam String value,
      HttpServletRequest httpRequest) {
    try {
      Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
      if (updaterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      SystemSettingResponse setting = systemSettingService.updateSystemSettingText(updaterStaffId, settingKey, value);
      return ResponseEntity.ok(setting);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  private Long getAuthenticatedStaffId(HttpServletRequest httpRequest) {
    Object staffId = httpRequest.getAttribute("staffId");
    if (staffId instanceof Number number) {
      return number.longValue();
    }
    return null;
  }
}
