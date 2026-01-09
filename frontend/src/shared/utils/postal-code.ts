/**
 * Utility functions for formatting Canadian postal codes.
 * 
 * Canadian postal codes follow the format: A1A 1A1
 * - First 3 characters: Forward Sortation Area (FSA)
 * - Space
 * - Last 3 characters: Local Delivery Unit (LDU)
 */

/**
 * Formats a postal code as the user types, adding a space after the first 3 characters.
 * Converts to uppercase and removes invalid characters.
 * 
 * @param value - The raw input value
 * @returns The formatted postal code (e.g., "H1A 1A1")
 */
export function formatPostalCode(value: string): string {
    // Remove all non-alphanumeric characters except spaces
    let cleaned = value.toUpperCase().replace(/[^A-Z0-9]/g, '');

    // Limit to 6 characters (without the space)
    if (cleaned.length > 6) {
        cleaned = cleaned.slice(0, 6);
    }

    // Insert space after first 3 characters
    if (cleaned.length > 3) {
        return `${cleaned.slice(0, 3)} ${cleaned.slice(3)}`;
    }

    return cleaned;
}

/**
 * Normalizes a postal code for storage by ensuring it's uppercase with a space in the middle.
 * 
 * @param value - The postal code to normalize
 * @returns The normalized postal code (e.g., "H1A 1A1") or empty string if invalid
 */
export function normalizePostalCode(value: string | undefined | null): string {
    if (!value) return '';

    // Remove all spaces and convert to uppercase
    const cleaned = value.toUpperCase().replace(/\s/g, '');

    // Must be exactly 6 characters
    if (cleaned.length !== 6) {
        return value.toUpperCase().trim();
    }

    // Insert space in the middle
    return `${cleaned.slice(0, 3)} ${cleaned.slice(3)}`;
}

/**
 * Validates a Canadian postal code format.
 * Accepts formats: A1A1A1, A1A 1A1, A1A-1A1
 * 
 * @param value - The postal code to validate
 * @returns True if valid, false otherwise
 */
export function isValidPostalCode(value: string | undefined | null): boolean {
    if (!value) return false;
    return /^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(value);
}

/**
 * onChange handler for postal code inputs that auto-formats as user types.
 * Use this with react-hook-form's onChange.
 * 
 * @param e - The input change event
 * @param onChange - The form field's onChange handler
 */
export function handlePostalCodeChange(
    e: React.ChangeEvent<HTMLInputElement>,
    onChange: (value: string) => void
): void {
    const formatted = formatPostalCode(e.target.value);
    onChange(formatted);
}
