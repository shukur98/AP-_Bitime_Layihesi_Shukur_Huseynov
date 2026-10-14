package utils;

public final class Config {
    private Config() {}

    public static String get(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value.isEmpty() ? fallback : value;
    }

    public static String apiUrl() {
        String base = get("QA_API_BASE_URL", "https://api.anarabbas.com");
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public static boolean enabled(String key) {
        return Boolean.parseBoolean(get(key, "false"));
    }
}
