export function parseHolidayDates(rawValue = "") {
  if (typeof rawValue !== "string") {
    return [];
  }

  return rawValue
    .split(/[\n,]+/)
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => normalizeHolidayDate(value))
    .filter(Boolean);
}

export function isHolidayDate(dateStr, holidayDates = []) {
  return holidayDates.includes(dateStr);
}

function normalizeHolidayDate(value) {
  const trimmed = value.trim();

  if (!trimmed) {
    return "";
  }

  const normalized = trimmed.replace(/\s+/g, "");
  const match = normalized.match(/^(\d{4})[-/年](\d{1,2})[-/月](\d{1,2})日?$/);

  if (match) {
    const [, year, month, day] = match;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  return trimmed;
}
