package hu.pelyh.aoptools;

import java.util.Locale;

public record WorkflowBlock(Type type, String target) {
    public enum Type {
        OP_ALL("OP ALL", 0xFF4CAF50, false, false),
        DEOP_ALL("deOP ALL", 0xFFFFA726, false, false),
        OP_PLAYER("OP XY", 0xFF42A5F5, true, false),
        DEOP_PLAYER("deOP XY", 0xFFAB47BC, true, false),
        KILL_ALL("KILL ALL", 0xFFE53935, false, true),
        KILL_PLAYER("KILL XY", 0xFFF06292, true, true),
        KICK_ALL("KICK ALL", 0xFFEF5350, false, true),
        KICK_PLAYER("KICK XY", 0xFFFF7043, true, true),
        BAN_ALL("BAN ALL", 0xFFD32F2F, false, true),
        BAN_PLAYER("BAN XY", 0xFFB71C1C, true, true);

        public final String label;
        public final int color;
        public final boolean needsTarget;
        public final boolean dangerous;

        Type(String label, int color, boolean needsTarget, boolean dangerous) {
            this.label = label;
            this.color = color;
            this.needsTarget = needsTarget;
            this.dangerous = dangerous;
        }
    }

    public static WorkflowBlock of(Type type, String target) {
        String cleaned = target == null ? "" : target.trim();
        return new WorkflowBlock(type, type.needsTarget ? cleaned : "");
    }

    public static WorkflowBlock decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        String[] parts = encoded.split("\\|", 2);
        try {
            Type type = Type.valueOf(parts[0].toUpperCase(Locale.ROOT));
            String target = parts.length == 2 ? parts[1] : "";
            if (type.needsTarget && !OperatorAutomation.isValidPlayerName(target)) return null;
            return of(type, target);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String encode() {
        return type.name() + (type.needsTarget ? "|" + target : "");
    }

    public String label() {
        return type.needsTarget ? type.label.replace("XY", target) : type.label;
    }
}
