package com.fredy.mobiAd.util;

public class MenuItemParser {
    /**
     * Removes the leading number, full stop, and space from a string.
     * Example: "1. EFM" -> "EFM", "2. SWAHILIES FM" -> "SWAHILIES FM"
     *
     * @param input The string to parse
     * @return The cleaned string, or the original if the pattern doesn't match
     */
    public static String removeNumberPrefix(String input) {
        if (input == null) {
            return null;
        }

        // Regular expression: matches "number. " at the start
        String pattern = "^\\d+\\.\\s+";
        return input.replaceFirst(pattern, "").trim();
    }

}
