export function parseConfirmedMonths(rawValue = "") {
  if (typeof rawValue !== "string") {
    return [];
  }

  return rawValue
    .split(/[\n,]+/)
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => value.replace(/\s+/g, ""))
    .filter((value) => /^\d{4}-\d{2}$/.test(value));
}

export function isMonthConfirmed(monthKey, confirmedMonths = []) {
  return confirmedMonths.includes(monthKey);
}

export function getMonthStatus({ monthKey, isConfirmed = false }) {
  if (isConfirmed) {
    return {
      kind: "confirmed",
      label: "確定済み",
      accentColor: "#059669",
      backgroundColor: "#ecfdf5",
      borderColor: "#34d399",
    };
  }

  return {
    kind: "pending",
    label: "確定前",
    accentColor: "#d97706",
    backgroundColor: "#fff7ed",
    borderColor: "#fdba74",
  };
}
