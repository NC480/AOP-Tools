package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;

public final class BlockWorkflowScreen extends Screen {
    private static final int BLOCK_HEIGHT = 17;
    private static final int BLOCK_GAP = 3;
    private static final int BODY_TOP = 68;

    private final BlockWorkflowModule module;
    private final OperatorAutomation automation;
    private final Screen parent;
    private final List<WorkflowPreset> presets;
    private List<WorkflowBlock> blocks;
    private int currentPresetIndex;
    private EditBox presetNameBox;
    private EditBox searchBox;
    private EditBox targetBox;
    private EditBox blockBox;
    private Button selfButton;
    private Button igniteButton;
    private DelaySlider delaySlider;
    private Button runButton;
    private Button activeButton;
    private Button autoRunButton;
    private WorkflowBlock dragging;
    private int dragOriginalIndex = -1;
    private int paletteScroll;
    private int sequenceScroll;
    private boolean runArmed;
    private List<String> valueSuggestions = List.of();
    private boolean showValueSuggestions;
    private String notice = "Keresés, majd húzd a blokkot a programba. Jobb kattintás: törlés.";
    private int noticeColor = 0xFFBDBDBD;

    public BlockWorkflowScreen(BlockWorkflowModule module, OperatorAutomation automation, Screen parent) {
        super(Component.literal("AOP Parancs Blokkok"));
        this.module = module;
        this.automation = automation;
        this.parent = parent;
        this.presets = new ArrayList<>(module.presets());
        this.currentPresetIndex = module.activePresetIndex(presets);
        this.blocks = new ArrayList<>(presets.get(currentPresetIndex).blocks());
    }

