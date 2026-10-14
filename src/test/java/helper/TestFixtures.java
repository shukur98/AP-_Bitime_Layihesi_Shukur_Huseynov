package helper;

import io.restassured.response.Response;
import model.BeneficiaryListResponse;
import model.BeneficiaryRequest;
import model.BeneficiaryResponse;
import model.VirtualCardRequest;
import model.VirtualCardResponse;
import utils.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

/**
 * Optional, explicitly enabled sandbox seed data. NEVER runs against an arbitrary API.
 * No card/beneficiary is created unless QA_AUTO_FIXTURES=true; the companion
 * script creates a NEW disposable token before seeding.
 *
 * Cache is tied to the actual token fingerprint, so an old card ID can never
 * silently be used with a new 24-hour sandbox.
 */
public final class TestFixtures {
    private TestFixtures() {}
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEACHER_API = "https://api.anarabbas.com";

    public static boolean enabled() {
        return Config.enabled("QA_AUTO_FIXTURES") && TEACHER_API.equals(Config.apiUrl());
    }

    public static String accountId() {
        return Config.get("QA_CARD_ACCOUNT_ID", TEACHER_API.equals(Config.apiUrl()) ? "acc_002" : "");
    }

    /** Generate genuine AZ ISO-13616 IBAN with MOD-97 (same algorithm as CallMan). */
    public static String unusedIban() {
        StringBuilder account = new StringBuilder(20);
        account.append('9');
        for (int i = 1; i < 20; i++) account.append(RANDOM.nextInt(10));
        String moved = "AIIB" + account + "AZ00";
        int remainder = 0;
        for (int i = 0; i < moved.length(); i++) {
            char ch = moved.charAt(i);
            String digits = ch >= 'A' && ch <= 'Z' ? String.valueOf(ch - 'A' + 10) : String.valueOf(ch);
            for (int j = 0; j < digits.length(); j++) remainder = (remainder * 10 + digits.charAt(j) - '0') % 97;
        }
        return "AZ" + String.format("%02d", 98 - remainder) + "AIIB" + account;
    }

    public static synchronized void ensureBeneficiaries(String token) {
        if (!enabled()) return;
        Response r = ApiHelper.get(token, "/beneficiaries", java.util.Map.of());
        ApiAssertHelper.status(r, 200);
        List<BeneficiaryResponse> items = r.as(BeneficiaryListResponse.class).items;
        if (items == null) throw new IllegalStateException("GET /beneficiaries returned null items");
        boolean hasNickname = items.stream().anyMatch(x -> x.nickname != null && x.nickname.length() >= 2);
        int toAdd = Math.max(0, 2 - items.size());
        if (!hasNickname) toAdd = Math.max(1, toAdd);
        if (items.size() + toAdd > 10)
            throw new IllegalStateException("Fixture sandbox has too many beneficiaries; use scripts/run-fixture-regression.sh --new-sandbox");
        String[][] seeds = {{"Aydın Məmmədov", "QAAna"}, {"Çinar Rzayev", "QADost"}};
        for (int i = 0; i < toAdd; i++) {
            String[] s = seeds[i % seeds.length];
            Response created = ApiHelper.post(token, "/beneficiaries",
                    new BeneficiaryRequest(s[0], unusedIban(), "AZN", s[1]));
            ApiAssertHelper.status(created, 201);
            if (created.jsonPath().getString("id") == null)
                throw new IllegalStateException("Fixture POST /beneficiaries did not return id");
        }
    }

    public static synchronized String activeCard(String token) {
        String explicit = Config.get("QA_ACTIVE_CARD_ID");
        if (!explicit.isBlank()) return explicit;
        if (!enabled()) return "";
        String account = accountId();
        if (account.isBlank()) throw new IllegalStateException("Cannot auto-create card without QA_CARD_ACCOUNT_ID");
        String fingerprint = fingerprint(token);
        Path file = sandboxDir().resolve("fixture-card-user1.properties");
        Properties saved = read(file);
        if (Config.apiUrl().equals(saved.getProperty("host"))
                && fingerprint.equals(saved.getProperty("tokenSha256"))
                && account.equals(saved.getProperty("account"))) {
            String prior = saved.getProperty("cardId", "");
            if (!prior.isBlank()) {
                Response existing = ApiHelper.authorized(token).get("/cards/" + prior);
                if (existing.statusCode() == 200 && "ACTIVE".equals(existing.jsonPath().getString("status")))
                    return prior;
            }
        }
        Response created = ApiHelper.post(token, "/cards",
                new VirtualCardRequest(account, new BigDecimal("800.00"), "QA fixture card"));
        if (created.statusCode() != 201) {
            throw new IllegalStateException("Could not prepare ACTIVE card fixture: HTTP "
                    + created.statusCode() + ". Sandbox may have reached the 3-card limit. "
                    + "Use scripts/run-fixture-regression.sh --new-sandbox for an isolated run.");
        }
        VirtualCardResponse card = created.as(VirtualCardResponse.class);
        if (card.id == null || !"ACTIVE".equals(card.status))
            throw new IllegalStateException("Card fixture missing id or ACTIVE status");
        Properties props = new Properties();
        props.setProperty("host", Config.apiUrl());
        props.setProperty("account", account);
        props.setProperty("tokenSha256", fingerprint);
        props.setProperty("cardId", card.id);
        writePrivate(file, props);
        return card.id;
    }

    private static String fingerprint(String token) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x & 0xff));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException("SHA-256 missing", e); }
    }
    private static Path sandboxDir() {
        String root = Config.get("QA_PROJECT_DIR", System.getProperty("qa.project.dir", System.getProperty("user.dir")));
        return Paths.get(root).toAbsolutePath().normalize().resolve(".qa-sandbox");
    }
    private static Properties read(Path f) {
        Properties p = new Properties();
        try {
            if (Files.isRegularFile(f) && !Files.isSymbolicLink(f))
                try (InputStream in = Files.newInputStream(f)) { p.load(in); }
        } catch (Exception ignored) { /* stale/corrupt fixture -> recreate only in opt-in mode */ }
        return p;
    }
    private static void writePrivate(Path file, Properties p) {
        try {
            Path dir = file.getParent();
            if (Files.isSymbolicLink(dir) || Files.isSymbolicLink(file))
                throw new IllegalStateException("Fixture storage must not be symbolic links");
            Files.createDirectories(dir);
            try { Files.setPosixFilePermissions(dir, EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)); }
            catch (UnsupportedOperationException ignored) { }
            Path tmp = Files.createTempFile(dir, "fixture-card-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING))
                    { p.store(out, "Fixture ID; never commit"); }
                try { Files.setPosixFilePermissions(tmp, EnumSet.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE)); }
                catch (UnsupportedOperationException ignored) { }
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(tmp); }
        } catch (Exception e) { throw new IllegalStateException("Unable to save fixture ID safely", e); }
    }
}
