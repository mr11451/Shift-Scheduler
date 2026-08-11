import React, { useState, useEffect } from "react";

export default function GroupSelector({ selectedGroupId, onChange, required = false }) {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadGroups();
  }, []);

  async function loadGroups() {
    try {
      setLoading(true);
      const response = await fetch("/api/groups");
      if (response.ok) {
        const data = await response.json();
        setGroups(data);
      } else {
        setError("グループの読み込みに失敗しました。");
      }
    } catch (err) {
      setError("通信エラーが発生しました。");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return <div className="group-selector">読み込み中...</div>;
  }

  return (
    <div className="group-selector">
      <select
        value={selectedGroupId || ""}
        onChange={(e) => onChange(e.target.value ? parseInt(e.target.value, 10) : null)}
        required={required}
      >
        <option value="">グループを選択してください</option>
        {groups.map((group) => (
          <option key={group.id} value={group.id}>
            {group.groupName}
          </option>
        ))}
      </select>
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}
