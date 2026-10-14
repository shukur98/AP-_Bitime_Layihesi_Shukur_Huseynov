package checks;

import utils.AzerbaijaniCollation;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Standalone preflight; requires only Java 11+, not Maven or live API. */
public final class AzerbaijaniCollationSmoke {
    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        List<String> source = Arrays.asList(
                "Cavid Məmmədov", "Çinar Rzayev", "Dəniz Səfərov", "Elvin Quliyev",
                "Əli Həsənov", "Fuad Əliyev", "Günay Həsənli", "İradə Rəhimova",
                "Nigar Əliyeva", "Tural Hüseynov");
        List<String> sorted = new ArrayList<>(source);
        sorted.sort(AzerbaijaniCollation.BY_ALPHABET);
        expect(sorted.equals(source), "Actual API #28 name list must satisfy Azerbaijani ordering: " + sorted);

        String alphabet = "abcçdeəfgğhxıijkqlmnoöprsştuüvyz";
        expect(alphabet.length() == 32, "Azerbaijani alphabet must contain 32 letters");
        for (int i = 0; i + 1 < alphabet.length(); i++) {
            String a = alphabet.substring(i, i+1);
            String b = alphabet.substring(i+1, i+2);
            expect(AzerbaijaniCollation.compare(a, b) < 0, "Wrong alphabet order: " + a + " < " + b);
            expect(AzerbaijaniCollation.compare(b, a) > 0, "Wrong reverse alphabet order: " + b + " > " + a);
        }
        expect(AzerbaijaniCollation.compare("Iradə", "İradə") < 0, "Dotless I before dotted İ");
        expect(AzerbaijaniCollation.compare("İradə", "Jalə") < 0, "Dotted İ before J");
        expect(AzerbaijaniCollation.compare("Əli", "Fuad") < 0, "Ə before F");
        expect(AzerbaijaniCollation.compare("ƏLİ", "əli") == 0, "Case-insensitive primary order");
        expect(AzerbaijaniCollation.compare("İradə", Normalizer.normalize("İradə", Normalizer.Form.NFD)) == 0,
                "Unicode normalization equivalence");
        for (String a: source) for (String b:source) {
            int forward = Integer.signum(AzerbaijaniCollation.compare(a, b));
            int backward = Integer.signum(AzerbaijaniCollation.compare(b, a));
            expect(forward == -backward, "Comparator antisymmetry: " + a + ", " + b);
        }
        System.out.println("[AZ COLLATION PASS] 10-name API sample, 32 letters, I/İ, Ə, case, Unicode, antisymmetry");
    }
}
