export function formatDate(value?: string | number | Date | null): string {
  if (value === undefined || value === null || value === '') return '-';

  try {
    let normalized: string;

    if (value instanceof Date) {
      normalized = value.toISOString();
    } else {
      normalized = String(value);
    }

    // Normalize common DB datetime format 'YYYY-MM-DD HH:MM:SS' -> 'YYYY-MM-DDTHH:MM:SS'
    if (normalized.includes(' ')) {
      normalized = normalized.replace(' ', 'T');
    }

    // If the input is a date-only string like 'YYYY-MM-DD', parse it as local
    // so the date isn't shifted by timezone when creating a Date from a UTC-only string.
    let d: Date;
    const dateOnlyMatch = /^\d{4}-\d{2}-\d{2}$/.test(normalized);
    if (dateOnlyMatch) {
      const [y, m, day] = normalized.split('-').map(Number);
      d = new Date(y, m - 1, day);
    } else {
      d = new Date(normalized);
    }
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleDateString();
  } catch {
    return '-';
  }
}

export function formatDateTime(value?: string | number | Date | null): string {
  if (value === undefined || value === null || value === '') return '-';

  try {
    let normalized: string;
    if (value instanceof Date) {
      normalized = value.toISOString();
    } else {
      normalized = String(value);
    }

    if (normalized.includes(' ')) normalized = normalized.replace(' ', 'T');

    let d: Date;
    const dateOnlyMatch = /^\d{4}-\d{2}-\d{2}$/.test(normalized);
    if (dateOnlyMatch) {
      const [y, m, day] = normalized.split('-').map(Number);
      d = new Date(y, m - 1, day);
    } else {
      d = new Date(normalized);
    }
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleString();
  } catch {
    return '-';
  }
}

export function parseToTimestamp(value?: string | number | Date | null): number {
  if (!value) return 0;
  try {
    let normalized = value instanceof Date ? value.toISOString() : String(value);
    if (normalized.includes(' ')) normalized = normalized.replace(' ', 'T');
    // If it's a date-only string, construct a local-date Date and return its timestamp
    let t: number;
    const dateOnlyMatch = /^\d{4}-\d{2}-\d{2}$/.test(normalized);
    if (dateOnlyMatch) {
      const [y, m, day] = normalized.split('-').map(Number);
      t = new Date(y, m - 1, day).getTime();
    } else {
      t = Date.parse(normalized);
    }
    return Number.isNaN(t) ? 0 : t;
  } catch {
    return 0;
  }
}
