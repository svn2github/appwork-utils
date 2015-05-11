package org.appwork.utils;

import java.util.Locale;

public class StringUtils {

    public static boolean contains(final String input, final String contains) {
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(contains)) {
            return false;
        }
        return input.contains(contains);
    }

    /**
     * @param name
     * @param jdPkgRule
     * @return
     */
    public static boolean endsWithCaseInsensitive(final String name, final String jdPkgRule) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(jdPkgRule)) {
            return false;
        }
        return name.toLowerCase(Locale.ENGLISH).endsWith(jdPkgRule.toLowerCase(Locale.ENGLISH));
    }

    public static boolean startsWithCaseInsensitive(final String name, final String jdPkgRule) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(jdPkgRule)) {
            return false;
        }
        return name.toLowerCase(Locale.ENGLISH).startsWith(jdPkgRule.toLowerCase(Locale.ENGLISH));
    }

    public static boolean containsIgnoreCase(String input, String contains) {
        if (input == null || contains == null) {
            return false;
        }
        return input.toLowerCase(Locale.ENGLISH).contains(contains.toLowerCase(Locale.ENGLISH));
    }

    /**
     * taken from http://stackoverflow.com/questions/4731055/whitespace-matching-regex-java
     */
    final private static String whitespace_chars = "[" /*
     * dummy empty string for homogeneity
     */
            + "\\u0009" // CHARACTER
            // TABULATION
            + "\\u000A" // LINE
            // FEED
            // (LF)
            + "\\u000B" // LINE
            // TABULATION
            + "\\u000C" // FORM
            // FEED
            // (FF)
            + "\\u000D" // CARRIAGE
            // RETURN
            // (CR)
            + "\\u0020" // SPACE
            + "\\u0085" // NEXT
            // LINE
            // (NEL)
            + "\\u00A0" // NO-BREAK
            // SPACE
            + "\\u1680" // OGHAM
            // SPACE
            // MARK
            + "\\u180E" // MONGOLIAN
            // VOWEL
            // SEPARATOR
            + "\\u2000" // EN QUAD
            + "\\u2001" // EM QUAD
            + "\\u2002" // EN SPACE
            + "\\u2003" // EM SPACE
            + "\\u2004" // THREE-PER-EM
            // SPACE
            + "\\u2005" // FOUR-PER-EM
            // SPACE
            + "\\u2006" // SIX-PER-EM
            // SPACE
            + "\\u2007" // FIGURE
            // SPACE
            + "\\u2008" // PUNCTUATION
            // SPACE
            + "\\u2009" // THIN
            // SPACE
            + "\\u200A" // HAIR
            // SPACE
            + "\\u2028" // LINE
            // SEPARATOR
            + "\\u2029" // PARAGRAPH
            // SEPARATOR
            + "\\u202F" // NARROW
            // NO-BREAK
            // SPACE
            + "\\u205F" // MEDIUM
            // MATHEMATICAL
            // SPACE
            + "\\u3000" // IDEOGRAPHIC
            // SPACE
            + "]";

    public static String trim(String input) {
        if (input != null) {
            return input.replaceAll("^" + StringUtils.whitespace_chars + "+", "").replaceAll(StringUtils.whitespace_chars + "+$", "");
        }
        return null;
    }

    /**
     * @param x
     * @param y
     * @return
     */
    public static boolean equals(final String x, final String y) {
        if (x == y) {
            return true;
        }
        if (x == null && y != null) {
            return false;
        }
        if (y == null && x != null) {
            return false;
        }
        return x.equals(y);
    }

    /**
     * @param pass
     * @param pass2
     * @return
     */
    public static boolean equalsIgnoreCase(final String pass, final String pass2) {
        if (pass == pass2) {
            return true;
        }
        if (pass == null && pass2 != null) {
            return false;
        }
        return pass.equalsIgnoreCase(pass2);
    }

    public static String fillPre(final String string, final String filler, final int minCount) {
        if (string.length() >= minCount) {
            return string;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append(string);
        while (sb.length() < minCount) {
            sb.insert(0, filler);
        }

        return sb.toString();
    }

    public static String fillPost(final String string, final String filler, final int minCount) {
        if (string.length() >= minCount) {
            return string;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append(string);
        while (sb.length() < minCount) {
            sb.append(filler);
        }

        return sb.toString();
    }

    /**
     * @param sameSource
     * @param sourceUrl
     * @return
     */
    public static String getCommonalities(final String a, final String b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        int i = 0;
        int max = Math.min(a.length(), b.length());
        for (i = 0; i < max; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, i);
    }

    /**
     * Returns wether a String is null,empty, or contains whitespace only
     *
     * @param ip
     * @return
     */
    public static boolean isEmpty(final String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /**
     * @param value
     * @return
     */
    public static boolean isNotEmpty(final String value) {
        return !StringUtils.isEmpty(value);
    }

}
