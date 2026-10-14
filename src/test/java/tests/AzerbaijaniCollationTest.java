package tests;

import org.junit.jupiter.api.Test;
import utils.AzerbaijaniCollation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for Azerbaijani collation and Unicode sort behavior. */
class AzerbaijaniCollationTest {

    @Test
    void reproducesActualTenNameResponseAsCorrectAzerbaijaniOrder() {
        List<String> names = Arrays.asList(
                "Cavid Məmmədov", "Çinar Rzayev", "Dəniz Səfərov", "Elvin Quliyev",
                "Əli Həsənov", "Fuad Əliyev", "Günay Həsənli", "İradə Rəhimova",
                "Nigar Əliyeva", "Tural Hüseynov");
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(AzerbaijaniCollation.BY_ALPHABET);
        assertEquals(names, sorted, "E < Ə < F according to Azerbaijani alphabet");
    }

    @Test
    void allThirtyTwoAzerbaijaniLettersHaveCorrectPrimaryOrder() {
        String alphabet = "abcçdeəfgğhxıijkqlmnoöprsştuüvyz";
        for (int index = 0; index + 1 < alphabet.length(); index++) {
            String a = String.valueOf(alphabet.charAt(index));
            String b = String.valueOf(alphabet.charAt(index + 1));
            assertTrue(AzerbaijaniCollation.compare(a, b) < 0, a + " must precede " + b);
            assertTrue(AzerbaijaniCollation.compare(b, a) > 0, b + " must follow " + a);
        }
    }

    @Test
    void distinguishesDotlessIFromDottedI() {
        assertTrue(AzerbaijaniCollation.compare("Iradə", "İradə") < 0);
        assertTrue(AzerbaijaniCollation.compare("İradə", "Jalə") < 0);
        assertTrue(AzerbaijaniCollation.compare("Elvin", "Əli") < 0);
        assertTrue(AzerbaijaniCollation.compare("Əli", "Fuad") < 0);
    }

    @Test
    void ignoresCaseAndCanonicalUnicodeFormForPrimaryOrdering() {
        assertEquals(0, AzerbaijaniCollation.compare("ƏLİ", "əli"));
        String decomposed = Normalizer.normalize("İradə", Normalizer.Form.NFD);
        assertEquals(0, AzerbaijaniCollation.compare("İradə", decomposed));
        assertEquals(0, AzerbaijaniCollation.compare("Cavid", "cavid"));
    }

    @Test
    void comparatorIsAntisymmetricAndTransitiveForAlphabet() {
        List<String> names = Arrays.asList("Tural", "İradə", "Əli", "Çinar", "Fuad", "Elvin", "Cavid");
        for (String a : names) for (String b : names) {
            assertEquals(Integer.signum(AzerbaijaniCollation.compare(a, b)),
                    -Integer.signum(AzerbaijaniCollation.compare(b, a)));
        }
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted, AzerbaijaniCollation.BY_ALPHABET);
        assertEquals(Arrays.asList("Cavid", "Çinar", "Elvin", "Əli", "Fuad", "İradə", "Tural"), sorted);
    }
}
