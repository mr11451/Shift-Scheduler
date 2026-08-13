import React, { useContext, useEffect, useState } from "react";
import { AuthContext } from "./context/AuthContext";
import { fetchWithAuth } from "./utils/fetchWithAuth";
import { isHolidayDate, parseHolidayDates, parseHolidayWeekdays } from "./utils/holidayDates";
import { getMonthStatus } from "./utils/shiftMonthStatus";
import { canSelectTarget } from "./utils/viewAccess";

const today = new Date().toISOString().slice(0, 10);

const STATUS_LABELS = {
  DRAFT: "下書き",
  SUBMITTED: "申請中",
  APPLIED: "承認済",
  REJECTED: "却下",
  PENDING: "申請中",
  APPROVED: "許可済",
  CANCELED: "取り消し済",
};

function CalendarView({ viewableStaffs, selectedStaffId, onSelectStaff, calendarDate, setCalendarDate, shiftAssignments, shiftRequests, holidayDates, holidayWeekdays, isMonthConfirmed }) {
  const monthKey = `${calendarDate.getFullYear()}-${String(calendarDate.getMonth() + 1).padStart(2, "0")}`;
  const monthStatus = getMonthStatus({ monthKey, isConfirmed: isMonthConfirmed });
  const getDaysInMonth = (date) => new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  const getFirstDayOfMonth = (date) => new Date(date.getFullYear(), date.getMonth(), 1).getDay();

  const daysInMonth = getDaysInMonth(calendarDate);
  const firstDay = getFirstDayOfMonth(calendarDate);
  const monthStr = `${calendarDate.getFullYear()}-${String(calendarDate.getMonth() + 1).padStart(2, "0")}`;

  const prevMonth = () => setCalendarDate(new Date(calendarDate.getFullYear(), calendarDate.getMonth() - 1));
  const nextMonth = () => setCalendarDate(new Date(calendarDate.getFullYear(), calendarDate.getMonth() + 1));

  const getShiftForDate = (dateStr) => {
    return shiftAssignments.find((assignment) => assignment.workDate === dateStr);
  };

  const getRequestForDate = (dateStr) => {
    return shiftRequests.find((request) => request.workDate === dateStr);
  };

  const isHoliday = (dateStr) => isHolidayDate(dateStr, holidayDates, holidayWeekdays);

  const days = [];
  for (let i = 0; i < firstDay; i++) days.push(null);
  for (let i = 1; i <= daysInMonth; i++) days.push(i);

  return (
    <div className="card">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1rem",
          padding: "0.75rem",
          borderRadius: "8px",
          border: `1px solid ${monthStatus.borderColor}`,
          backgroundColor: monthStatus.backgroundColor,
        }}
      >
        <button type="button" onClick={prevMonth} style={{ flex: 0 }}>
          ← 前月
        </button>
        <div style={{ textAlign: "center" }}>
          <h2 style={{ margin: 0 }}>
            {calendarDate.getFullYear()}年 {calendarDate.getMonth() + 1}月
          </h2>
          <div style={{ marginTop: "0.2rem", color: monthStatus.accentColor, fontSize: "0.9rem", fontWeight: 600 }}>
            {monthStatus.label}
          </div>
        </div>
        <button type="button" onClick={nextMonth} style={{ flex: 0 }}>
          次月 →
        </button>
      </div>

      <div style={{ marginBottom: "1rem" }}>
        <label htmlFor="calendarStaff">スタッフ </label>
        <select
          id="calendarStaff"
          value={selectedStaffId}
          onChange={(e) => onSelectStaff(e.target.value)}
        >
          <option value="">-- スタッフを選択 --</option>
          {viewableStaffs.map((s) => (
            <option key={s.id} value={s.id}>
              {s.staffName}
            </option>
          ))}
        </select>
        <div style={{ marginTop: "0.35rem", color: "#6b7280", fontSize: "0.9rem" }}>
          閲覧可能なスタッフはそのまま選択できます。承認が必要な相手は申請の確認ダイアログが表示されます。
        </div>
      </div>

      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr>
            {["日", "月", "火", "水", "木", "金", "土"].map((d) => (
              <th key={d} style={{ padding: "0.5rem", border: "1px solid var(--line)", textAlign: "center" }}>
                {d}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: Math.ceil(days.length / 7) }).map((_, weekIdx) => (
            <tr key={weekIdx}>
              {days.slice(weekIdx * 7, (weekIdx + 1) * 7).map((day, dayIdx) => {
                const dateStr = day
                  ? `${monthStr}-${String(day).padStart(2, "0")}`
                  : null;
                const shift = dateStr ? getShiftForDate(dateStr) : null;
                const request = dateStr ? getRequestForDate(dateStr) : null;

                return (
                  <td
                    key={dayIdx}
                    style={{
                      padding: "0.75rem",
                      border: "1px solid var(--line)",
                      minHeight: "80px",
                      verticalAlign: "top",
                      backgroundColor: day ? (monthStatus.kind === "confirmed" ? "#f8fffb" : "#fff") : "#f5f5f5",
                    }}
                  >
                    {day && (
                      <>
                        <strong>{day}</strong>
                        {dateStr && isHoliday(dateStr) && (
                          <div style={{ marginTop: "0.25rem", fontSize: "0.8rem", color: "#b45309", fontWeight: 600 }}>
                            休業日
                          </div>
                        )}
                        {selectedStaffId && shift && (
                          <div style={{ marginTop: "0.5rem", fontSize: "0.9rem" }}>
                            <div style={{ color: "#666" }}>{shift.shiftName}</div>
                          </div>
                        )}
                        {selectedStaffId && request && (
                          <div style={{ marginTop: "0.4rem", fontSize: "0.85rem", padding: "0.3rem 0.4rem", backgroundColor: "#f3f4f6", borderRadius: "6px" }}>
                            <div style={{ color: "#374151" }}>{request.desiredShiftName}</div>
                            <div style={{ color: "#6b7280", fontSize: "0.8rem" }}>
                              {STATUS_LABELS[request.status] ?? request.status}
                            </div>
                          </div>
                        )}
                      </>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FormView({ shiftTypes, workDate, setWorkDate, shiftTypeId, setShiftTypeId, onSubmit, loading, message, messageType, shiftRequests, onDeleteRequest, permissionRequests, onApprovePermission, onRejectPermission, onCancelPermission, approvedPermissions, onRemoveViewTarget, holidayDates, holidayWeekdays, isMonthConfirmed }) {
  const sortedRequests = [...(shiftRequests || [])].sort((a, b) => (b.workDate || "").localeCompare(a.workDate || ""));

  return (
    <form className="card" onSubmit={onSubmit}>
      <h2>シフト申請登録</h2>

      <label htmlFor="workDate">勤務日</label>
      <input
        id="workDate"
        type="date"
        value={workDate}
        onChange={(e) => setWorkDate(e.target.value)}
        required
      />

      <label htmlFor="shiftTypeId">希望シフト</label>
      <select
        id="shiftTypeId"
        value={shiftTypeId}
        onChange={(e) => setShiftTypeId(e.target.value)}
        required
      >
        <option value="">-- シフトを選択 --</option>
        <option value="vacation">休暇</option>
        {shiftTypes.map((t) => (
          <option key={t.id} value={t.id}>
            {t.shiftName}
            {t.startTime ? ` (${t.startTime}〜${t.endTime})` : ""}
            {t.isOffType ? "（休暇）" : ""}
          </option>
        ))}
      </select>

      <div style={{ marginBottom: "0.75rem", color: "#6b7280", fontSize: "0.9rem" }}>
        希望シフトは自動生成時の参考情報で、NGシフトより優先度は低くなります。
      </div>
      <button type="submit" disabled={loading || isMonthConfirmed}>
        {loading ? "登録中..." : isMonthConfirmed ? "確定済みのため申請できません" : "申請する"}
      </button>
      {message && <div className={messageType}>{message}</div>}

      <div style={{ marginTop: "1.5rem", borderTop: "1px solid var(--line)", paddingTop: "1rem" }}>
        <h3 style={{ margin: "0 0 0.75rem" }}>申請済み一覧</h3>
        {isMonthConfirmed ? (
          <p style={{ margin: 0, color: "#6b7280" }}>この月はシフトが確定済みのため、申請は表示されません。</p>
        ) : sortedRequests.length === 0 ? (
          <p style={{ margin: 0, color: "#6b7280" }}>まだ申請がありません。</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: "0.5rem" }}>
            {sortedRequests.map((request) => (
              <li key={request.id} style={{ border: "1px solid var(--line)", borderRadius: "8px", padding: "0.75rem", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "0.75rem" }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{request.workDate}</div>
                  {isHolidayDate(request.workDate, holidayDates, holidayWeekdays) && (
                    <div style={{ color: "#b45309", fontSize: "0.85rem", marginTop: "0.2rem", fontWeight: 600 }}>
                      休業日
                    </div>
                  )}
                  <div style={{ color: "#374151", marginTop: "0.2rem" }}>{request.desiredShiftName || "シフト未指定"}</div>
                  <div style={{ color: "#6b7280", fontSize: "0.85rem", marginTop: "0.2rem" }}>
                    {STATUS_LABELS[request.status] ?? request.status}
                  </div>
                </div>
                {request.status === "DRAFT" && (
                  <button
                    type="button"
                    onClick={() => onDeleteRequest(request.id)}
                    disabled={request.workDate && new Date(request.workDate) < new Date(new Date().toDateString())}
                    style={{ padding: "0.4rem 0.7rem", border: "1px solid #dc2626", borderRadius: "6px", backgroundColor: "#fff1f2", color: "#b91c1c", cursor: request.workDate && new Date(request.workDate) < new Date(new Date().toDateString()) ? "not-allowed" : "pointer", opacity: request.workDate && new Date(request.workDate) < new Date(new Date().toDateString()) ? 0.6 : 1 }}
                  >
                    {request.workDate && new Date(request.workDate) < new Date(new Date().toDateString()) ? "操作不可" : "削除"}
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div style={{ marginTop: "1.5rem", borderTop: "1px solid var(--line)", paddingTop: "1rem" }}>
        <h3 style={{ margin: "0 0 0.75rem" }}>閲覧承認申請</h3>
        {permissionRequests.length === 0 ? (
          <p style={{ margin: 0, color: "#6b7280" }}>閲覧承認の申請はありません。</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: "0.5rem" }}>
            {permissionRequests.map((permission) => (
              <li key={permission.id} style={{ border: "1px solid var(--line)", borderRadius: "8px", padding: "0.75rem", display: "grid", gap: "0.5rem" }}>
                <div style={{ fontWeight: 600 }}>{permission.requesterStaffName} → {permission.targetStaffName}</div>
                <div style={{ color: "#374151" }}>状態: {STATUS_LABELS[permission.status] ?? permission.status}</div>
                {permission.status === "PENDING" && (
                  <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                    <button type="button" onClick={() => onApprovePermission(permission.id)} style={{ padding: "0.4rem 0.7rem", border: "1px solid #059669", borderRadius: "6px", backgroundColor: "#ecfdf5", color: "#047857", cursor: "pointer" }}>
                      許可
                    </button>
                    <button type="button" onClick={() => onRejectPermission(permission.id)} style={{ padding: "0.4rem 0.7rem", border: "1px solid #dc2626", borderRadius: "6px", backgroundColor: "#fff1f2", color: "#b91c1c", cursor: "pointer" }}>
                      却下
                    </button>
                  </div>
                )}
                {permission.status === "APPROVED" && (
                  <button type="button" onClick={() => onCancelPermission(permission.id)} style={{ padding: "0.4rem 0.7rem", border: "1px solid #6b7280", borderRadius: "6px", backgroundColor: "#f9fafb", color: "#374151", cursor: "pointer", width: "fit-content" }}>
                    取り消し
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div style={{ marginTop: "1.5rem", borderTop: "1px solid var(--line)", paddingTop: "1rem" }}>
        <h3 style={{ margin: "0 0 0.75rem" }}>閲覧できるスタッフ一覧</h3>
        {approvedPermissions.length === 0 ? (
          <p style={{ margin: 0, color: "#6b7280" }}>現在閲覧できるスタッフはいません。</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: "0.5rem" }}>
            {approvedPermissions.map((permission) => (
              <li key={permission.id} style={{ border: "1px solid var(--line)", borderRadius: "8px", padding: "0.75rem", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "0.75rem" }}>
                <span>{permission.targetStaffName || permission.targetStaffId}</span>
                <button type="button" onClick={() => onRemoveViewTarget(permission.id)} style={{ padding: "0.4rem 0.7rem", border: "1px solid #6b7280", borderRadius: "6px", backgroundColor: "#f9fafb", color: "#374151", cursor: "pointer" }}>
                  取り消し
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </form>
  );
}

export default function MemberPage() {
  const { auth } = useContext(AuthContext);
  const [staffs, setStaffs] = useState([]);
  const [viewableStaffs, setViewableStaffs] = useState([]);
  const [approvedTargetStaffIds, setApprovedTargetStaffIds] = useState([]);
  const [shiftTypes, setShiftTypes] = useState([]);
  const [shiftAssignments, setShiftAssignments] = useState([]);
  const [shiftRequests, setShiftRequests] = useState([]);
  const [permissionRequests, setPermissionRequests] = useState([]);
  const [approvedPermissions, setApprovedPermissions] = useState([]);

  const [selectedStaffId, setSelectedStaffId] = useState(auth?.staffId ? String(auth.staffId) : "");
  const [workDate, setWorkDate] = useState(today);
  const [shiftTypeId, setShiftTypeId] = useState("");

  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [loading, setLoading] = useState(false);
  const [holidayDates, setHolidayDates] = useState([]);
  const [holidayWeekdays, setHolidayWeekdays] = useState([]);
  const [isCurrentMonthConfirmed, setIsCurrentMonthConfirmed] = useState(false);
  const [isSelectedDateConfirmed, setIsSelectedDateConfirmed] = useState(false);

  const [currentView, setCurrentView] = useState("calendar");
  const [calendarDate, setCalendarDate] = useState(new Date());

  const roleLabelMap = {
    MASTER: "マスター",
    CHIEF: "チーフ",
    MEMBER: "メンバー",
  };

  const loginSummary = auth
    ? `${auth.staffName || "不明"} / ${roleLabelMap[auth.roleLevel] || auth.roleLevel || "不明"}`
    : null;

  useEffect(() => {
    loadStaffs();
    loadShiftTypes();
    loadHolidayDates();
  }, []);

  useEffect(() => {
    loadCurrentMonthConfirmationStatus(calendarDate);
  }, [calendarDate]);

  useEffect(() => {
    if (auth?.staffId) {
      loadApprovedPermissions();
      loadPermissionRequests();
    } else {
      setApprovedTargetStaffIds([]);
      setPermissionRequests([]);
      setApprovedPermissions([]);
    }
  }, [auth?.staffId]);

  useEffect(() => {
    if (!auth?.staffId) {
      setViewableStaffs([]);
      return;
    }

    if (staffs.length === 0) {
      setViewableStaffs([]);
      return;
    }

    const currentSelection = staffs.find((staff) => Number(staff.id) === Number(selectedStaffId));
    if (selectedStaffId && !currentSelection) {
      setSelectedStaffId(String(auth.staffId));
    }
  }, [auth?.staffId, staffs, selectedStaffId]);

  useEffect(() => {
    if (selectedStaffId) {
      const monthStart = `${calendarDate.getFullYear()}-${String(calendarDate.getMonth() + 1).padStart(2, "0")}-01`;
      const monthEnd = new Date(calendarDate.getFullYear(), calendarDate.getMonth() + 1, 0);
      const monthEndStr = `${monthEnd.getFullYear()}-${String(monthEnd.getMonth() + 1).padStart(2, "0")}-${String(monthEnd.getDate()).padStart(2, "0")}`;
      loadShiftAssignments(monthStart, monthEndStr);
      loadShiftRequests(monthStart, monthEndStr);
    } else {
      setShiftAssignments([]);
      setShiftRequests([]);
    }
  }, [selectedStaffId, calendarDate]);

  useEffect(() => {
    if (auth?.staffId && !selectedStaffId) {
      setSelectedStaffId(String(auth.staffId));
    }
  }, [auth, selectedStaffId]);

  useEffect(() => {
    if (!workDate) {
      setIsSelectedDateConfirmed(false);
      return;
    }

    const [year, month] = workDate.split("-").map(Number);
    if (!year || !month) {
      setIsSelectedDateConfirmed(false);
      return;
    }

    loadConfirmationStatusForDate(year, month);
  }, [workDate]);

  async function loadStaffs() {
    try {
      const res = await fetchWithAuth("/api/staffs");
      if (!res.ok) throw new Error("スタッフ一覧の取得に失敗しました。");
      const staffList = await res.json();
      setStaffs(staffList);
      setViewableStaffs(staffList);
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function loadApprovedPermissions() {
    if (!auth?.staffId) return;
    try {
      const res = await fetchWithAuth(`/api/calendar-view-permissions/requester/${auth.staffId}/status/APPROVED`);
      if (!res.ok) throw new Error("閲覧承認一覧の取得に失敗しました。");
      const permissions = await res.json();
      setApprovedPermissions(Array.isArray(permissions) ? permissions : []);
      setApprovedTargetStaffIds(Array.isArray(permissions) ? permissions.map((permission) => permission.targetStaffId) : []);
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function loadPermissionRequests() {
    if (!auth?.staffId) return;
    try {
      const res = await fetchWithAuth(`/api/calendar-view-permissions/target/${auth.staffId}/status/PENDING`);
      if (!res.ok) throw new Error("閲覧申請一覧の取得に失敗しました。");
      setPermissionRequests(await res.json());
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function loadShiftTypes() {
    try {
      const res = await fetchWithAuth("/api/shift-types/active");
      if (!res.ok) throw new Error("シフトタイプ一覧の取得に失敗しました。");
      const types = await res.json();
      setShiftTypes(Array.isArray(types) ? types : []);
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function loadHolidayDates() {
    try {
      const res = await fetchWithAuth("/api/system-settings");
      if (!res.ok) throw new Error("休業日の取得に失敗しました。" );
      const settings = await res.json();
      const holidaySetting = Array.isArray(settings) ? settings.find((item) => item.settingKey === "holidayDates") : null;
      const holidayWeekdaySetting = Array.isArray(settings) ? settings.find((item) => item.settingKey === "holidayWeekdays") : null;
      const rawValue = holidaySetting?.settingValueText || "";
      const rawWeekdays = holidayWeekdaySetting?.settingValueText || "";
      setHolidayDates(parseHolidayDates(rawValue));
      setHolidayWeekdays(parseHolidayWeekdays(rawWeekdays));
    } catch (e) {
      setHolidayDates([]);
      setHolidayWeekdays([]);
    }
  }

  async function loadCurrentMonthConfirmationStatus(date) {
    try {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const res = await fetchWithAuth(`/api/system-settings/confirmedShiftMonths/status?year=${year}&month=${month}`);
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

  async function loadConfirmationStatusForDate(year, month) {
    try {
      const res = await fetchWithAuth(`/api/system-settings/confirmedShiftMonths/status?year=${year}&month=${month}`);
      if (!res.ok) {
        setIsSelectedDateConfirmed(false);
        return;
      }
      const status = await res.json();
      setIsSelectedDateConfirmed(Boolean(status?.confirmed));
    } catch (e) {
      setIsSelectedDateConfirmed(false);
    }
  }

  async function loadShiftRequests(start, end) {
    try {
      const res = await fetchWithAuth(`/api/shift-requests/staff/${selectedStaffId}?startDate=${start}&endDate=${end}`);
      if (!res.ok) throw new Error("シフト申請一覧の取得に失敗しました。");
      setShiftRequests(await res.json());
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function loadShiftAssignments(start, end) {
    try {
      const res = await fetchWithAuth(`/api/shift-assignments/staff/${selectedStaffId}?startDate=${start}&endDate=${end}`);
      if (!res.ok) throw new Error("シフト一覧の取得に失敗しました。");
      setShiftAssignments(await res.json());
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  function showMessage(msg, type) {
    setMessage(msg);
    setMessageType(type);
    setTimeout(() => setMessage(""), 4000);
  }

  async function requestPermission(targetStaffId) {
    try {
      const res = await fetchWithAuth("/api/calendar-view-permissions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ targetStaffId: Number(targetStaffId) }),
      });
      if (!res.ok) {
        const err = await res.text();
        throw new Error(err || "閲覧承認の申請に失敗しました。");
      }
      showMessage("閲覧承認を申請しました。承認までお待ちください。", "success");
      await loadApprovedPermissions();
      setSelectedStaffId(String(auth?.staffId || ""));
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  function handleStaffSelection(nextStaffId) {
    if (!nextStaffId) {
      setSelectedStaffId("");
      return;
    }

    const targetStaff = staffs.find((staff) => Number(staff.id) === Number(nextStaffId));
    const isSelectable = canSelectTarget({
      targetStaffId: nextStaffId,
      currentStaffId: auth?.staffId,
      viewableStaffIds: viewableStaffs.map((staff) => staff.id),
      approvedTargetStaffIds,
    });

    if (isSelectable) {
      setSelectedStaffId(String(nextStaffId));
      return;
    }

    const targetName = targetStaff?.staffName || "指定されたスタッフ";
    const shouldRequest = window.confirm(`${targetName}さんの閲覧承認を申請しますか？`);
    if (!shouldRequest) {
      setSelectedStaffId(String(auth?.staffId || ""));
      return;
    }

    requestPermission(nextStaffId);
  }

  async function handlePermissionAction(permissionId, action) {
    try {
      const res = await fetchWithAuth(`/api/calendar-view-permissions/${permissionId}/${action}`, {
        method: "POST",
      });
      if (!res.ok) {
        const err = await res.text();
        throw new Error(err || "操作に失敗しました。");
      }
      showMessage(action === "approve" ? "閲覧申請を許可しました。" : action === "reject" ? "閲覧申請を却下しました。" : "閲覧許可を取り消しました。", "success");
      await loadPermissionRequests();
      await loadApprovedPermissions();
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  async function onSubmit(event) {
    event.preventDefault();
    if (!shiftTypeId) return showMessage("シフトタイプを選択してください。", "error");

    const selectedShiftType = shiftTypes.find((type) => String(type.id) === String(shiftTypeId));
    const isVacation = shiftTypeId === "vacation" || Boolean(selectedShiftType?.isOffType);

    if (isSelectedDateConfirmed) {
      showMessage("この月はすでに確定済みのため、新しい申請はできません。", "error");
      return;
    }

    setLoading(true);
    try {
      const res = await fetchWithAuth("/api/shift-requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          workDate,
          desiredShiftTypeId: isVacation ? null : Number(shiftTypeId),
          isVacation,
        }),
      });
      if (!res.ok) {
        const err = await res.text();
        throw new Error(err || "シフト申請の登録に失敗しました。");
      }
      showMessage("シフト申請を登録しました。", "success");
      setWorkDate(today);
      setShiftTypeId("");
      const monthStart = `${calendarDate.getFullYear()}-${String(calendarDate.getMonth() + 1).padStart(2, "0")}-01`;
      const monthEnd = new Date(calendarDate.getFullYear(), calendarDate.getMonth() + 1, 0);
      const monthEndStr = `${monthEnd.getFullYear()}-${String(monthEnd.getMonth() + 1).padStart(2, "0")}-${String(monthEnd.getDate()).padStart(2, "0")}`;
      await loadShiftRequests(monthStart, monthEndStr);
      setCurrentView("calendar");
    } catch (e) {
      showMessage(e.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteRequest(requestId) {
    if (!window.confirm("この申請を削除しますか？")) return;

    try {
      const res = await fetchWithAuth(`/api/shift-requests/${requestId}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const err = await res.text();
        throw new Error(err || "申請の削除に失敗しました。");
      }
      showMessage("申請を削除しました。", "success");
      const monthStart = `${calendarDate.getFullYear()}-${String(calendarDate.getMonth() + 1).padStart(2, "0")}-01`;
      const monthEnd = new Date(calendarDate.getFullYear(), calendarDate.getMonth() + 1, 0);
      const monthEndStr = `${monthEnd.getFullYear()}-${String(monthEnd.getMonth() + 1).padStart(2, "0")}-${String(monthEnd.getDate()).padStart(2, "0")}`;
      await loadShiftRequests(monthStart, monthEndStr);
    } catch (e) {
      showMessage(e.message, "error");
    }
  }

  return (
    <main className="shell">
      <h1>シフト管理</h1>
      <p className="subtitle">スタッフのシフト申請を登録・確認できます。</p>
      {loginSummary && (
        <p style={{ margin: "-0.5rem 0 1rem", fontSize: "0.8rem", color: "#6b7280" }}>
          ログイン中: {loginSummary}
        </p>
      )}

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <button
          type="button"
          onClick={() => setCurrentView("calendar")}
          style={{
            flex: 1,
            padding: "0.75rem",
            backgroundColor: currentView === "calendar" ? "var(--accent)" : "#ddd",
            color: currentView === "calendar" ? "#fff" : "#000",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          カレンダー
        </button>
        <button
          type="button"
          onClick={() => setCurrentView("form")}
          style={{
            flex: 1,
            padding: "0.75rem",
            backgroundColor: currentView === "form" ? "var(--accent)" : "#ddd",
            color: currentView === "form" ? "#fff" : "#000",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          申請フォーム
        </button>
      </div>

      <section className="layout">
        {currentView === "calendar" ? (
          <CalendarView
            viewableStaffs={viewableStaffs}
            selectedStaffId={selectedStaffId}
            onSelectStaff={handleStaffSelection}
            calendarDate={calendarDate}
            setCalendarDate={setCalendarDate}
            shiftAssignments={shiftAssignments}
            shiftRequests={shiftRequests}
            holidayDates={holidayDates}
            holidayWeekdays={holidayWeekdays}
            isMonthConfirmed={isCurrentMonthConfirmed}
          />
        ) : (
          <FormView
            shiftTypes={shiftTypes}
            workDate={workDate}
            setWorkDate={setWorkDate}
            shiftTypeId={shiftTypeId}
            setShiftTypeId={setShiftTypeId}
            onSubmit={onSubmit}
            loading={loading}
            message={message}
            messageType={messageType}
            shiftRequests={shiftRequests}
            onDeleteRequest={handleDeleteRequest}
            permissionRequests={permissionRequests}
            onApprovePermission={(permissionId) => handlePermissionAction(permissionId, "approve")}
            onRejectPermission={(permissionId) => handlePermissionAction(permissionId, "reject")}
            onCancelPermission={(permissionId) => handlePermissionAction(permissionId, "cancel")}
            approvedPermissions={approvedPermissions}
            onRemoveViewTarget={(permissionId) => handlePermissionAction(permissionId, "cancel")}
            holidayDates={holidayDates}
            holidayWeekdays={holidayWeekdays}
            isMonthConfirmed={isSelectedDateConfirmed}
          />
        )}
      </section>
    </main>
  );
}
