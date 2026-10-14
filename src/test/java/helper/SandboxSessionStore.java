package helper;

import utils.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

/**
 * Reuses one training sandbox token across separate Maven and Gauge JVMs.
 *
 * The token is a credential: it is stored only in a git-ignored local directory
 * (permissions 0700/0600 on macOS/Linux). Do not share or commit the directory.
 * Set QA_SESSION_PERSIST=false to disable disk persistence.
 */
public final class SandboxSessionStore {
    private SandboxSessionStore() {}

    private static final Duration SAFE_LIFETIME = Duration.ofHours(23).plusMinutes(50);
    private static final Set<PosixFilePermission> PRIVATE_DIR = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> PRIVATE_FILE = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    static boolean enabled() {
        // Only the documented teaching sandbox opts in automatically.
        return Boolean.parseBoolean(Config.get("QA_SESSION_PERSIST",
                String.valueOf("https://api.anarabbas.com".equals(Config.apiUrl()))));
    }

    private static Path directory() {
        String base = Config.get("QA_PROJECT_DIR",
                System.getProperty("qa.project.dir", System.getProperty("user.dir")));
        return Paths.get(base).toAbsolutePath().normalize().resolve(".qa-sandbox");
    }

    private static Path tokenFile(int user) { return directory().resolve("user" + user + ".properties"); }
    private static Path lockFile(int user) { return directory().resolve("user" + user + ".lock"); }

    /**
     * Cross-process lock prevents parallel Maven/Gauge commands from creating
     * two independent sandboxes due to simultaneous first-time login.
     */
    static String acquire(int user, String email, Supplier<String> newLogin) {
        if (!enabled()) return newLogin.get();
        Path lockPath = lockFile(user);
        try {
            makePrivateDir(directory());
            if (Files.isSymbolicLink(lockPath)) throw new IOException("Session lock must not be symbolic link");
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                makePrivateFile(lockPath);
                try (FileLock lock = channel.lock()) {
                    String existing = read(user, email);
                    if (existing != null) return existing;
                    String created = newLogin.get();
                    write(user, email, created);
                    return created;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot lock local sandbox session. "
                    + "Fix .qa-sandbox permissions or set QA_SESSION_PERSIST=false.", e);
        }
    }

    private static void makePrivateDir(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("Session directory must not be a symbolic link");
        Files.createDirectories(path);
        try { Files.setPosixFilePermissions(path, PRIVATE_DIR); }
        catch (UnsupportedOperationException ignored) { /* Windows / non-POSIX */ }
    }

    private static void makePrivateFile(Path path) throws IOException {
        try { Files.setPosixFilePermissions(path, PRIVATE_FILE); }
        catch (UnsupportedOperationException ignored) { /* Windows / non-POSIX */ }
    }

    /** No network request. Returns null if absent, expired, or scoped to other user/host. */
    static String read(int user, String email) {
        if (!enabled() || Config.enabled("QA_FRESH_SANDBOX")) return null;
        Path path = tokenFile(user);
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return null;
            Properties data = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                data.load(reader);
            }
            String host = data.getProperty("api", "");
            String owner = data.getProperty("email", "");
            String token = data.getProperty("token", "").trim();
            long expires = Long.parseLong(data.getProperty("reuseUntil", "0"));
            if (!Config.apiUrl().equals(host) || !email.equals(owner) || token.isEmpty()
                    || Instant.now().toEpochMilli() >= expires) return null;
            return token;
        } catch (Exception e) {
            // A corrupt local file must not be treated as a valid credential.
            return null;
        }
    }

    static void write(int user, String email, String token) {
        if (!enabled()) return;
        Path path = tokenFile(user);
        try {
            makePrivateDir(path.getParent());
            if (Files.isSymbolicLink(path)) throw new IOException("Session file must not be a symbolic link");
            Path temp = Files.createTempFile(path.getParent(), ".sandbox-", ".tmp");
            try {
                makePrivateFile(temp);
                Properties data = new Properties();
                data.setProperty("api", Config.apiUrl());
                data.setProperty("email", email);
                data.setProperty("token", token);
                data.setProperty("reuseUntil", String.valueOf(Instant.now().plus(SAFE_LIFETIME).toEpochMilli()));
                try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    data.store(writer, "Local training sandbox session. SECRET: never commit/share.");
                }
                try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
                makePrivateFile(path);
            } finally { Files.deleteIfExists(temp); }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot safely save local sandbox token. "
                    + "Fix the .qa-sandbox directory permissions or set QA_SESSION_PERSIST=false.", e);
        }
    }
}
