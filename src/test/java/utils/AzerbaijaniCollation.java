package utils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;

/**
 * Azerbaijani Latin alphabet PRIMARY ordering for GRAD-102 names.
 *
 * On some JDKs, Collator.getInstance(Locale.forLanguageTag("az-AZ"))
 * falls back to an incomplete locale collation table, placing Ə after Z.
 * The lab's sort contract uses JavaScript localeCompare(..., "az") and
 * expects E < Ə < F. This dependency-free, deterministic primary alphabet
 * comparator avoids JDK/CLDR-provider differences for Azerbaijani letters.
 *
 * This checks primary alphabet order, not every ICU/ECMA-402 secondary and
 * tertiary rule for arbitrary punctuation, ligatures, or mixed scripts.
 */
public final class AzerbaijaniCollation {
    private static final Locale AZ = Locale.forLanguageTag("az-AZ");
    // Official primary ordering, including the distinct ı and i letters.
    private static final String ALPHABET = "abcçdeəfgğhxıijkqlmnoöprsştuüvyz";

    public static final Comparator<String> BY_ALPHABET = AzerbaijaniCollation::compare;

    private AzerbaijaniCollation() { }

    public static int compare(String first, String second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Full names to compare must not be null");
        }
        String left = primaryForm(first);
        String right = primaryForm(second);
        int i = 0;
        int j = 0;
        while (i < left.length() && j < right.length()) {
            int a = left.codePointAt(i);
            int b = right.codePointAt(j);
            int diff = Integer.compare(rank(a), rank(b));
            if (diff != 0) return diff;
            // The only same-ranked Azerbaijani letter is the same letter.
            // For unsupported characters, fall back to code-point order.
            if (a != b && ALPHABET.indexOf(a) < 0 && ALPHABET.indexOf(b) < 0) {
                return Integer.compare(a, b);
            }
            i += Character.charCount(a);
            j += Character.charCount(b);
        }
        return Integer.compare(left.length() - i, right.length() - j);
    }

    private static String primaryForm(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(AZ);
    }

    private static int rank(int codePoint) {
        // Space and name punctuation come before letters in normal collation.
        if (Character.isWhitespace(codePoint)) return 0;
        if (codePoint == '-' || codePoint == '\'' || codePoint == 0x2019) return 1;
        int index = ALPHABET.indexOf(codePoint);
        if (index >= 0) return 10 + index;
        // Unknown characters are stable, but not promised to match ICU rules.
        return 1000 + codePoint;
    }
}
