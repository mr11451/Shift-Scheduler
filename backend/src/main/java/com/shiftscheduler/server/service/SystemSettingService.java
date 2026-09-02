package com.shiftscheduler.server.service;

import java.time.LocalDate;
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
  private static final String CLOSING_DAY_KEY = "closingDay";
  private static final String CONFIRMED_SHIFT_PERIODS_KEY = "confirmedShiftPeriods";

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

  /**
   * Update a boolean-valued system setting; MASTER only.
   */
  @Transactional
  public SystemSettingResponse updateSystemSettingBoolean(Long updaterStaffId, String settingKey, Boolean value) {
    // Check MASTER privilege
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater)) {
      throw new IllegalArgumentException("マスターのみシステム設定を更新できます。");
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

  /**
   * Update a text-valued system setting. MASTER may update any key; CHIEF may only update
   * their own group's entry within autoShiftGenerationRules.
   */
  @Transactional
  public SystemSettingResponse updateSystemSettingText(Long updaterStaffId, String settingKey, String value) {
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    String valueToSave = value;
    if (CLOSING_DAY_KEY.equals(settingKey)) {
      try {
        int closingDay = Integer.parseInt(value == null ? "" : value.trim());
        if (closingDay < 1 || closingDay > 31) {
          throw new NumberFormatException();
        }
        valueToSave = String.valueOf(closingDay);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("締め日は1〜31の範囲で指定してください。");
      }
    }

    if (!accessControlService.isMaster(updater)) {
      // CHIEF may only update their own group's auto-generation rule; every other setting stays MASTER-only.
      if (!AUTO_SHIFT_GENERATION_RULES_KEY.equals(settingKey) || !accessControlService.isChief(updater)) {
        throw new IllegalArgumentException("マスターのみシステム設定を更新できます。");
      }

      valueToSave = mergeGroupRuleForChief(updater, value);
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
        .orElse(new SystemSetting());

    if (setting.getSettingKey() == null) {
      setting.setSettingKey(settingKey);
    }

    setting.setSettingValueText(valueToSave);
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());

    SystemSetting saved = systemSettingRepository.save(setting);
    return convertToResponse(saved);
  }

  /**
   * Rebuild the full autoShiftGenerationRules JSON from the persisted value, replacing only the
   * requesting CHIEF's own group entry. This prevents a CHIEF payload from altering the default
   * rules or any other group's rules, even if the submitted JSON contains such changes.
   */
  private String mergeGroupRuleForChief(Staff updater, String incomingJson) {
    Long groupId = updater.getGroup() != null ? updater.getGroup().getId() : null;
    if (groupId == null) {
      throw new IllegalArgumentException("グループに所属していないため自動生成ルールを更新できません。");
    }

    JsonNode incomingGroupRule;
    try {
      JsonNode incoming = objectMapper.readTree(incomingJson);
      JsonNode incomingGroupRules = incoming.path("groupRules");
      incomingGroupRule = incomingGroupRules.get(String.valueOf(groupId));
    } catch (Exception e) {
      throw new IllegalArgumentException("自動生成ルールの形式が不正です。");
    }

    if (incomingGroupRule == null || incomingGroupRule.isMissingNode()) {
      throw new IllegalArgumentException("自分のグループのルールが指定されていません。");
    }

    ObjectNode root = createRulesRoot(getSystemSettingTextValue(AUTO_SHIFT_GENERATION_RULES_KEY));
    if (!root.has("defaultRules")) {
      root.set("defaultRules", objectMapper.createObjectNode());
    }
    ObjectNode groupRulesNode = root.with("groupRules");
    groupRulesNode.set(String.valueOf(groupId), incomingGroupRule);

    return root.toString();
  }

  /**
   * Look up a single system setting by key.
   */
  public SystemSettingResponse getSystemSettingByKey(String settingKey) {
    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
        .orElseThrow(() -> new IllegalArgumentException("設定が見つかりません: " + settingKey));
    return convertToResponse(setting);
  }

  /**
   * List every system setting.
   */
  public List<SystemSettingResponse> getAllSystemSettings() {
    return systemSettingRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * Read a boolean setting value, or null if unset.
   */
  public Boolean getSystemSettingBooleanValue(String settingKey) {
    return systemSettingRepository.findBySettingKey(settingKey)
        .map(SystemSetting::getSettingValueBoolean)
        .orElse(null);
  }

  /**
   * Read a text setting value, or null if unset.
   */
  public String getSystemSettingTextValue(String settingKey) {
    return systemSettingRepository.findBySettingKey(settingKey)
        .map(SystemSetting::getSettingValueText)
        .orElse(null);
  }

  /**
   * Return the configured closing day. The last day of each month is used when the setting has
   * not been created yet, preserving the existing calendar-month behavior.
   */
  public int getClosingDay() {
    String value = getSystemSettingTextValue(CLOSING_DAY_KEY);
    try {
      int closingDay = Integer.parseInt(value == null ? "31" : value.trim());
      return closingDay >= 1 && closingDay <= 31 ? closingDay : 31;
    } catch (NumberFormatException e) {
      return 31;
    }
  }

  /**
   * Resolve the shift period that contains a date. A period ends on the configured closing day,
   * or the last valid day when a month is shorter than that day.
   */
  public ShiftPeriod getShiftPeriod(LocalDate date) {
    LocalDate effectiveDate = date == null ? LocalDate.now() : date;
    int closingDay = getClosingDay();
    LocalDate thisMonthClosingDate = effectiveDate.withDayOfMonth(
        Math.min(closingDay, effectiveDate.lengthOfMonth()));
    LocalDate endDate = effectiveDate.isAfter(thisMonthClosingDate)
        ? effectiveDate.plusMonths(1).withDayOfMonth(Math.min(closingDay, effectiveDate.plusMonths(1).lengthOfMonth()))
        : thisMonthClosingDate;
    LocalDate previousMonth = endDate.minusMonths(1);
    LocalDate startDate = previousMonth.withDayOfMonth(Math.min(closingDay, previousMonth.lengthOfMonth())).plusDays(1);
    return new ShiftPeriod(startDate, endDate);
  }

  /**
   * Check whether the period containing the date has been confirmed.
   */
  public boolean isShiftPeriodConfirmed(LocalDate date) {
    ShiftPeriod period = getShiftPeriod(date);
    if (parseConfirmedPeriods(getSystemSettingTextValue(CONFIRMED_SHIFT_PERIODS_KEY)).contains(period.key())) {
      return true;
    }

    // Existing installations stored month keys. They remain valid while the closing day is month-end.
    return getClosingDay() == 31 && isMonthConfirmed(period.endDate().getYear(), period.endDate().getMonthValue());
  }

  /**
   * Check whether the given "YYYY-MM" month key is in the confirmed months list.
   */
  public boolean isMonthConfirmed(String monthKey) {
    if (monthKey == null || !monthKey.matches("\\d{4}-\\d{2}")) {
      return false;
    }

    return parseConfirmedMonths(getSystemSettingTextValue("confirmedShiftMonths")).contains(monthKey);
  }

  /**
   * Check whether the given year/month is confirmed.
   */
  public boolean isMonthConfirmed(int year, int month) {
    String monthKey = String.format("%04d-%02d", year, month);
    return isMonthConfirmed(monthKey);
  }

  /**
   * Remove a month from the confirmed months list; MASTER/CHIEF only.
   */
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

  /** Remove a period confirmation; MASTER/CHIEF only. */
  @Transactional
  public void removeConfirmedShiftPeriod(Long updaterStaffId, LocalDate date) {
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));
    if (!accessControlService.isMaster(updater) && !accessControlService.isChief(updater)) {
      throw new IllegalArgumentException("管理者のみ確認状態を変更できます。");
    }

    String periodKey = getShiftPeriod(date).key();
    List<String> periods = parseConfirmedPeriods(getSystemSettingTextValue(CONFIRMED_SHIFT_PERIODS_KEY)).stream()
        .filter(existing -> !periodKey.equals(existing))
        .collect(Collectors.toList());
    SystemSetting setting = systemSettingRepository.findBySettingKey(CONFIRMED_SHIFT_PERIODS_KEY)
        .orElseGet(() -> {
          SystemSetting created = new SystemSetting();
          created.setSettingKey(CONFIRMED_SHIFT_PERIODS_KEY);
          return created;
        });
    setting.setSettingValueText(String.join(",", periods));
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());
    systemSettingRepository.save(setting);
  }

  /**
   * Confirm a month, locking its assignments and reconciling submitted shift requests
   * against the confirmed schedule; MASTER/CHIEF only.
   */
  @Transactional
  public void confirmMonth(Long updaterStaffId, int year, int month) {
    confirmShiftPeriod(updaterStaffId, LocalDate.of(year, month, 1));
  }

  /**
   * Confirm the period containing the given date and reconcile submitted shift requests in it.
   */
  @Transactional
  public void confirmShiftPeriod(Long updaterStaffId, LocalDate date) {
    Staff updater = staffRepository.findById(updaterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (!accessControlService.isMaster(updater) && !accessControlService.isChief(updater)) {
      throw new IllegalArgumentException("管理者のみ確定操作を行えます。");
    }

    ShiftPeriod period = getShiftPeriod(date);
    List<String> confirmedPeriods = new ArrayList<>(new LinkedHashSet<>(parseConfirmedPeriods(getSystemSettingTextValue(CONFIRMED_SHIFT_PERIODS_KEY))));
    if (!confirmedPeriods.contains(period.key())) {
      confirmedPeriods.add(period.key());
    }

    SystemSetting setting = systemSettingRepository.findBySettingKey(CONFIRMED_SHIFT_PERIODS_KEY)
        .orElseGet(() -> {
          SystemSetting created = new SystemSetting();
          created.setSettingKey(CONFIRMED_SHIFT_PERIODS_KEY);
          return created;
        });

    setting.setSettingValueText(String.join(",", confirmedPeriods));
    setting.setUpdatedBy(updater);
    setting.setUpdatedAt(OffsetDateTime.now());
    systemSettingRepository.save(setting);

    shiftRequestService.reconcileShiftRequestsForMonth(period.startDate(), period.endDate());
  }

  /**
   * Reset the stored required-headcount for a shift type back to zero (e.g. after deletion).
   */
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

  /**
   * Parse the persisted autoShiftGenerationRules JSON into a mutable node, defaulting to
   * an empty object if unset or malformed.
   */
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

  /**
   * Parse the comma/newline separated confirmedShiftMonths setting into valid "YYYY-MM" keys.
   */
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

  private List<String> parseConfirmedPeriods(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return List.of();
    }

    return List.of(rawValue.split("[\\n,]+"))
        .stream()
        .map(String::trim)
        .filter(value -> value.matches("\\d{4}-\\d{2}-\\d{2}"))
        .collect(Collectors.toList());
  }

  public record ShiftPeriod(LocalDate startDate, LocalDate endDate) {
    public String key() {
      return endDate.toString();
    }
  }

  /**
   * Map the entity to its API response shape.
   */
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
