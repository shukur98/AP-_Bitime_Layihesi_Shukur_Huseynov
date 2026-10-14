package tests;

import helper.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline validation of CallMan-compatible AZ IBAN generator; no API calls. */
public class FixtureGeneratorTest {
    @Test
    void azIbanHasCorrectChecksum() {
        for (int i=0; i<20; i++) {
            String iban = TestFixtures.unusedIban();
            assertTrue(iban.matches("AZ[0-9]{2}AIIB[0-9]{20}"), iban);
            String moved = iban.substring(4) + iban.substring(0, 4);
            int mod = 0;
            for (int j = 0; j < moved.length(); j++) {
                char ch = moved.charAt(j);
                String digits = ch >= 'A' && ch <= 'Z' ? "" + (ch - 'A' + 10) : "" + ch;
                for (int k = 0; k < digits.length(); k++)
                    mod = (mod * 10 + digits.charAt(k) - '0') % 97;
            }
            assertEquals(1, mod, "ISO-13616 / MOD-97 check digits");
        }
    }
}
