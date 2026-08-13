package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shiftscheduler.server.api.SystemSettingResponse;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.domain.SystemSetting;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.repository.SystemSettingRepository;

@Service
public class SystemSettingService {

  private static final String AUTO_SHIFT_GENERATION_RULES_KEY = "autoShiftGenerationRules";

  @Autowired
  private SystemSettingRepository systemSettingRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private ShiftRequestService shiftRequestService;

  @Autowired
  private AccessControlService accessControlService;

  @Autowired
  private ObjectMapper objectMapper;

  @Transactional
  public SystemSettingResponse updateSystemSettingBoolean(Long updaterStaffId, String settingKey, Boolean value) {
    // Check MASTER privilege
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater)) {
      throw new IllegalArgumentException("マスタのみシステム設定を更新できます。");
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
        .orElse(new SystemSetting());

    if (setting.getSettingKey() == null) {
      setting.setSettingKey(settingKey);
    }

    setting.setSettingValueBoolean(value);
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());

    SystemSetting saved = systemSettingRepository.save(setting);
    return convertToResponse(saved);
  }

  @Transactional
  public SystemSettingResponse updateSystemSettingText(Long updaterStaffId, String settingKey, String value) {
    // Check MASTER privilege
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater)) {
      throw new IllegalArgumentException("マスタのみシステム設定を更新できます。");
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
        .orElse(new SystemSetting());

    if (setting.getSettingKey() == null) {
      setting.setSettingKey(settingKey);
    }

    setting.setSettingValueText(value);
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());

    SystemSetting saved = systemSettingRepository.save(setting);
    return convertToResponse(saved);
  }

  public SystemSettingResponse getSystemSettingByKey(String settingKey) {
    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
        .orElseThrow(() -> new IllegalArgumentException("設定が見つかりません: " + settingKey));
    return convertToResponse(setting);
  }

  public List<SystemSettingResponse> getAllSystemSettings() {
    return systemSettingRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public Boolean getSystemSettingBooleanValue(String settingKey) {
    return systemSettingRepository.findBySettingKey(settingKey)
        .map(SystemSetting::getSettingValueBoolean)
        .orElse(null);
  }

  public String getSystemSettingTextValue(String settingKey) {
    return systemSettingRepository.findBySettingKey(settingKey)
        .map(SystemSetting::getSettingValueText)
        .orElse(null);
  }

  public boolean isMonthConfirmed(String monthKey) {
    if (monthKey == null || !monthKey.matches("\\d{4}-\\d{2}")) {
      return false;
    }

    return parseConfirmedMonths(getSystemSettingTextValue("confirmedShiftMonths")).contains(monthKey);
  }

  public boolean isMonthConfirmed(int year, int month) {
    String monthKey = String.format("%04d-%02d", year, month);
    return isMonthConfirmed(monthKey);
  }

  @Transactional
  public void removeConfirmedMonth(Long updaterStaffId, int year, int month) {
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater) && !accessControlService.isChief(updater)) {
      throw new IllegalArgumentException("管理者のみ確認状態を変更できます。");
    }

    String monthKey = String.format("%04d-%02d", year, month);
    List<String> confirmedMonths = parseConfirmedMonths(getSystemSettingTextValue("confirmedShiftMonths"));
    List<String> updatedMonths = confirmedMonths.stream()
        .filter(existing -> !monthKey.equals(existing))
        .collect(Collectors.toList());

    String newValue = String.join(",", updatedMonths);
    SystemSetting setting = systemSettingRepository.findBySettingKey("confirmedShiftMonths")
        .orElseGet(() -> {
          SystemSetting created = new SystemSetting();
          created.setSettingKey("confirmedShiftMonths");
          return created;
        });

    setting.setSettingValueText(newValue);
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());
    systemSettingRepository.save(setting);
  }

  @Transactional
  public void confirmMonth(Long updaterStaffId, int year, int month) {
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater) && !accessControlService.isChief(updater)) {
      throw new IllegalArgumentException("管理者のみ確定操作を行えます。");
    }

    String monthKey = String.format("%04d-%02d", year, month);
    List<String> confirmedMonths = new ArrayList<>(new LinkedHashSet<>(parseConfirmedMonths(getSystemSettingTextValue("confirmedShiftMonths"))));
    if (!confirmedMonths.contains(monthKey)) {
      confirmedMonths.add(monthKey);
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey("confirmedShiftMonths")
        .orElseGet(() -> {
          SystemSetting created = new SystemSetting();
          created.setSettingKey("confirmedShiftMonths");
          return created;
        });

    setting.setSettingValueText(String.join(",", confirmedMonths));
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());
    systemSettingRepository.save(setting);

    YearMonth yearMonth = YearMonth.of(year, month);
    shiftRequestService.reconcileShiftRequestsForMonth(yearMonth.atDay(1), yearMonth.atEndOfMonth());
  }

  @Transactional
  public void resetAutoShiftRequiredCount(Long shiftTypeId) {
    if (shiftTypeId == null) {
      return;
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey(AUTO_SHIFT_GENERATION_RULES_KEY)
        .orElseGet(() -> {
          SystemSetting created = new SystemSetting();
          created.setSettingKey(AUTO_SHIFT_GENERATION_RULES_KEY);
          return created;
        });

    ObjectNode root = createRulesRoot(setting.getSettingValueText());
    ObjectNode requiredCounts = root.with("requiredCounts");
    requiredCounts.put(String.valueOf(shiftTypeId), 0);
    setting.setSettingValueText(root.toString());
    setting.setUpdatedAt(OffsetDateTime.now());

    systemSettingRepository.save(setting);
  }

  private ObjectNode createRulesRoot(String rawValue) {
    if (rawValue != null && !rawValue.isBlank()) {
      try {
        JsonNode parsed = objectMapper.readTree(rawValue);
        if (parsed instanceof ObjectNode objectNode) {
          return objectNode.deepCopy();
        }
      } catch (Exception ignored) {
        // Fall back to an empty rules object when persisted JSON is malformed.
      }
    }

    return objectMapper.createObjectNode();
  }

  private List<String> parseConfirmedMonths(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return List.of();
    }

    return List.of(rawValue.split("[\\n,]+"))
        .stream()
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(value -> value.replaceAll("\\s+", ""))
        .filter(value -> value.matches("\\d{4}-\\d{2}"))
        .collect(Collectors.toList());
  }

  private SystemSettingResponse convertToResponse(SystemSetting setting) {
    SystemSettingResponse response = new SystemSettingResponse();
    response.setSettingKey(setting.getSettingKey());
    response.setSettingValueBoolean(setting.getSettingValueBoolean());
    response.setSettingValueText(setting.getSettingValueText());
    response.setUpdatedBy(setting.getUpdatedBy() != null ? setting.getUpdatedBy().getStaffName() : null);
    response.setUpdatedAt(setting.getUpdatedAt());
    return response;
  }
}
