export function normalizeClosingDay(value) {
  const closingDay = Number(value);
  return Number.isInteger(closingDay) && closingDay >= 1 && closingDay <= 31 ? closingDay : 31;
}

export function toLocalDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function getShiftPeriod(referenceDate, configuredClosingDay) {
  const closingDay = normalizeClosingDay(configuredClosingDay);
  const date = new Date(referenceDate.getFullYear(), referenceDate.getMonth(), referenceDate.getDate());
  const closingThisMonth = new Date(date.getFullYear(), date.getMonth(), Math.min(closingDay, new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()));
  const endDate = date > closingThisMonth
    ? new Date(date.getFullYear(), date.getMonth() + 1, Math.min(closingDay, new Date(date.getFullYear(), date.getMonth() + 2, 0).getDate()))
    : closingThisMonth;
  const previousMonthLastDay = new Date(endDate.getFullYear(), endDate.getMonth(), 0).getDate();
  const startDate = new Date(endDate.getFullYear(), endDate.getMonth() - 1, Math.min(closingDay, previousMonthLastDay) + 1);
  return { startDate, endDate, key: toLocalDateString(endDate) };
}

export function getDatesInShiftPeriod(referenceDate, closingDay) {
  const { startDate, endDate } = getShiftPeriod(referenceDate, closingDay);
  const dates = [];
  for (let date = new Date(startDate); date <= endDate; date.setDate(date.getDate() + 1)) {
    dates.push(new Date(date));
  }
  return dates;
}