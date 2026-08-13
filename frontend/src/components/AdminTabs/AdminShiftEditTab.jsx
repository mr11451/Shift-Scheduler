import React, { useState, useEffect, useContext, useMemo } from "react";
import { fetchWithAuth } from "../../utils/fetchWithAuth";
import { AuthContext } from "../../context/AuthContext";
import { isHolidayDate, parseHolidayDates, parseHolidayWeekdays } from "../../utils/holidayDates";
import "./AdminShiftEditTab.css";

const REQUEST_STATUS_SHORT_LABELS = {
  DRAFT: "下書",
  SUBMITTED: "申請",
  APPLIED: "承認",
  REJECTED: "却下",
};

const WEEKDAY_LABELS = ["日", "月", "火", "水", "木", "金", "土"];

export default function AdminShiftEditTab() {
  const { auth } = useContext(AuthContext);
  const isAdminRole = auth?.roleLevel === "MASTER" || auth?.roleLevel === "CHIEF";
  const [currentDate, setCurrentDate] = useState(() => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
  });
  const [staffsByGroup, setStaffsByGroup] = useState([]);
  const [shiftTypes, setShiftTypes] = useState([]);
  const [shiftAssignments, setShiftAssignments] = useState({}); // key: staffId-date, value: shiftTypeId
  const [shiftRequests, setShiftRequests] = useState({}); // key: staffId-date, value: request
  const [holidayDates, setHolidayDates] = useState([]);
  const [holidayWeekdays, setHolidayWeekdays] = useState([]);
  const [requiredCounts, setRequiredCounts] = useState({});
  const [requiredCountsByGroup, setRequiredCountsByGroup] = useState({});
  const [maxConsecutiveWorkdays, setMaxConsecutiveWorkdays] = useState(0);
  const [minimumShiftGapHours, setMinimumShiftGapHours] = useState(0);
  const [isCurrentMonthConfirmed, setIsCurrentMonthConfirmed] = useState(false);
  const [confirmingMonth, setConfirmingMonth] = useState(false);
  const [autoGenerating, setAutoGenerating] = useState(false);
  const [clearingMonth, setClearingMonth] = useState(false);
  const [activeCellKey, setActiveCellKey] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");

  useEffect(() => {
    loadData();
  }, [currentDate, auth?.token, auth?.roleLevel]);

  function toLocalDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function toLocalDateStringByDay(year, monthIndex, day) {
    const month = String(monthIndex + 1).padStart(2, "0");
    const dayString = String(day).padStart(2, "0");
    return `${year}-${month}-${dayString}`;
  }

  async function loadData(preserveMessage = false) {
    try {
      setLoading(true);
      if (!preserveMessage) {
        setMessage("");
        setMessageType("");
      }
      let loadedStaffs = [];

      await loadHolidayDates();
      await loadCurrentMonthConfirmationStatus(currentDate);

      const stRes = await fetchWithAuth("/api/shift-types?active=true", { redirectOnUnauthorized: false });
      if (stRes.ok) {
        setShiftTypes(await stRes.json());
      }

      const sRes = await fetchWithAuth("/api/staffs", { redirectOnUnauthorized: false });
      if (sRes.ok) {
        loadedStaffs = await sRes.json();
        groupStaffs(loadedStaffs);
      } else {
        groupStaffs([]);
      }

      const firstDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
      const lastDay = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
      const startDate = toLocalDateString(firstDay);
      const endDate = toLocalDateString(lastDay);

      const saRes = await fetchWithAuth(`/api/shift-assignments?startDate=${startDate}&endDate=${endDate}`, {
        redirectOnUnauthorized: false,
      });
      if (saRes.ok) {
        const assignments = await saRes.json();
        const assignmentMap = {};
        assignments.forEach((a) => {
          const key = `${a.staffId}-${a.workDate}`;
          assignmentMap[key] = a;
        });
        setShiftAssignments(assignmentMap);
      } else {
        setShiftAssignments({});
      }

      if (loadedStaffs.length > 0) {
        const requestMap = await loadShiftRequestsForMonth(startDate, endDate, loadedStaffs);
        setShiftRequests(requestMap);
      } else {
        setShiftRequests({});
      }
    } catch (e) {
      setShiftAssignments({});
      setShiftRequests({});
      setStaffsByGroup([]);
      setShiftTypes([]);
      setMessage(e.message || "シフト編集画面の読み込みに失敗しました。\nログイン状態やAPI接続を確認してください。")
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  function groupStaffs(staffs) {
    const groups = {};
    staffs.forEach((staff) => {
      const groupName = (staff.groupName && String(staff.groupName).trim())
        || (staff.groupId ? `グループ ${staff.groupId}` : "未分類");
      if (!groups[groupName]) groups[groupName] = [];
      groups[groupName].push(staff);
    });

    // Sort groups and staff within groups
    const sorted = Object.entries(groups)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([group, staffList]) => ({
        groupName: group,
        staffs: staffList.sort((a, b) => {
          if (a.staffCode !== b.staffCode) return a.staffCode.localeCompare(b.staffCode);
          return a.staffName.localeCompare(b.staffName);
        }),
      }));

    setStaffsByGroup(sorted);
  }

  function getDaysInMonth(date) {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  }

  function getWeekday(day) {
    return new Date(currentDate.getFullYear(), currentDate.getMonth(), day).getDay();
  }

  function getWeekdayLabel(day) {
    const weekDay = getWeekday(day);
    return WEEKDAY_LABELS[weekDay];
  }

  function getWeekendColumnStyle(day) {
    const weekDay = getWeekday(day);

    if (weekDay === 0) {
      return {
        backgroundColor: "#fff1f0",
        color: "#111827",
      };
    }

    if (weekDay === 6) {
      return {
        backgroundColor: "#eff6ff",
        color: "#111827",
      };
    }

    return {
      backgroundColor: "#ffffff",
      color: "inherit",
    };
  }

  async function loadHolidayDates() {
    try {
      const res = await fetchWithAuth("/api/system-settings", { redirectOnUnauthorized: false });
      if (!res.ok) {
        setHolidayDates([]);
        return;
      }
      const settings = await res.json();
      const holidaySetting = Array.isArray(settings) ? settings.find((item) => item.settingKey === "holidayDates") : null;
      const holidayWeekdaySetting = Array.isArray(settings) ? settings.find((item) => item.settingKey === "holidayWeekdays") : null;
      const autoShiftRuleSetting = Array.isArray(settings)
        ? settings.find((item) => item.settingKey === "autoShiftGenerationRules")
        : null;
      const rawValue = holidaySetting?.settingValueText || "";
      const rawWeekdays = holidayWeekdaySetting?.settingValueText || "";
      setHolidayDates(parseHolidayDates(rawValue));
      setHolidayWeekdays(parseHolidayWeekdays(rawWeekdays));

      let parsedGapHours = 0;
      let parsedMaxConsecutiveWorkdays = 0;
      let parsedRequiredCounts = {};
      let parsedRequiredCountsByGroup = {};
      try {
        const parsedRoot = JSON.parse(autoShiftRuleSetting?.settingValueText || "{}");
        const hasGroupedRules = parsedRoot && typeof parsedRoot === "object" && (parsedRoot.defaultRules || parsedRoot.groupRules);
        const parsedRules = hasGroupedRules ? (parsedRoot.defaultRules || {}) : parsedRoot;
        const ruleGap = Number(parsedRules?.minimumShiftGapHours ?? 0);
        const ruleMaxConsecutive = Number(parsedRules?.maxConsecutiveWorkdays ?? 0);
        parsedRequiredCounts = parsedRules?.requiredCounts && typeof parsedRules.requiredCounts === "object" ? parsedRules.requiredCounts : {};
        if (hasGroupedRules && parsedRoot.groupRules && typeof parsedRoot.groupRules === "object") {
          Object.entries(parsedRoot.groupRules).forEach(([groupId, groupRule]) => {
            const groupRequiredCounts = groupRule?.requiredCounts;
            if (groupRequiredCounts && typeof groupRequiredCounts === "object") {
              parsedRequiredCountsByGroup[groupId] = groupRequiredCounts;
            }
          });
        }
        parsedGapHours = Number.isFinite(ruleGap) ? Math.max(0, ruleGap) : 0;
        parsedMaxConsecutiveWorkdays = Number.isFinite(ruleMaxConsecutive) ? Math.max(0, ruleMaxConsecutive) : 0;
      } catch (error) {
        parsedGapHours = 0;
        parsedMaxConsecutiveWorkdays = 0;
        parsedRequiredCounts = {};
        parsedRequiredCountsByGroup = {};
      }
      setMinimumShiftGapHours(parsedGapHours);
      setMaxConsecutiveWorkdays(parsedMaxConsecutiveWorkdays);
      setRequiredCounts(parsedRequiredCounts);
      setRequiredCountsByGroup(parsedRequiredCountsByGroup);
    } catch (e) {
      setHolidayDates([]);
      setHolidayWeekdays([]);
      setRequiredCounts({});
      setRequiredCountsByGroup({});
      setMinimumShiftGapHours(0);
      setMaxConsecutiveWorkdays(0);
    }
  }

  const staffGroupKeyByStaffId = useMemo(() => {
    const groupMap = {};
    staffsByGroup.forEach((group) => {
      group.staffs.forEach((staff) => {
        const key = String(staff.id);
        groupMap[key] = staff.groupId != null ? String(staff.groupId) : "__ungrouped__";
      });
    });
    return groupMap;
  }, [staffsByGroup]);

  const dailyShiftCountsByGroup = useMemo(() => {
    const counts = {};

    Object.values(shiftAssignments).forEach((assignment) => {
      if (!assignment?.workDate || assignment.shiftTypeId == null) {
        return;
      }

      const groupKey = staffGroupKeyByStaffId[String(assignment.staffId)] || "__ungrouped__";
      const dateKey = assignment.workDate;
      const shiftTypeId = String(assignment.shiftTypeId);
      if (!counts[groupKey]) {
        counts[groupKey] = {};
      }
      if (!counts[groupKey][dateKey]) {
        counts[groupKey][dateKey] = {};
      }
      counts[groupKey][dateKey][shiftTypeId] = (counts[groupKey][dateKey][shiftTypeId] || 0) + 1;
    });

    return counts;
  }, [shiftAssignments, staffGroupKeyByStaffId]);

  function getRequiredCountsForStaff(staffId) {
    const groupKey = staffGroupKeyByStaffId[String(staffId)] || "__ungrouped__";
    const groupRequired = requiredCountsByGroup[groupKey];
    if (groupRequired && typeof groupRequired === "object") {
      return groupRequired;
    }
    return requiredCounts;
  }

  function isRequiredCountShortageDateForStaff(dateStr, staffId) {
    const groupKey = staffGroupKeyByStaffId[String(staffId)] || "__ungrouped__";
    const countsByShiftType = dailyShiftCountsByGroup?.[groupKey]?.[dateStr] || {};
    const targetRequiredCounts = getRequiredCountsForStaff(staffId);

    return Object.entries(targetRequiredCounts).some(([shiftTypeId, requiredCountValue]) => {
      const requiredCount = Number(requiredCountValue);
      if (!Number.isFinite(requiredCount) || requiredCount <= 0) {
        return false;
      }

      const actualCount = Number(countsByShiftType[String(shiftTypeId)] ?? 0);
      return actualCount < requiredCount;
    });
  }

  async function loadCurrentMonthConfirmationStatus(date) {
    try {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const res = await fetchWithAuth(
        `/api/system-settings/confirmedShiftMonths/status?year=${year}&month=${month}`,
        { redirectOnUnauthorized: false }
      );

      if (!res.ok) {
        setIsCurrentMonthConfirmed(false);
        return;
      }

      const status = await res.json();
      setIsCurrentMonthConfirmed(Boolean(status?.confirmed));
    } catch (e) {
      setIsCurrentMonthConfirmed(false);
    }
  }

  function previousMonth() {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  }

  function nextMonth() {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  }

  function getShiftName(staffId, day) {
    const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
    const key = `${staffId}-${dateStr}`;
    const assignment = shiftAssignments[key];
    if (!assignment) return "";

    const shiftType = shiftTypes.find((st) => st.id === assignment.shiftTypeId);
    if (!shiftType) return "";

    return shiftType.isOffType ? "" : shiftType.shiftName;
  }

  function getShiftRequest(staffId, day) {
    const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
    return shiftRequests[`${staffId}-${dateStr}`] || null;
  }

  function parseShiftTypeIds(value) {
    if (!value) {
      return [];
    }

    if (Array.isArray(value)) {
      const parsedIds = [];
      value.forEach((item) => {
        if (typeof item === "number") {
          if (Number.isFinite(item)) {
            parsedIds.push(item);
          }
          return;
        }

        if (item && typeof item === "object") {
          const shiftTypeId = Number(item.shiftTypeId);
          if (Number.isFinite(shiftTypeId)) {
            parsedIds.push(shiftTypeId);
          }
          return;
        }

        const id = Number(item);
        if (Number.isFinite(id)) {
          parsedIds.push(id);
        }
      });
      return parsedIds;
    }

    if (typeof value === "string") {
      const trimmed = value.trim();
      if (!trimmed) {
        return [];
      }

      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          const parsedIds = [];
          parsed.forEach((item) => {
            if (typeof item === "number") {
              if (Number.isFinite(item)) {
                parsedIds.push(item);
              }
              return;
            }

            if (item && typeof item === "object") {
              const shiftTypeId = Number(item.shiftTypeId);
              if (Number.isFinite(shiftTypeId)) {
                parsedIds.push(shiftTypeId);
              }
              return;
            }

            const id = Number(item);
            if (Number.isFinite(id)) {
              parsedIds.push(id);
            }
          });
          return parsedIds;
        }
        if (parsed && Array.isArray(parsed.shiftTypeIds)) {
          return parsed.shiftTypeIds.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
        if (parsed && Array.isArray(parsed.ngShiftTypeIds)) {
          return parsed.ngShiftTypeIds.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
        if (parsed && Array.isArray(parsed.blockedShiftTypeIds)) {
          return parsed.blockedShiftTypeIds.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
        if (parsed && Number.isFinite(Number(parsed.shiftTypeId))) {
          return [Number(parsed.shiftTypeId)];
        }
      } catch (error) {
        // Try legacy plain-text formats like "1,2,3".
      }

      const legacyIds = trimmed
        .split(/[\r\n,;]+/)
        .map((item) => Number(item.trim()))
        .filter((item) => Number.isFinite(item));
      if (legacyIds.length) {
        return legacyIds;
      }
    }

    return [];
  }

  function parseWeekdayIds(value) {
    if (!value) {
      return [];
    }

    if (Array.isArray(value)) {
      const weekdays = [];
      value.forEach((item) => {
        if (typeof item === "number") {
          if (Number.isInteger(item) && item >= 0 && item <= 6) {
            weekdays.push(item);
          }
          return;
        }

        if (item && typeof item === "object") {
          const weekday = Number(item.weekdayId ?? item.weekday);
          if (Number.isInteger(weekday) && weekday >= 0 && weekday <= 6) {
            weekdays.push(weekday);
          }
        }
      });
      return weekdays;
    }

    if (typeof value !== "string") {
      return [];
    }

    const trimmed = value.trim();
    if (!trimmed) {
      return [];
    }

    try {
      const parsed = JSON.parse(trimmed);
      if (parsed && Array.isArray(parsed.weekdayIds)) {
        return parsed.weekdayIds
          .map((item) => Number(item))
          .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
      }

      if (parsed && Array.isArray(parsed.ngShiftWeekdayIds)) {
        return parsed.ngShiftWeekdayIds
          .map((item) => Number(item))
          .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
      }

      if (parsed && Array.isArray(parsed.weekdays)) {
        return parsed.weekdays
          .map((item) => Number(item))
          .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
      }

      if (parsed && Array.isArray(parsed.weekDayIds)) {
        return parsed.weekDayIds
          .map((item) => Number(item))
          .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
      }

      if (parsed && Number.isInteger(Number(parsed.weekdayId))) {
        const weekday = Number(parsed.weekdayId);
        return weekday >= 0 && weekday <= 6 ? [weekday] : [];
      }

      if (parsed && Number.isInteger(Number(parsed.weekDayId))) {
        const weekday = Number(parsed.weekDayId);
        return weekday >= 0 && weekday <= 6 ? [weekday] : [];
      }

      if (Array.isArray(parsed)) {
        return parsed
          .map((item) => {
            if (typeof item === "number") {
              return Number(item);
            }
            if (item && typeof item === "object") {
              return Number(item.weekdayId ?? item.weekday);
            }
            return Number.NaN;
          })
          .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
      }
    } catch (error) {
      // Try legacy plain-text formats like "1,2,3".
    }

    return trimmed
      .split(/[\r\n,;]+/)
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
  }

  function buildNgCondition(staff) {
    const typeIds = [
      ...parseShiftTypeIds(staff?.ngShiftTypeIds),
    ];

    const weekdayIds = [
      ...parseWeekdayIds(staff?.ngShiftWeekdayIds),
      ...parseWeekdayIds(staff?.ngShiftTypeIds),
    ];

    return {
      shiftTypeIds: [...new Set(typeIds)],
      weekdayIds: [...new Set(weekdayIds)],
    };
  }

  function parseTimeToMinutes(value) {
    if (!value || typeof value !== "string") {
      return null;
    }

    const parts = value.split(":");
    if (parts.length < 2) {
      return null;
    }

    const hours = Number(parts[0]);
    const minutes = Number(parts[1]);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
      return null;
    }

    return hours * 60 + minutes;
  }

  function isNgWeekdayAllowed(day, weekdayIds) {
    if (!weekdayIds.length) {
      return true;
    }
    return weekdayIds.includes(getWeekday(day));
  }

  function isStaffInNgShiftBand(staff, shiftTypeId, day) {
    if (!shiftTypeId || !Number.isInteger(day)) {
      return false;
    }

    const ngCondition = buildNgCondition(staff);
    const ngShiftTypeIds = ngCondition.shiftTypeIds;
    const ngWeekdayIds = ngCondition.weekdayIds;
    const isShiftTypeBlocked = ngShiftTypeIds.includes(Number(shiftTypeId));
    const isWeekdayBlocked = ngWeekdayIds.includes(getWeekday(day));
    return isShiftTypeBlocked || isWeekdayBlocked;
  }

  function findNearestAssignmentDate(staffId, targetDateString, direction) {
    const dates = Object.values(shiftAssignments)
      .filter((assignment) => Number(assignment.staffId) === Number(staffId))
      .map((assignment) => assignment.workDate)
      .filter(Boolean)
      .sort();

    if (direction === "prev") {
      let prev = null;
      dates.forEach((date) => {
        if (date < targetDateString) {
          prev = date;
        }
      });
      return prev;
    }

    return dates.find((date) => date > targetDateString) || null;
  }

  function isMinimumShiftGapViolation(staffId, day, shiftTypeId) {
    if (!shiftTypeId || minimumShiftGapHours <= 0) {
      return false;
    }

    const targetShiftType = shiftTypes.find((item) => Number(item.id) === Number(shiftTypeId));
    if (!targetShiftType) {
      return false;
    }

    const targetStartMinutes = parseTimeToMinutes(targetShiftType.startTime);
    const targetEndMinutes = parseTimeToMinutes(targetShiftType.endTime);
    if (targetStartMinutes == null || targetEndMinutes == null) {
      return false;
    }

    const targetDateString = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
    const targetDate = new Date(`${targetDateString}T00:00:00`);
    const targetStart = new Date(targetDate.getTime() + targetStartMinutes * 60 * 1000);

    const prevDateString = findNearestAssignmentDate(staffId, targetDateString, "prev");
    if (prevDateString) {
      const prevAssignment = shiftAssignments[`${staffId}-${prevDateString}`];
      const prevShiftType = shiftTypes.find((item) => Number(item.id) === Number(prevAssignment?.shiftTypeId));
      const prevStartMinutes = parseTimeToMinutes(prevShiftType?.startTime);
      const prevEndMinutes = parseTimeToMinutes(prevShiftType?.endTime);
      if (prevStartMinutes != null && prevEndMinutes != null) {
        const prevDate = new Date(`${prevDateString}T00:00:00`);
        const prevEndDayOffset = prevEndMinutes <= prevStartMinutes ? 1 : 0;
        const prevEnd = new Date(prevDate.getTime() + (prevEndMinutes + prevEndDayOffset * 24 * 60) * 60 * 1000);
        const gapHours = (targetStart.getTime() - prevEnd.getTime()) / (60 * 60 * 1000);
        if (gapHours < minimumShiftGapHours) {
          return true;
        }
      }
    }

    const targetEndDayOffset = targetEndMinutes <= targetStartMinutes ? 1 : 0;
    const targetEnd = new Date(targetDate.getTime() + (targetEndMinutes + targetEndDayOffset * 24 * 60) * 60 * 1000);

    const nextDateString = findNearestAssignmentDate(staffId, targetDateString, "next");
    if (nextDateString) {
      const nextAssignment = shiftAssignments[`${staffId}-${nextDateString}`];
      const nextShiftType = shiftTypes.find((item) => Number(item.id) === Number(nextAssignment?.shiftTypeId));
      const nextStartMinutes = parseTimeToMinutes(nextShiftType?.startTime);
      if (nextStartMinutes != null) {
        const nextDate = new Date(`${nextDateString}T00:00:00`);
        const nextStart = new Date(nextDate.getTime() + nextStartMinutes * 60 * 1000);
        const gapHours = (nextStart.getTime() - targetEnd.getTime()) / (60 * 60 * 1000);
        if (gapHours < minimumShiftGapHours) {
          return true;
        }
      }
    }

    return false;
  }

  function isWorkShiftType(shiftTypeId) {
    if (!shiftTypeId) {
      return false;
    }

    const shiftType = shiftTypes.find((item) => Number(item.id) === Number(shiftTypeId));
    if (!shiftType) {
      return false;
    }

    return !shiftType.isOffType;
  }

  function isConsecutiveWorkdaysViolation(staffId, day, shiftTypeId) {
    if (maxConsecutiveWorkdays <= 0) {
      return false;
    }

    if (!isWorkShiftType(shiftTypeId)) {
      return false;
    }

    const workedDays = new Set();
    Object.values(shiftAssignments)
      .filter((assignment) => Number(assignment.staffId) === Number(staffId))
      .forEach((assignment) => {
        if (!isWorkShiftType(assignment.shiftTypeId)) {
          return;
        }

        const workDate = String(assignment.workDate || "");
        const dayValue = Number(workDate.split("-")[2]);
        if (Number.isInteger(dayValue)) {
          workedDays.add(dayValue);
        }
      });

    workedDays.add(Number(day));

    let consecutive = 1;
    let cursor = Number(day) - 1;
    while (workedDays.has(cursor)) {
      consecutive += 1;
      cursor -= 1;
    }

    cursor = Number(day) + 1;
    while (workedDays.has(cursor)) {
      consecutive += 1;
      cursor += 1;
    }

    return consecutive > maxConsecutiveWorkdays;
  }

  function isShiftMismatch(staff, shiftTypeId, day) {
    const shiftRequest = getShiftRequest(staff.id, day);
    if (!shiftRequest) {
      return false;
    }

    if (shiftRequest.desiredShiftTypeId == null) {
      return Boolean(shiftTypeId);
    }

    if (!shiftTypeId) {
      return true;
    }

    return Number(shiftRequest.desiredShiftTypeId) !== Number(shiftTypeId);
  }

  async function loadShiftRequestsForMonth(startDate, endDate, staffs) {
    const groupIds = [...new Set(staffs.map((staff) => staff.groupId).filter(Boolean))];
    const ungroupedStaffIds = [...new Set(staffs.filter((staff) => !staff.groupId).map((staff) => staff.id))];

    const requestFetches = [
      ...groupIds.map((groupId) =>
        fetchWithAuth(`/api/shift-requests/group/${groupId}?startDate=${startDate}&endDate=${endDate}`, {
          redirectOnUnauthorized: false,
        })
      ),
      ...ungroupedStaffIds.map((staffId) =>
        fetchWithAuth(`/api/shift-requests/staff/${staffId}?startDate=${startDate}&endDate=${endDate}`, {
          redirectOnUnauthorized: false,
        })
      ),
    ];

    const responses = await Promise.all(requestFetches);
    const requestMap = {};

    for (const response of responses) {
      if (!response.ok) {
        continue;
      }

      const requests = await response.json();
      requests.forEach((request) => {
        requestMap[`${request.staffId}-${request.workDate}`] = request;
      });
    }

    return requestMap;
  }

  async function handleShiftChange(staffId, day, selectedShiftTypeId) {
    const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
    const key = `${staffId}-${dateStr}`;
    
    try {
      if (!selectedShiftTypeId) {
        // Delete shift
        const assignment = shiftAssignments[key];
        if (assignment) {
          await fetchWithAuth(`/api/shift-assignments/${assignment.id}`, { 
            method: "DELETE",
            redirectOnUnauthorized: false,
          });
          const newMap = { ...shiftAssignments };
          delete newMap[key];
          setShiftAssignments(newMap);
        }
      } else {
        // Create or update shift
        const shiftTypeId = Number(selectedShiftTypeId);
        const shiftType = shiftTypes.find((st) => st.id === shiftTypeId);
        if (!shiftType) {
          setMessage("無効なシフトです。");
          setMessageType("error");
          return;
        }

        const assignment = shiftAssignments[key];
        if (assignment) {
          // Update existing
          const response = await fetchWithAuth(`/api/shift-assignments/${assignment.id}`, {
            method: "PUT",
            headers: { 
              "Content-Type": "application/json"
            },
            body: JSON.stringify({ shiftTypeId: shiftType.id }),
            redirectOnUnauthorized: false,
          });
          if (response.ok) {
            const updated = await response.json();
            setShiftAssignments({ ...shiftAssignments, [key]: updated });
          }
        } else {
          // Create new
          const response = await fetchWithAuth("/api/shift-assignments", {
            method: "POST",
            headers: { 
              "Content-Type": "application/json"
            },
            body: JSON.stringify({ staffId, workDate: dateStr, shiftTypeId: shiftType.id }),
            redirectOnUnauthorized: false,
          });
          if (response.ok) {
            const created = await response.json();
            setShiftAssignments({ ...shiftAssignments, [key]: created });
          }
        }
      }
    } catch (e) {
      setMessage("シフト更新に失敗しました。");
      setMessageType("error");
    } finally {
      setActiveCellKey(null);
    }
  }

  function openCellEditor(staffId, day) {
    const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
    if (holidayDates.includes(dateStr)) {
      return;
    }
    if (isCurrentMonthConfirmed && !isAdminRole) {
      setMessage("確定済み月のシフトは管理者のみ変更できます。");
      setMessageType("error");
      return;
    }
    setActiveCellKey(`${staffId}-${dateStr}`);
  }

  async function handleConfirmCurrentMonth() {
    const monthKey = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, "0")}`;
    if (isCurrentMonthConfirmed) {
      setMessage(`${monthKey}はすでに確定済みです。`);
      setMessageType("success");
      return;
    }

    setConfirmingMonth(true);
    setMessage("");
    setMessageType("");

    try {
      const res = await fetchWithAuth(`/api/system-settings/confirmedShiftMonths/text?value=${encodeURIComponent(monthKey)}`, {
        method: "PUT",
        redirectOnUnauthorized: false,
      });

      if (!res.ok) {
        const detail = await res.text();
        throw new Error(detail || "確定処理に失敗しました。" );
      }

      setIsCurrentMonthConfirmed(true);
      setMessage(`${monthKey} を確定しました。`);
      setMessageType("success");
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setConfirmingMonth(false);
    }
  }

  async function handleAutoGenerate() {
    if (isCurrentMonthConfirmed) {
      setMessage("この月は確定済みのため自動生成できません。");
      setMessageType("error");
      return;
    }

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;
    setAutoGenerating(true);
    setMessage("");
    setMessageType("");

    try {
      const res = await fetchWithAuth(
        `/api/shift-assignments/auto-generate?year=${year}&month=${month}`,
        {
          method: "POST",
          redirectOnUnauthorized: false,
        }
      );

      if (!res.ok) {
        const detail = await res.text();
        throw new Error(detail || "自動生成に失敗しました。");
      }

      let result = null;
      try {
        result = await res.json();
      } catch (e) {
        result = null;
      }

      const unmetConditions = Array.isArray(result?.unmetConditions) ? result.unmetConditions : [];
      const unmetConditionLines = unmetConditions.map((condition) => {
        const workDate = condition.workDate || "日付不明";
        const shiftTypeName = condition.shiftTypeName || "シフト不明";
        const shortageCount = condition.shortageCount ?? 0;
        const reason = condition.reason || "条件を満たせませんでした。";
        return `${workDate} ${shiftTypeName}: 不足 ${shortageCount}件 (${reason})`;
      });
      setMessage(
        [
          `自動生成が完了しました。生成件数: ${result?.generatedCount ?? 0} 件 / 未割当(必須希望): ${result?.unassignedRequiredCount ?? 0} 件 / リトライ: ${result?.retryCount ?? 0} 回`,
          unmetConditionLines.length > 0 ? `未充足条件:\n${unmetConditionLines.join("\n")}` : "",
        ].filter(Boolean).join("\n")
      );
      setMessageType("success");
      await loadData(true);
    } catch (e) {
      setMessage(e.message || "自動生成に失敗しました。");
      setMessageType("error");
    } finally {
      setAutoGenerating(false);
    }
  }

  async function handleClearMonthShifts() {
    const targetYear = currentDate.getFullYear();
    const targetMonth = currentDate.getMonth() + 1;

    if (isCurrentMonthConfirmed && !(isAdminRole && isFutureMonth)) {
      setMessage("この月は確定済みのためクリアできません。");
      setMessageType("error");
      return;
    }

    const monthKey = `${targetYear}-${String(targetMonth).padStart(2, "0")}`;
    const confirmed = window.confirm(`${monthKey} のシフト状態をクリアします。\n申請データは保持されます。よろしいですか？`);
    if (!confirmed) {
      return;
    }

    const year = targetYear;
    const month = targetMonth;
    setClearingMonth(true);
    setMessage("");
    setMessageType("");

    try {
      const res = await fetchWithAuth(
        `/api/shift-assignments/month?year=${year}&month=${month}`,
        {
          method: "DELETE",
          redirectOnUnauthorized: false,
        }
      );

      if (!res.ok) {
        const detail = await res.text();
        throw new Error(detail || "シフト状態のクリアに失敗しました。");
      }

      let result = null;
      try {
        result = await res.json();
      } catch (e) {
        result = null;
      }

      setIsCurrentMonthConfirmed(false);
      setMessage(`シフト状態をクリアしました（${result?.deletedCount ?? 0}件）。申請データは保持されています。`);
      setMessageType("success");
      await loadData(true);
    } catch (e) {
      setMessage(e.message || "シフト状態のクリアに失敗しました。");
      setMessageType("error");
    } finally {
      setClearingMonth(false);
    }
  }

  const monthKey = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, "0")}`;
  const monthLabel = `${currentDate.getFullYear()}年 ${String(currentDate.getMonth() + 1).padStart(2, "0")}月`;
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;
  const isFutureMonth = currentDate.getFullYear() > currentYear || (currentDate.getFullYear() === currentYear && currentDate.getMonth() + 1 > currentMonth);
  const daysInMonth = getDaysInMonth(currentDate);
  const days = Array.from({ length: daysInMonth }, (_, i) => i + 1);

  if (loading) return <div className="card">読み込み中...</div>;

  if (!staffsByGroup.length && !message) {
    return (
      <div className="card">
        <p>表示できるスタッフ情報がありません。ログイン状態または API 接続を確認してください。</p>
      </div>
    );
  }

  return (
    <div>
      {message && <div className={messageType} style={{ marginBottom: "1rem", padding: "0.75rem", borderRadius: "4px", whiteSpace: "pre-line" }}>{message}</div>}

      <div style={{ marginBottom: "1.5rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem", gap: "0.5rem" }}>
          <button
            type="button"
            onClick={previousMonth}
            style={{
              padding: "0.4rem 0.6rem",
              backgroundColor: "#ddd",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "0.85rem",
              whiteSpace: "nowrap",
            }}
          >
            ← 前月
          </button>
          <h2 style={{ margin: "0 auto", textAlign: "center", fontSize: "1.2rem", whiteSpace: "nowrap" }}>{monthLabel}</h2>
          <button
            type="button"
            onClick={nextMonth}
            style={{
              padding: "0.4rem 0.6rem",
              backgroundColor: "#ddd",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "0.85rem",
              whiteSpace: "nowrap",
            }}
          >
            次月 →
          </button>
        </div>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button
            type="button"
            onClick={handleConfirmCurrentMonth}
            disabled={confirmingMonth || isCurrentMonthConfirmed}
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: isCurrentMonthConfirmed ? "#059669" : "var(--accent)",
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: confirmingMonth || isCurrentMonthConfirmed ? "default" : "pointer",
              fontWeight: 600,
              opacity: confirmingMonth ? 0.7 : 1,
            }}
          >
            {confirmingMonth ? "処理中..." : isCurrentMonthConfirmed ? "確定済み" : "確定"}
          </button>
          <button
            type="button"
            onClick={handleAutoGenerate}
            disabled={autoGenerating || clearingMonth || loading || isCurrentMonthConfirmed}
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: "var(--accent)",
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: autoGenerating || clearingMonth || loading || isCurrentMonthConfirmed ? "default" : "pointer",
              fontWeight: 600,
              opacity: autoGenerating ? 0.7 : 1,
            }}
          >
            {autoGenerating ? "生成中..." : "[自動生成]"}
          </button>
          <button
            type="button"
            onClick={handleClearMonthShifts}
            disabled={clearingMonth || autoGenerating || loading || (isCurrentMonthConfirmed && !(isAdminRole && isFutureMonth))}
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: "#d9534f",
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: clearingMonth || autoGenerating || loading || (isCurrentMonthConfirmed && !(isAdminRole && isFutureMonth)) ? "default" : "pointer",
              fontWeight: 600,
              opacity: clearingMonth ? 0.7 : 1,
            }}
          >
            {clearingMonth ? "クリア中..." : "[シフト状態クリア]"}
          </button>
        </div>
      </div>

      <div className="shift-table-container">
        <div className="shift-fixed-column">
          <div className="shift-header-row" style={{ height: "2.5rem", alignItems: "center" }}>
            <div style={{ padding: "0.5rem", fontWeight: 600, textAlign: "center" }}>番号</div>
            <div style={{ padding: "0.5rem", fontWeight: 600, textAlign: "left" }}>氏名</div>
          </div>

          {staffsByGroup.map((group) => (
            <div key={group.groupName}>
              <div
                style={{
                  padding: "0.75rem",
                  backgroundColor: "#f0f0f0",
                  fontWeight: 600,
                  borderBottom: "2px solid var(--line)",
                  textAlign: "left",
                  height: "2.5rem",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                {group.groupName}
              </div>

              {group.staffs.map((staff, idx) => {
                const assignedCount = days.filter((day) => {
                  const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
                  const cellKey = `${staff.id}-${dateStr}`;
                  const assignment = shiftAssignments[cellKey];
                  return Boolean(assignment);
                }).length;

                return (
                  <div key={staff.id} className="shift-staff-row" style={{ display: "flex", borderBottom: "1px solid var(--line)" }}>
                    <div style={{ padding: "0.5rem", width: "3rem", textAlign: "center", fontSize: "0.9rem" }}>
                      {idx + 1}
                    </div>
                    <div style={{ padding: "0.5rem", flex: 1, display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.35rem" }}>
                      <span style={{ textAlign: "left" }}>{staff.staffName}</span>
                      {assignedCount > 0 && (
                        <span style={{ color: "#6b7280", fontSize: "0.85rem", marginLeft: "auto", textAlign: "right" }}>{assignedCount}</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        <div className="shift-scroll-column">
          <div className="shift-header-row" style={{ height: "2.5rem", alignItems: "center" }}>
            {days.map((day) => (
              <div
                key={day}
                className="shift-date-header-cell"
                style={{
                  padding: "0.5rem",
                  fontWeight: 600,
                  textAlign: "center",
                  minWidth: "3rem",
                  borderRight: "1px solid var(--line)",
                  ...getWeekendColumnStyle(day),
                }}
              >
                <span className="shift-date-number">{day}</span>
                <span className="shift-date-weekday">({getWeekdayLabel(day)})</span>
              </div>
            ))}
          </div>

          {staffsByGroup.map((group) => (
            <div key={group.groupName}>
              <div
                style={{
                  display: "flex",
                  backgroundColor: "#f0f0f0",
                  borderBottom: "2px solid var(--line)",
                  height: "2.5rem",
                  alignItems: "center",
                }}
              >
                {days.map((day) => (
                  <div
                    key={day}
                    style={{
                      padding: "0.75rem",
                      minWidth: "3rem",
                      borderRight: "1px solid var(--line)",
                      ...getWeekendColumnStyle(day),
                    }}
                  />
                ))}
              </div>

              {group.staffs.map((staff) => (
                <div key={staff.id} className="shift-staff-row" style={{ display: "flex", borderBottom: "1px solid var(--line)" }}>
                  {days.map((day) => {
                    const dateStr = toLocalDateStringByDay(currentDate.getFullYear(), currentDate.getMonth(), day);
                    const cellKey = `${staff.id}-${dateStr}`;
                    const currentShiftName = getShiftName(staff.id, day);
                    const shiftRequest = getShiftRequest(staff.id, day);
                    const weekendColumnStyle = getWeekendColumnStyle(day);
                    const isHoliday = isHolidayDate(dateStr, holidayDates, holidayWeekdays);
                    const isActive = activeCellKey === cellKey;
                    const assignment = shiftAssignments[cellKey];
                    const currentShiftTypeId = assignment ? String(assignment.shiftTypeId) : "";
                    const isMismatch = isShiftMismatch(staff, currentShiftTypeId, day);
                    const isNgBandViolation = isStaffInNgShiftBand(staff, currentShiftTypeId, day);
                    const isShiftGapViolation = isMinimumShiftGapViolation(staff.id, day, currentShiftTypeId);
                    const isConsecutiveViolation = isConsecutiveWorkdaysViolation(staff.id, day, currentShiftTypeId);
                    const isRequiredCountShortageCell = isRequiredCountShortageDateForStaff(dateStr, staff.id);
                    const shouldHighlightRed = isMismatch || isNgBandViolation || isShiftGapViolation || isConsecutiveViolation || isRequiredCountShortageCell;

                    if (isActive) {
                      return (
                        <select
                          className="shift-cell-control shift-cell-select"
                          key={`${staff.id}-${day}`}
                          value={currentShiftTypeId}
                          onChange={(e) => handleShiftChange(staff.id, day, e.target.value)}
                          onBlur={() => setActiveCellKey(null)}
                          autoFocus
                          style={{
                            minWidth: "3rem",
                            border: "none",
                            borderRight: "1px solid var(--line)",
                            backgroundColor: weekendColumnStyle.backgroundColor,
                            color: shouldHighlightRed ? "#b91c1c" : "#111827",
                          }}
                        >
                          <option value="">-</option>
                          {shiftTypes.map((shiftType) => (
                            <option key={shiftType.id} value={shiftType.id}>
                              {shiftType.shiftName}
                            </option>
                          ))}
                        </select>
                      );
                    }

                    return (
                      <button
                        className="shift-cell-control shift-cell-button"
                        key={`${staff.id}-${day}`}
                        type="button"
                        onClick={() => openCellEditor(staff.id, day)}
                        style={{
                          minWidth: "3rem",
                          border: "none",
                          borderRight: "1px solid var(--line)",
                          backgroundColor: isHoliday
                            ? "#fef3c7"
                            : weekendColumnStyle.backgroundColor,
                          color: isHoliday
                            ? "#b45309"
                            : shouldHighlightRed
                              ? "#b91c1c"
                              : "#111827",
                          cursor: isHoliday ? "not-allowed" : "pointer",
                        }}
                        title={isHoliday ? "休業日です" : "クリックしてシフトを選択"}
                        disabled={isHoliday || (isCurrentMonthConfirmed && !isAdminRole)}
                      >
                        <span className="shift-cell-symbol">{isHoliday ? "休" : currentShiftName || "-"}</span>
                        {shiftRequest && (
                          <span className={`shift-request-badge status-${shiftRequest.status?.toLowerCase?.() || "unknown"}`}>
                            {shiftRequest.desiredShiftName} {REQUEST_STATUS_SHORT_LABELS[shiftRequest.status] || shiftRequest.status}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
