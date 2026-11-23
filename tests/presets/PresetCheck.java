package hu.pelyh.aoptools;

import java.util.List;

public final class PresetCheck {
    public static void main(String[] args) {
        String boundary = "a".repeat(23) + "🌱";
        WorkflowPreset clipped = WorkflowPreset.empty(boundary);
        require(clipped.name().equals("a".repeat(23)), "split Unicode name");
        require(clipped.equals(WorkflowPreset.decode(clipped.encode())), "name changed after reopening");
        String name = "a".repeat(21) + "🌱";
        require(WorkflowPreset.prefix(name, 22).equals("a".repeat(21)), "split before duplicate suffix");

        WorkflowPreset original = new WorkflowPreset("Bázis; ✓", List.of(
                WorkflowBlock.of(WorkflowBlock.Type.BLOCK_UNDER_ALL, "", "stone"),
                WorkflowBlock.of(WorkflowBlock.Type.BLOCK_UNDER_ALL, "", "grass_block")));
        require(original.equals(WorkflowPreset.decode(original.encode())), "block order or names changed");
        WorkflowBlock reason = WorkflowBlock.of(WorkflowBlock.Type.KICK_PLAYER, "Steve", "Saját indok ✓");
        require(reason.equals(WorkflowBlock.decode(reason.encode())), "kick reason changed after reopening");
        require(WorkflowBlock.decode("KICK_PLAYER|Steve").reason().equals(WorkflowBlock.DEFAULT_REASON),
                "old kick block did not receive the compatible default reason");
        require(WorkflowPreset.decode("%%%") == null, "accepted malformed preset");
        require(WorkflowPreset.empty("   ").name().equals("Preset"), "blank name fallback");
        System.out.println("Preset persistence checks passed.");
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
