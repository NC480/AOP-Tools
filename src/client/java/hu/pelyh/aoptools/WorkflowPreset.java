package hu.pelyh.aoptools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public record WorkflowPreset(String name, List<WorkflowBlock> blocks) {
    public WorkflowPreset {
        name = cleanName(name);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
    }

    public static WorkflowPreset empty(String name) {
        return new WorkflowPreset(name, List.of());
    }

    public String encode() {
        StringBuilder result = new StringBuilder(base64(name));
        for (WorkflowBlock block : blocks) result.append(';').append(base64(block.encode()));
        return result.toString();
    }

    public static WorkflowPreset decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String[] parts = encoded.split(";", -1);
            String name = unbase64(parts[0]);
            List<WorkflowBlock> blocks = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                WorkflowBlock block = WorkflowBlock.decode(unbase64(parts[i]));
                if (block != null) blocks.add(block);
            }
            return new WorkflowPreset(name, blocks);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String cleanName(String value) {
        if (value == null) return "Preset";
        String cleaned = value.trim().replace("\n", " ").replace("\r", " ");
        if (cleaned.isEmpty()) return "Preset";
        return cleaned.substring(0, Math.min(24, cleaned.length()));
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String unbase64(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