    @Override
    protected void init() {
        int presetY = 22;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> selectPreset(currentPresetIndex - 1))
                .bounds(12, presetY, 22, 20).build());
        presetNameBox = addRenderableWidget(new EditBox(font, 37, presetY, 105, 20, Component.literal("Preset neve")));
        presetNameBox.setMaxLength(24);
        presetNameBox.setValue(presets.get(currentPresetIndex).name());
        presetNameBox.setResponder(value -> renameCurrentPreset(value));
        addRenderableWidget(Button.builder(Component.literal(">"), button -> selectPreset(currentPresetIndex + 1))
                .bounds(145, presetY, 22, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+ Új"), button -> newPreset())
                .bounds(171, presetY, 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Törlés"), button -> deletePreset())
                .bounds(223, presetY, 52, 20).build());
        activeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            storeCurrentPreset();
            module.activePreset.set(presets.get(currentPresetIndex).name());
            save(); refreshButtons();
        }).bounds(279, presetY, 82, 20).build());
        autoRunButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.autoRun.set(!module.autoRun.get()); refreshButtons();
        }).bounds(365, presetY, Math.max(95, width - 377), 20).build());

        int optionWidth = Math.max(90, Math.min(160, (width - 40) / 3));
        selfButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.excludeSelf.set(!module.excludeSelf.get());
            refreshButtons();
        }).bounds(12, 43, optionWidth, 20).build());
        igniteButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.autoIgnite.set(!module.autoIgnite.get());
            refreshButtons();
        }).bounds(16 + optionWidth, 43, optionWidth, 20).build());
        delaySlider = addRenderableWidget(new DelaySlider(width - 12 - optionWidth, 43, optionWidth, 20));

        int paletteX = 12;
        int paletteWidth = paletteWidth();
        int fieldWidth = paletteWidth - 16;
        searchBox = addRenderableWidget(new EditBox(font, paletteX + 8, BODY_TOP + 18, fieldWidth, 18, Component.literal("Keresés")));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.literal("Keresés"));
        searchBox.setResponder(value -> paletteScroll = 0);

        int halfWidth = (fieldWidth - 4) / 2;
        targetBox = addRenderableWidget(new EditBox(font, paletteX + 8, BODY_TOP + 39, halfWidth, 18, Component.literal("XY játékos")));
        targetBox.setMaxLength(16);
        targetBox.setFilter(value -> value.isEmpty() || OperatorAutomation.isValidPlayerName(value));
        targetBox.setHint(Component.literal("XY név"));

        blockBox = addRenderableWidget(new EditBox(font, paletteX + 12 + halfWidth, BODY_TOP + 39, fieldWidth - halfWidth - 4, 18, Component.literal("Blokk")));
        blockBox.setMaxLength(80);
        blockBox.setHint(Component.literal("tnt / item"));
        blockBox.setResponder(this::updateValueSuggestions);

        int bottomY = height - 24;
        int buttonWidth = Math.max(92, Math.min(145, (width - 32) / 3));
        runButton = addRenderableWidget(Button.builder(Component.empty(), button -> runNow())
                .bounds(12, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Program törlése"), button -> {
            blocks.clear(); save(); runArmed = false;
            notice("A blokkprogram törölve.", ChatFormatting.YELLOW); refreshButtons();
        }).bounds(16 + buttonWidth, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Mentés és vissza"), button -> onClose())
                .bounds(width - 12 - buttonWidth, bottomY, buttonWidth, 20).build());
        refreshButtons();
    }

    private int paletteWidth() { return Math.max(205, Math.min(300, (int) (width * 0.45))); }
    private int bodyBottom() { return height - 46; }
    private int paletteViewportTop() { return BODY_TOP + 61; }
    private int workspaceX() { return 22 + paletteWidth(); }

    private void refreshButtons() {
        selfButton.setMessage(Component.literal("Saját fiók: " + (module.excludeSelf.get() ? "KIHAGY" : "BENNE")));
        igniteButton.setMessage(Component.literal("Auto ignite: " + (module.autoIgnite.get() ? "BE" : "KI")));
        String current = presets.get(currentPresetIndex).name();
        activeButton.setMessage(Component.literal(current.equalsIgnoreCase(module.activePreset.get()) ? "AKTÍV ✓" : "Aktívvá tesz"));
        autoRunButton.setMessage(Component.literal("Autorun: " + (module.autoRun.get() ? "BE" : "KI")));
        delaySlider.refreshFromSetting();
        runButton.setMessage(Component.literal(runArmed ? "BIZTOSAN FUTTATOD?" : "Futtatás / leállítás"));
    }

    private void runNow() {
        save();
        module.activePreset.set(presets.get(currentPresetIndex).name());
        if (module.containsDangerousBlock() && !runArmed) {
            runArmed = true;
            notice("KILL/KICK/BAN van a programban. Kattints még egyszer!", ChatFormatting.RED);
            refreshButtons();
            return;
        }
        OperatorAutomation.Result result = automation.runWorkflow(minecraft, true);
        runArmed = false;
        notice(result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        refreshButtons();
    }

    private List<WorkflowBlock.Type> filteredTypes() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(WorkflowBlock.Type.values())
                .filter(type -> query.isEmpty() || type.label.toLowerCase(Locale.ROOT).contains(query)
                        || type.name().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void updateValueSuggestions(String typed) {
        String query = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            valueSuggestions = List.of();
            showValueSuggestions = false;
            return;
        }

        LinkedHashSet<String> matches = new LinkedHashSet<>();
        addRegistryMatches(matches, BuiltInRegistries.BLOCK.keySet(), query);
        addRegistryMatches(matches, BuiltInRegistries.ITEM.keySet(), query);
        valueSuggestions = matches.stream().limit(6).toList();
        showValueSuggestions = !valueSuggestions.isEmpty();
    }

    private static void addRegistryMatches(LinkedHashSet<String> matches,
                                           Iterable<?> ids,
                                           String query) {
        for (Object id : ids) {
            String full = id.toString();
            int colon = full.indexOf(':');
            String path = colon >= 0 ? full.substring(colon + 1) : full;
            if (full.startsWith(query) || path.startsWith(query)) matches.add(full);
        }
    }

    private void storeCurrentPreset() {
        String oldName = presets.get(currentPresetIndex).name();
        String requested = presetNameBox == null ? oldName : presetNameBox.getValue();
        String name = uniqueName(WorkflowPreset.cleanName(requested), currentPresetIndex);
        presets.set(currentPresetIndex, new WorkflowPreset(name, blocks));
        if (oldName.equalsIgnoreCase(module.activePreset.get())) module.activePreset.set(name);
        if (presetNameBox != null && !presetNameBox.getValue().equals(name)) presetNameBox.setValue(name);
    }

    private void save() {
        storeCurrentPreset();
        module.savePresets(presets);
    }

    private void renameCurrentPreset(String requested) {
        if (requested == null || requested.isBlank()) return;
        String oldName = presets.get(currentPresetIndex).name();
        String newName = uniqueName(WorkflowPreset.cleanName(requested), currentPresetIndex);
        presets.set(currentPresetIndex, new WorkflowPreset(newName, blocks));
        if (oldName.equalsIgnoreCase(module.activePreset.get())) module.activePreset.set(newName);
    }

    private String uniqueName(String base, int ignoredIndex) {
        String candidate = base;
        int suffix = 2;
        while (nameExists(candidate, ignoredIndex)) {
            String ending = " " + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 24 - ending.length())) + ending;
        }
        return candidate;
    }

    private boolean nameExists(String name, int ignoredIndex) {
        for (int i = 0; i < presets.size(); i++) {
            if (i != ignoredIndex && presets.get(i).name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void selectPreset(int wantedIndex) {
        storeCurrentPreset();
        currentPresetIndex = Math.floorMod(wantedIndex, presets.size());
        blocks = new ArrayList<>(presets.get(currentPresetIndex).blocks());
        presetNameBox.setValue(presets.get(currentPresetIndex).name());
        sequenceScroll = 0; runArmed = false; save(); refreshButtons();
        notice("Preset kiválasztva: " + presets.get(currentPresetIndex).name(), ChatFormatting.AQUA);
    }

    private void newPreset() {
        storeCurrentPreset();
        String name = uniqueName("Új preset", -1);
        presets.add(WorkflowPreset.empty(name));
        currentPresetIndex = presets.size() - 1;
        blocks = new ArrayList<>();
        presetNameBox.setValue(name);
        sequenceScroll = 0; save(); refreshButtons();
        notice("Új üres preset-konténer létrehozva.", ChatFormatting.GREEN);
    }

    private void deletePreset() {
        if (presets.size() == 1) {
            notice("Legalább egy presetnek maradnia kell.", ChatFormatting.RED); return;
        }
        String removed = presets.remove(currentPresetIndex).name();
        currentPresetIndex = Math.min(currentPresetIndex, presets.size() - 1);
        blocks = new ArrayList<>(presets.get(currentPresetIndex).blocks());
        presetNameBox.setValue(presets.get(currentPresetIndex).name());
        if (removed.equalsIgnoreCase(module.activePreset.get())) module.activePreset.set(presets.get(currentPresetIndex).name());
        sequenceScroll = 0; save(); refreshButtons();
        notice("Preset törölve: " + removed, ChatFormatting.YELLOW);
    }
    private void notice(String message, ChatFormatting color) {
        notice = message;
        noticeColor = color.getColor() == null ? 0xFFFFFFFF : 0xFF000000 | color.getColor();
    }

    @Override public void onClose() { save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF0B0E14);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        int paletteX = 12, paletteWidth = paletteWidth(), bodyBottom = bodyBottom();
        int workspaceX = workspaceX(), workspaceWidth = width - workspaceX - 12;
        graphics.fill(paletteX, BODY_TOP, paletteX + paletteWidth, bodyBottom, 0xCC171A20);
        graphics.renderOutline(paletteX, BODY_TOP, paletteWidth, bodyBottom - BODY_TOP, 0xFF4B5563);
        graphics.fill(workspaceX, BODY_TOP, workspaceX + workspaceWidth, bodyBottom, 0xCC111827);
        graphics.renderOutline(workspaceX, BODY_TOP, workspaceWidth, bodyBottom - BODY_TOP, 0xFF4B5563);

        List<WorkflowBlock.Type> types = filteredTypes();
        int viewportTop = paletteViewportTop();
        clampPaletteScroll(types.size());
        graphics.drawString(font, "BLOKKOK • " + types.size() + " találat", paletteX + 8, BODY_TOP + 6, 0xFFFFFFFF);
        String presetTitle = "PRESET-KONTÉNER: " + presets.get(currentPresetIndex).name();
        if (presets.get(currentPresetIndex).name().equalsIgnoreCase(module.activePreset.get())) presetTitle += "  [AKTÍV]";
        graphics.drawString(font, presetTitle, workspaceX + 8, BODY_TOP + 7, 0xFFFFFFFF);
        graphics.enableScissor(paletteX + 1, viewportTop, paletteX + paletteWidth - 1, bodyBottom - 1);
        int paletteY = viewportTop + 2;
        for (int index = paletteScroll; index < types.size() && paletteY < bodyBottom; index++) {
            WorkflowBlock block = preview(types.get(index));
            drawBlock(graphics, paletteX + 8, paletteY, paletteWidth - 16, block, false);
            paletteY += blockHeight(block, paletteWidth - 16, false) + BLOCK_GAP;
        }
        graphics.disableScissor();

        int sequenceY = BODY_TOP + 24;
        clampSequenceScroll();
        int blockY = sequenceY;
        for (int index = sequenceScroll; index < blocks.size() && blockY < bodyBottom; index++) {
            WorkflowBlock block = blocks.get(index);
            drawBlock(graphics, workspaceX + 8, blockY, workspaceWidth - 16, block, true);
            blockY += blockHeight(block, workspaceWidth - 16) + BLOCK_GAP;
        }
        if (blocks.isEmpty()) graphics.drawCenteredString(font, "Húzz ide blokkokat", workspaceX + workspaceWidth / 2,
                BODY_TOP + 55, 0xFF7C8799);
        graphics.drawString(font, notice, 12, height - 41, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderValueSuggestions(graphics);
        if (dragging != null) drawBlock(graphics, mouseX - 85, mouseY - BLOCK_HEIGHT / 2, 170, dragging, true);
    }

    private WorkflowBlock preview(WorkflowBlock.Type type) {
        String target = type.needsTarget ? (targetBox.getValue().isBlank() ? "XY" : targetBox.getValue()) : "";
        String value = type.needsValue ? (blockBox.getValue().isBlank() ? "minecraft:tnt" : blockBox.getValue()) : "";
        return WorkflowBlock.of(type, target, value);
    }

    private void drawBlock(GuiGraphics graphics, int x, int y, int blockWidth, WorkflowBlock block, boolean sequence) {
        int color = block.type().color;
        List<String> lines = blockLines(block, blockWidth, sequence);
        int actualWidth = 58;
        for (String line : lines) actualWidth = Math.max(actualWidth, font.width(line) + 12);
        actualWidth = Math.min(blockWidth, actualWidth);
        int height = lines.size() > 1 ? 28 : BLOCK_HEIGHT;
        graphics.fill(x, y, x + actualWidth, y + height, color);
        graphics.renderOutline(x, y, actualWidth, height, 0xDDFFFFFF);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + 6, y + 5 + i * 11, 0xFFFFFFFF, true);
        }
    }

    private int blockHeight(WorkflowBlock block, int maxWidth) {
        return blockLines(block, maxWidth, true).size() > 1 ? 28 : BLOCK_HEIGHT;
    }

    private List<String> blockLines(WorkflowBlock block, int maxWidth, boolean sequence) {
        int textWidth = Math.max(24, maxWidth - 12);
        String prefix = sequence ? "⋮⋮ " : "+ ";
        String action = block.type().needsTarget
                ? block.type().label.replace("XY", block.target())
                : block.type().label;
        String first = prefix + action;
        if (block.type().needsValue) {
            String combined = first + ": " + block.value();
            if (font.width(combined) <= textWidth) return List.of(combined);
            return List.of(fitText(first, textWidth), fitText(block.value(), textWidth));
        }
        if (font.width(first) <= textWidth) return List.of(first);

        int split = first.lastIndexOf(' ');
        while (split > 0 && font.width(first.substring(0, split)) > textWidth) {
            split = first.lastIndexOf(' ', split - 1);
        }
        if (split <= 0) return List.of(fitText(first, textWidth));
        return List.of(fitText(first.substring(0, split), textWidth),
                fitText(first.substring(split + 1), textWidth));
    }

    private String fitText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return text.substring(0, end) + suffix;
    }

    private void renderValueSuggestions(GuiGraphics graphics) {
        if (!showValueSuggestions || !blockBox.isFocused()) return;
        int x = blockBox.getX();
        int y = blockBox.getY() + blockBox.getHeight() + 1;
        int maxRight = 12 + paletteWidth() - 8;
        int suggestionWidth = blockBox.getWidth();
        for (String suggestion : valueSuggestions) {
            suggestionWidth = Math.max(suggestionWidth, font.width(suggestion) + 8);
        }
        suggestionWidth = Math.min(suggestionWidth, maxRight - x);
        int height = valueSuggestions.size() * 12 + 2;
        graphics.fill(x, y, x + suggestionWidth, y + height, 0xF2181C24);
        graphics.renderOutline(x, y, suggestionWidth, height, 0xFF7C8799);
        for (int i = 0; i < valueSuggestions.size(); i++) {
            graphics.drawString(font, fitText(valueSuggestions.get(i), suggestionWidth - 8),
                    x + 4, y + 3 + i * 12, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int paletteWidth = paletteWidth(), bodyBottom = bodyBottom(), viewportTop = paletteViewportTop();
        if (event.button() == 0 && showValueSuggestions && blockBox.isFocused()) {
            int suggestionY = blockBox.getY() + blockBox.getHeight() + 1;
            int row = ((int) my - suggestionY - 2) / 12;
            if (mx >= blockBox.getX() && mx <= 12 + paletteWidth - 8
                    && my >= suggestionY && row >= 0 && row < valueSuggestions.size()) {
                String selected = valueSuggestions.get(row);
                blockBox.setValue(selected);
                blockBox.setCursorPosition(selected.length());
                showValueSuggestions = false;
                return true;
            }
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        List<WorkflowBlock.Type> types = filteredTypes();
        if (event.button() == 0 && mx >= 20 && mx < 12 + paletteWidth - 8 && my >= viewportTop && my < bodyBottom) {
            WorkflowBlock.Type clickedType = null;
            int y = viewportTop + 2;
            for (int i = paletteScroll; i < types.size() && y < bodyBottom; i++) {
                WorkflowBlock block = preview(types.get(i));
                int blockHeight = blockHeight(block, paletteWidth - 16, false);
                if (my >= y && my < y + blockHeight) {
                    clickedType = types.get(i);
                    break;
                }
                y += blockHeight + BLOCK_GAP;
            }
            if (clickedType != null) {
                WorkflowBlock.Type type = clickedType;
                String target = targetBox.getValue().trim();
                String value = blockBox.getValue().trim();
                if (type.needsTarget && !OperatorAutomation.isValidPlayerName(target)) {
                    targetBox.setFocused(true); notice("Előbb adj meg érvényes XY játékosnevet.", ChatFormatting.RED); return true;
                }
                if (type.needsValue && !WorkflowBlock.isValidBlockId(value)) {
                    blockBox.setFocused(true); notice("Adj meg blokkazonosítót, például: tnt vagy minecraft:stone", ChatFormatting.RED); return true;
                }
                dragging = WorkflowBlock.of(type, target, value); dragOriginalIndex = -1; setDragging(true); return true;
            }
        }
        int index = sequenceIndexAt(mx, my);
        if (index >= 0 && index < blocks.size()) {
            if (event.button() == 1) {
                blocks.remove(index); save(); runArmed = false; refreshButtons();
                notice("Blokk törölve.", ChatFormatting.YELLOW); return true;
            }
            if (event.button() == 0) {
                dragging = blocks.remove(index); dragOriginalIndex = index; setDragging(true); return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            if (event.x() >= workspaceX() && event.x() <= width - 12 && event.y() >= BODY_TOP && event.y() <= bodyBottom()) {
                blocks.add(Math.min(insertionIndex(event.y()), blocks.size()), dragging);
                notice("Blokk hozzáadva és mentve.", ChatFormatting.GREEN);
            } else if (dragOriginalIndex >= 0) blocks.add(Math.min(dragOriginalIndex, blocks.size()), dragging);
            dragging = null; dragOriginalIndex = -1; setDragging(false); runArmed = false; save(); refreshButtons(); return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= 12 && mouseX < 12 + paletteWidth() && mouseY >= BODY_TOP && mouseY <= bodyBottom()) {
            paletteScroll -= (int) Math.signum(verticalAmount);
            clampPaletteScroll(filteredTypes().size()); return true;
        }
        if (mouseX >= workspaceX() && mouseX <= width - 12 && mouseY >= BODY_TOP && mouseY <= bodyBottom()) {
            sequenceScroll -= (int) Math.signum(verticalAmount); clampSequenceScroll(); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int sequenceIndexAt(double mx, double my) {
        if (mx < workspaceX() + 8 || mx > width - 20) return -1;
        int y = BODY_TOP + 24;
        int maxWidth = width - workspaceX() - 28;
        for (int i = sequenceScroll; i < blocks.size() && y < bodyBottom(); i++) {
            int height = blockHeight(blocks.get(i), maxWidth, true);
            if (my >= y && my < y + height) return i;
            y += height + BLOCK_GAP;
        }
        return -1;
    }
    private int insertionIndex(double my) {
        int y = BODY_TOP + 24;
        int maxWidth = width - workspaceX() - 28;
        for (int i = sequenceScroll; i < blocks.size(); i++) {
            int height = blockHeight(blocks.get(i), maxWidth, true);
            if (my < y + height / 2.0) return i;
            y += height + BLOCK_GAP;
        }
        return blocks.size();
    }
    private int blockHeight(WorkflowBlock block, int maxWidth, boolean sequence) {
        return blockLines(block, maxWidth, sequence).size() > 1 ? 28 : BLOCK_HEIGHT;
    }
    private void clampSequenceScroll() { sequenceScroll = Math.max(0, Math.min(sequenceScroll, Math.max(0, blocks.size() - 1))); }
    private void clampPaletteScroll(int count) { paletteScroll = Math.max(0, Math.min(paletteScroll, Math.max(0, count - 1))); }

    private final class DelaySlider extends AbstractSliderButton {
        private static final int MIN_TICKS = 1, MAX_TICKS = 200;
        private DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), toValue(module.commandDelayTicks.get())); updateMessage();
        }
        @Override protected void updateMessage() {
            double seconds = module.commandDelayTicks.get() / 20.0;
            setMessage(Component.literal("Ismétlés: " + String.format(Locale.ROOT, seconds < 1 ? "%.2f" : "%.1f", seconds) + " mp"));
        }
        @Override protected void applyValue() {
            module.commandDelayTicks.set(MIN_TICKS + (int) Math.round(value * (MAX_TICKS - MIN_TICKS))); updateMessage();
        }
        private void refreshFromSetting() { value = toValue(module.commandDelayTicks.get()); updateMessage(); }
        private static double toValue(int ticks) {
            int clamped = Math.max(MIN_TICKS, Math.min(MAX_TICKS, ticks));
            return (clamped - MIN_TICKS) / (double) (MAX_TICKS - MIN_TICKS);
        }
    }
}
