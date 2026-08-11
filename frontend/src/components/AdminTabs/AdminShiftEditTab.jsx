import React, { useState, useEffect, useContext } from "react";
import { fetchWithAuth } from "../../utils/fetchWithAuth";
import { AuthContext } from "../../context/AuthContext";
import { parseHolidayDates } from "../../utils/holidayDates";
import "./AdminShiftEditTab.css";

const REQUEST_STATUS_SHORT_LABELS = {
  DRAFT: "下書",
  SUBMITTED: "申請",
  APPLIED: "承認",
  REJECTED: "却下",
};

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
      const groupName = staff.groupId ? `グループ ${staff.groupId}` : "未分類";
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

  async function loadHolidayDates() {
    try {
      const res = await fetchWithAuth("/api/system-settings", { redirectOnUnauthorized: false });
      if (!res.ok) {
        setHolidayDates([]);
        return;
      }
      const settings = await res.json();
      const holidaySetting = Array.isArray(settings) ? settings.find((item) => item.settingKey === "holidayDates") : null;
      const rawValue = holidaySetting?.settingValueText || "";
      setHolidayDates(parseHolidayDates(rawValue));
    } catch (e) {
      setHolidayDates([]);
    }
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
      return value.map((item) => Number(item)).filter((item) => Number.isFinite(item));
    }

    if (typeof value === "string") {
      const trimmed = value.trim();
      if (!trimmed) {
        return [];
      }

      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          return parsed.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
        if (parsed && Array.isArray(parsed.shiftTypeIds)) {
          return parsed.shiftTypeIds.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
      } catch (error) {
        // Ignore legacy text values.
      }
    }

    return [];
  }

  function isStaffInNgShiftBand(staff, shiftTypeId) {
    if (!shiftTypeId) {
      return false;
    }

    const ngShiftTypeIds = parseShiftTypeIds(staff?.ngShiftTypeIds ?? staff?.ngShiftTimeBands);
    if (!ngShiftTypeIds.length) {
      return false;
    }

    return ngShiftTypeIds.includes(Number(shiftTypeId));
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
                style={{
                  padding: "0.5rem",
                  fontWeight: 600,
                  textAlign: "center",
                  minWidth: "3rem",
                  borderRight: "1px solid var(--line)",
                }}
              >
                {day}
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
                      backgroundColor: "#f0f0f0",
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
                    const isHoliday = holidayDates.includes(dateStr);
                    const isActive = activeCellKey === cellKey;
                    const assignment = shiftAssignments[cellKey];
                    const currentShiftTypeId = assignment ? String(assignment.shiftTypeId) : "";
                    const isMismatch = isShiftMismatch(staff, currentShiftTypeId, day);
                    const isNgBandViolation = isStaffInNgShiftBand(staff, currentShiftTypeId);
                    const isNgBandMatch = Boolean(currentShiftTypeId) && isStaffInNgShiftBand(staff, currentShiftTypeId);
                    const shouldHighlightRed = isMismatch || isNgBandViolation || isNgBandMatch;

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
                            backgroundColor: "#fff",
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
                          backgroundColor: isHoliday ? "#fef3c7" : shouldHighlightRed ? "#fef2f2" : "#fff",
                          color: isHoliday ? "#b45309" : shouldHighlightRed ? "#b91c1c" : "inherit",
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
