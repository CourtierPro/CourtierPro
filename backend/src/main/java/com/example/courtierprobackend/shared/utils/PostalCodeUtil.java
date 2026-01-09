package com.example.courtierprobackend.shared.utils;

/**
 * Utility class for formatting Canadian postal codes.
 * 
 * Canadian postal codes follow the format: A1A 1A1
 * - First 3 characters: Forward Sortation Area (FSA)
 * - Space
 * - Last 3 characters: Local Delivery Unit (LDU)
 */
public class PostalCodeUtil {

    private PostalCodeUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Normalizes a Canadian postal code to uppercase with a space in the middle.
     * 
     * @param postalCode The postal code to normalize (can be null or in various formats)
     * @return The normalized postal code (e.g., "H1A 1A1") or null if input is null/blank
     */
    public static String normalize(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            return null;
        }
        
        // Remove all whitespace and convert to uppercase
        String cleaned = postalCode.toUpperCase().replaceAll("\\s", "");
        
        // If it's exactly 6 characters, insert space in the middle
        if (cleaned.length() == 6) {
            return cleaned.substring(0, 3) + " " + cleaned.substring(3);
        }
        
        // If already has space or hyphen in correct position, just normalize
        String withoutHyphen = postalCode.replace("-", " ").toUpperCase().trim();
        if (withoutHyphen.matches("^[A-Z]\\d[A-Z] \\d[A-Z]\\d$")) {
            return withoutHyphen;
        }
        
        // Return uppercase trimmed version if format is not standard
        return postalCode.toUpperCase().trim();
    }

    /**
     * Validates if a postal code matches the Canadian format.
     * Accepts formats: A1A1A1, A1A 1A1, A1A-1A1
     * 
     * @param postalCode The postal code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            return false;
        }
        return postalCode.matches("^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$");
    }
}
