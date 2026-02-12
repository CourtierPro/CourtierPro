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

    // Check if it's a timestamp without timezone info (e.g. '2023-10-25T14:30:00')
    // If so, assume UTC by appending 'Z'
    const isIsoNoTz = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/.test(normalized);
    if (isIsoNoTz) {
      normalized += 'Z';
    }

    let d: Date;
    const dateOnlyMatch = /^\d{4}-\d{2}-\d{2}$/.test(normalized);
    if (dateOnlyMatch) {
      const [y, m, day] = normalized.split('-').map(Number);
      d = new Date(y, m - 1, day);
    } else {
      d = new Date(normalized);
    }
    if (Number.isNaN(d.getTime())) return '-';
    // Use 'fr-CA' or undefined (browser default) based on need, but usually toLocaleString() respects browser
    // However, to ensure consistency with the user's issue, let's stick to standard behavior.
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

/**
 * Parse a date-only string (e.g., "2026-01-20") as a local date, not UTC.
 * This prevents the off-by-one day issue when displaying dates in local timezones.
 * For datetime strings, falls back to regular Date parsing.
 */
export function parseLocalDate(dateString?: string): Date | null {
  if (!dateString) return null;

  try {
    // Check if it's a date-only string (YYYY-MM-DD)
    const dateOnlyMatch = /^\d{4}-\d{2}-\d{2}$/.test(dateString);
    if (dateOnlyMatch) {
      const [year, month, day] = dateString.split('-').map(Number);
      return new Date(year, month - 1, day);
    }
    // For datetime strings, use regular parsing
    const date = new Date(dateString);
    return Number.isNaN(date.getTime()) ? null : date;
  } catch {
    return null;
  }
}

/**
 * Returns the current date in YYYY-MM-DD format based on local time.
 * Used for setting min/max attributes on date inputs.
 */
export function getLocalDateString(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
