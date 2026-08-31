import React, { useState, useEffect } from "react";
import "./StaffList.css";
import { fetchWithAuth } from "../utils/fetchWithAuth";

// Read-only staff calendar grouped by group, with month navigation.
export default function StaffList() {
  const [staffsByGroup, setStaffsByGroup] = useState({});
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [loading, setLoading] = useState(true);
  const [currentMonth, setCurrentMonth] = useState(new Date());

  useEffect(() => {
    loadStaffsGrouped();
  }, []);

  // Fetch staff grouped for calendar display.
  async function loadStaffsGrouped() {
    try {
      setLoading(true);
      const response = await fetchWithAuth("/api/staffs");
      if (response.ok) {
        const staffs = await response.json();
        
        // Group staffs by group
        const grouped = {};
        staffs.forEach((staff) => {
          const groupName = staff.groupName || "未所属";
          if (!grouped[groupName]) {
            grouped[groupName] = [];
          }
          grouped[groupName].push(staff);
        });

        // Sort each group by staffCode and staffName
        Object.keys(grouped).forEach((groupName) => {
          grouped[groupName].sort((a, b) => {
            if (a.staffCode !== b.staffCode) {
              return a.staffCode.localeCompare(b.staffCode);
            }
            return a.staffName.localeCompare(b.staffName);
          });
        });

        setStaffsByGroup(grouped);
      } else {
        setMessage("スタッフ情報の読み込みに失敗しました。");
        setMessageType("error");
      }
    } catch (error) {
      setMessage("通信エラーが発生しました。");
      setMessageType("error");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  // Navigate the calendar to the previous month.
  function previousMonth() {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1));
  }

  // Navigate the calendar to the next month.
  function nextMonth() {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1));
  }

  // Number of days in the given month.
  function getDaysInMonth(date) {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  }

  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth() + 1;
  const daysInMonth = getDaysInMonth(currentMonth);
  const days = Array.from({ length: daysInMonth }, (_, i) => i + 1);

  if (loading) {
    return <div className="staff-list-container">読み込み中...</div>;
  }

  return (
    <div className="staff-list-container">
      <h2>シフト編集（グループ単位表示）</h2>

      {message && (
        <div className={`message message-${messageType}`}>
          {message}
        </div>
      )}

      <div className="month-navigation">
        <button onClick={previousMonth} className="btn-month">
          ◀ 前月へ
        </button>
        <span className="month-display">
          {year}年{month}月
        </span>
        <button onClick={nextMonth} className="btn-month">
          次月へ ▶
        </button>
      </div>

      <div className="shifts-table-container">
        {Object.entries(staffsByGroup).map(([groupName, staffs]) => (
          <div key={groupName} className="group-section">
            <div className="group-header">グループ: {groupName}</div>
            <table className="shifts-table">
              <thead>
                <tr>
                  <th className="col-number">番号</th>
                  <th className="col-name">氏名</th>
                  {days.map((day) => (
                    <th key={day} className="col-day">
                      {day}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {staffs.map((staff, index) => (
                  <tr key={staff.id}>
                    <td className="col-number">{index + 1}</td>
                    <td className="col-name">{staff.staffName}</td>
                    {days.map((day) => (
                      <td key={day} className="col-shift">
                        <input
                          type="text"
                          maxLength="2"
                          className="shift-input"
                          placeholder="-"
                          title={`${staff.staffName} ${year}-${month}-${day}`}
                        />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  );
}
