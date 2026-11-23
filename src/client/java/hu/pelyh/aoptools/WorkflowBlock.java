package hu.pelyh.aoptools;

import java.util.Locale;
import java.util.regex.Pattern;

public record WorkflowBlock(Type type, String target, String value) {
    private static final Pattern BLOCK_ID = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+(?:\\[[a-z0-9_=,.-]+])?");
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    public static final String DEFAULT_REASON = "AOP blokkprogram";

    public enum Type {
        OP_ALL("OP ALL", 0xFF4CAF50, false, false, false, false),
        DEOP_ALL("deOP ALL", 0xFFFFA726, false, false, false, false),
        OP_PLAYER("OP XY", 0xFF42A5F5, true, false, false, false),
        DEOP_PLAYER("deOP XY", 0xFFAB47BC, true, false, false, false),
        KILL_ALL("KILL ALL", 0xFFE53935, false, false, false, true),
        KILL_PLAYER("KILL XY", 0xFFF06292, true, false, false, true),
        KICK_ALL("KICK ALL", 0xFFEF5350, false, false, true, true),
        KICK_PLAYER("KICK XY", 0xFFFF7043, true, false, true, true),
        BAN_ALL("BAN ALL", 0xFFD32F2F, false, false, true, true),
        BAN_PLAYER("BAN XY", 0xFFB71C1C, true, false, true, true),
        BLOCK_UNDER_ALL("BLOKK ALL LÁBA ALÁ", 0xFF00897B, false, true, false, false),
        BLOCK_UNDER_PLAYER("BLOKK XY LÁBA ALÁ", 0xFF26A69A, true, true, false, false);

        public final String label;
        public final int color;
        public final boolean needsTarget;
        public final boolean needsValue;
        public final boolean needsReason;
        public final boolean dangerous;

        Type(String label, int color, boolean needsTarget, boolean needsValue,
             boolean needsReason, boolean dangerous) {
            this.label = label;
            this.color = color;
            this.needsTarget = needsTarget;
            this.needsValue = needsValue;
            this.needsReason = needsReason;
            this.dangerous = dangerous;
        }

        public boolean continuous() {
            return this == BLOCK_UNDER_ALL || this == BLOCK_UNDER_PLAYER;
        }
    }

    public static WorkflowBlock of(Type type, String target, String value) {
        String stored = type.needsValue ? normalizeBlockId(value)
                : type.needsReason ? cleanReason(value) : "";
        return new WorkflowBlock(type, type.needsTarget ? clean(target) : "", stored);
    }

    public static WorkflowBlock decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        String[] parts = encoded.split("\\|", 3);
        try {
            Type type = Type.valueOf(parts[0].toUpperCase(Locale.ROOT));
            String target = parts.length > 1 ? parts[1] : "";
            String value = parts.length > 2 ? parts[2] : "";
            if (type.needsTarget && !isValidPlayerName(target)) return null;
            if (type.needsValue && !isValidBlockId(value)) return null;
            if (type.needsReason && value.isBlank()) value = DEFAULT_REASON;
            return of(type, target, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String encode() {
        if (type.needsValue || type.needsReason) return type.name() + "|" + target + "|" + value;
        return type.name() + (type.needsTarget ? "|" + target : "");
    }

    public String label() {
        String result = type.needsTarget ? type.label.replace("XY", target) : type.label;
        return type.needsValue ? result + ": " + value
                : type.needsReason ? result + " — " + reason() : result;
    }

    public String reason() {
        return type.needsReason && !value.isBlank() ? value : DEFAULT_REASON;
    }

    public static boolean isValidBlockId(String value) {
        return value != null && BLOCK_ID.matcher(clean(value).toLowerCase(Locale.ROOT)).matches();
    }

    public static boolean isValidPlayerName(String value) {
        return value != null && PLAYER_NAME.matcher(value).matches();
    }

    public static String normalizeBlockId(String value) {
        String cleaned = clean(value).toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) return "";
        int state = cleaned.indexOf('[');
        String id = state >= 0 ? cleaned.substring(0, state) : cleaned;
        String suffix = state >= 0 ? cleaned.substring(state) : "";
        return (id.contains(":") ? id : "minecraft:" + id) + suffix;
    }

    public static String baseId(String value) {
        String normalized = normalizeBlockId(value);
        int state = normalized.indexOf('[');
        return state >= 0 ? normalized.substring(0, state) : normalized;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replace("|", "").replace("\n", "").replace("\r", "");
    }

    private static String cleanReason(String value) {
        String cleaned = value == null ? "" : value.trim().replace('|', ' ')
                .replace('\n', ' ').replace('\r', ' ');
        if (cleaned.isEmpty()) return DEFAULT_REASON;
        int end = Math.min(cleaned.length(), 120);
        if (end > 0 && Character.isHighSurrogate(cleaned.charAt(end - 1))) end--;
        return cleaned.substring(0, end);
    }
}
