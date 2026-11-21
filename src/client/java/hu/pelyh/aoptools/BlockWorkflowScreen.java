package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
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
    private EditorLayout layout;

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
    private boolean displayedBusy;
    private List<String> valueSuggestions = List.of();
    private boolean showValueSuggestions;
    private int suggestionScroll;
    private int suggestionSelection;
    private static final int SUGGESTION_HEIGHT = 14;
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
        layout = new EditorLayout(width, height);
        String search = searchBox == null ? "" : searchBox.getValue();
        String target = targetBox == null ? "" : targetBox.getValue();
        String item = blockBox == null ? "" : blockBox.getValue();
        int presetY = 22;
        int nav = layout.navWidth();
        int nameX = 16 + nav;
        int nextX = nameX + layout.presetNameWidth() + 4;
        int addX = nextX + nav + 4;
        int deleteX = addX + layout.addWidth() + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> selectPreset(currentPresetIndex - 1))
                .bounds(12, presetY, nav, 18).build());
        presetNameBox = addRenderableWidget(new EditBox(font, nameX, presetY, layout.presetNameWidth(), 18, Component.literal("Preset neve")));
        presetNameBox.setMaxLength(24);
        presetNameBox.setValue(presets.get(currentPresetIndex).name());
        presetNameBox.setResponder(value -> renameCurrentPreset(value));
        addRenderableWidget(Button.builder(Component.literal(">"), button -> selectPreset(currentPresetIndex + 1))
                .bounds(nextX, presetY, nav, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+ Új"), button -> newPreset())
                .bounds(addX, presetY, layout.addWidth(), 18).build());
        addRenderableWidget(Button.builder(Component.literal("Törlés"), button -> confirmRemoval(false))
                .tooltip(Tooltip.create(Component.literal("Kiválasztott preset törlése")))
                .bounds(deleteX, presetY, layout.deleteWidth(), 18).build());
        activeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            storeCurrentPreset();
            module.activePreset.set(presets.get(currentPresetIndex).name());
            save(); refreshButtons();
        }).bounds(deleteX + layout.deleteWidth() + 4, presetY, layout.activeWidth(), 18).build());
        autoRunButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.autoRun.set(!module.autoRun.get()); refreshButtons();
        }).bounds(layout.autoRunX(), presetY, layout.autoRunWidth(), 18).build());

        int optionWidth = (width - 32) / 3;
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
        searchBox = addRenderableWidget(new EditBox(font, paletteX + 8, bodyTop() + 18, fieldWidth, 18, Component.literal("Keresés")));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.literal("Keresés"));
        searchBox.setResponder(value -> paletteScroll = 0);
        searchBox.setValue(search);

        int halfWidth = (fieldWidth - 4) / 2;
        targetBox = addRenderableWidget(new EditBox(font, paletteX + 8, bodyTop() + 39, halfWidth, 18, Component.literal("XY játékos")));
        targetBox.setMaxLength(16);
        targetBox.setFilter(value -> value.isEmpty() || OperatorAutomation.isValidPlayerName(value));
        targetBox.setHint(Component.literal("XY név"));
        targetBox.setValue(target);

        blockBox = addRenderableWidget(new EditBox(font, paletteX + 12 + halfWidth, bodyTop() + 39, fieldWidth - halfWidth - 4, 18, Component.literal("Blokk")));
        blockBox.setMaxLength(80);
        blockBox.setHint(Component.literal("Blokk"));
        blockBox.setResponder(this::updateValueSuggestions);
        blockBox.setValue(item);

        int bottomY = height - 24;
        int buttonWidth = Math.max(92, Math.min(145, (width - 32) / 3));
        runButton = addRenderableWidget(Button.builder(Component.empty(), button -> runNow())
                .bounds(12, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Blokkok ürítése"), button -> confirmRemoval(true))
                .bounds(16 + buttonWidth, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Mentés és vissza"), button -> onClose())
                .bounds(width - 12 - buttonWidth, bottomY, buttonWidth, 20).build());
        refreshButtons();
    }

    private int paletteWidth() { return layout.paletteWidth(); }
    private int bodyTop() { return layout.bodyTop(); }
    private int bodyBottom() { return layout.bodyBottom(); }
    private int paletteViewportTop() { return bodyTop() + 61; }
    private int workspaceX() { return layout.workspaceX(); }

    private void refreshButtons() {
        selfButton.setMessage(Component.literal("Saját fiók: " + (module.excludeSelf.get() ? "ki" : "be")));
        igniteButton.setMessage(Component.literal("Auto ignite: " + (module.autoIgnite.get() ? "BE" : "KI")));
        String current = presets.get(currentPresetIndex).name();
        activeButton.setMessage(Component.literal(current.equalsIgnoreCase(module.activePreset.get()) ? "Aktív ✓" : layout.compact() ? "Aktivál" : "Aktívvá tesz"));
        autoRunButton.setMessage(Component.literal("Autorun: " + (module.autoRun.get() ? "BE" : "KI")));
        delaySlider.refreshFromSetting();
        displayedBusy = automation.isBusy();
        runButton.setMessage(Component.literal(displayedBusy ? "Leállítás" : runArmed ? "Megerősítés" : "Futtatás"));
    }

    private void runNow() {
        if (automation.isBusy()) {
            automation.cancelQueuedCommands();
            runArmed = false;
            notice("Futtatás leállítva.", ChatFormatting.YELLOW);
            refreshButtons();
            return;
        }
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
        valueSuggestions = matches.stream().sorted().toList();
        suggestionScroll = 0;
        suggestionSelection = 0;
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

    private void confirmRemoval(boolean onlyBlocks) {
        if (onlyBlocks && blocks.isEmpty()) return;
        if (!onlyBlocks && presets.size() == 1) {
            notice("Legalább egy presetnek maradnia kell.", ChatFormatting.YELLOW);
            return;
        }
        String name = presets.get(currentPresetIndex).name();
        Component question = Component.literal(onlyBlocks ? "Törlöd a preset blokkjait?" : "Törlöd ezt a presetet?");
        minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
            minecraft.setScreen(this);
            if (!confirmed) return;
            if (!onlyBlocks) {
                deletePreset();
                return;
            }
            blocks.clear();
            runArmed = false;
            save();
            refreshButtons();
            notice("A blokkprogram törölve.", ChatFormatting.YELLOW);
        }, question, Component.literal(name + " · " + blocks.size() + " blokk")));
    }
    private void notice(String message, ChatFormatting color) {
        notice = message;
        noticeColor = color.getColor() == null ? 0xFFFFFFFF : 0xFF000000 | color.getColor();
    }

    @Override public void onClose() { save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (displayedBusy != automation.isBusy()) refreshButtons();
        graphics.fill(0, 0, width, height, 0xFF101722);
        graphics.fill(12, 6, 15, 16, 0xFF62D5BF);
        graphics.drawString(font, "Blokkszerkesztő", 20, 7, 0xFFF2F6FA);
        String counter = (currentPresetIndex + 1) + " / " + presets.size() + " preset";
        graphics.drawString(font, counter, width - 12 - font.width(counter), 7, 0xFFA2B3C6);
        int paletteX = 12, paletteWidth = paletteWidth(), bodyBottom = bodyBottom();
        int workspaceX = workspaceX(), workspaceWidth = width - workspaceX - 12;
        int top = bodyTop();
        graphics.fill(paletteX, top, paletteX + paletteWidth, bodyBottom, 0xFF192332);
        graphics.fill(workspaceX, top, workspaceX + workspaceWidth, bodyBottom, 0xFF141E2C);
        graphics.fill(paletteX, top, paletteX + paletteWidth, top + 1, 0xFF547884);
        graphics.fill(workspaceX, top, workspaceX + workspaceWidth, top + 1, 0xFF62D5BF);

        List<WorkflowBlock.Type> types = filteredTypes();
        int viewportTop = paletteViewportTop();
        clampPaletteScroll(types.size());
        graphics.drawString(font, "Paletta · " + types.size(), paletteX + 8, top + 6, 0xFFB8DBDE);
        String presetTitle = presets.get(currentPresetIndex).name() + " · " + blocks.size() + " blokk";
        graphics.drawString(font, fitText(presetTitle, workspaceWidth - 16), workspaceX + 8, top + 7, 0xFFC6EDE7);
        graphics.enableScissor(paletteX + 1, viewportTop, paletteX + paletteWidth - 1, bodyBottom - 1);
        int paletteY = viewportTop + 2;
        for (int index = paletteScroll; index < types.size() && paletteY < bodyBottom; index++) {
            WorkflowBlock block = preview(types.get(index));
            drawBlock(graphics, paletteX + 8, paletteY, paletteWidth - 16, block, false);
            paletteY += blockHeight(block, paletteWidth - 16, false) + BLOCK_GAP;
        }
        graphics.disableScissor();

        int sequenceY = top + 24;
        clampSequenceScroll();
        graphics.enableScissor(workspaceX + 1, sequenceY, width - 13, bodyBottom - 1);
        int blockY = sequenceY;
        for (int index = sequenceScroll; index < blocks.size() && blockY < bodyBottom; index++) {
            WorkflowBlock block = blocks.get(index);
            drawBlock(graphics, workspaceX + 8, blockY, workspaceWidth - 16, block, true);
            blockY += blockHeight(block, workspaceWidth - 16) + BLOCK_GAP;
        }
        if (blocks.isEmpty()) graphics.drawCenteredString(font, "Húzz ide egy blokkot", workspaceX + workspaceWidth / 2,
                sequenceY + 10, 0xFFA2B3C6);
        if (dragging != null && mouseX >= workspaceX && mouseX < width - 12 && mouseY >= sequenceY && mouseY < bodyBottom) {
            int marker = Math.min(bodyBottom - 3, insertionY(mouseY));
            graphics.fill(workspaceX + 7, marker, width - 19, marker + 2, 0xFF62D5BF);
        }
        graphics.disableScissor();
        graphics.drawString(font, fitText(notice, width - 24), 12, height - 40, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderValueSuggestions(graphics, mouseX, mouseY);
        if (dragging != null) drawBlock(graphics, mouseX - 85, mouseY - BLOCK_HEIGHT / 2, 170, dragging, true);
    }

    private WorkflowBlock preview(WorkflowBlock.Type type) {
        String target = type.needsTarget ? (targetBox.getValue().isBlank() ? "XY" : targetBox.getValue()) : "";
        String value = type.needsValue ? (blockBox.getValue().isBlank() ? "minecraft:tnt" : blockBox.getValue()) : "";
        return WorkflowBlock.of(type, target, value);
    }

    private record Chip(List<String> lines, int width, int height) { }

    private Chip chip(WorkflowBlock block, int maxWidth, boolean sequence) {
        List<String> lines = blockLines(block, maxWidth, sequence);
        int measured = 0;
        for (String line : lines) measured = Math.max(measured, font.width(line));
        return new Chip(lines, Math.min(maxWidth, measured + 16),
                lines.size() > 1 ? 28 : BLOCK_HEIGHT);
    }

    private void drawBlock(GuiGraphics graphics, int x, int y, int blockWidth, WorkflowBlock block, boolean sequence) {
        Chip chip = chip(block, blockWidth, sequence);
        int right = x + chip.width(), bottom = y + chip.height();
        int accent = block.type().color;
        graphics.fill(x + 2, y, right - 2, bottom, 0xFF37455A);
        graphics.fill(x, y + 2, right, bottom - 2, 0xFF37455A);
        graphics.fill(x + 1, y + 2, right - 1, bottom - 2, 0xFF202C3C);
        graphics.fill(x + 1, y + 3, x + 4, bottom - 3, accent);
        for (int i = 0; i < chip.lines().size(); i++) {
            graphics.drawString(font, chip.lines().get(i), x + 9, y + 4 + i * 11,
                    i == 0 ? 0xFFF1F5F9 : 0xFFB5C7D9, false);
        }
    }

    private int blockHeight(WorkflowBlock block, int maxWidth) {
        return blockLines(block, maxWidth, true).size() > 1 ? 28 : BLOCK_HEIGHT;
    }

    private List<String> blockLines(WorkflowBlock block, int maxWidth, boolean sequence) {
        int textWidth = Math.max(0, maxWidth - 16);
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
        int available = maxWidth - font.width("…");
        return available >= 0 ? font.plainSubstrByWidth(text, available) + "…" : "";
    }

    private record Popup(int x, int y, int width, int rows) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + 2 + rows * SUGGESTION_HEIGHT;
        }

        int rowAt(double mx, double my) {
            if (!contains(mx, my) || my < y + 1) return -1;
            int row = ((int) my - y - 1) / SUGGESTION_HEIGHT;
            return row < rows ? row : -1;
        }
    }

    private Popup suggestionPopup() {
        if (!showValueSuggestions || !blockBox.isFocused() || valueSuggestions.isEmpty()) return null;
        int right = blockBox.getX() + blockBox.getWidth();
        int popupWidth = Math.min(right - 12, Math.max(blockBox.getWidth(), paletteWidth() - 16));
        int belowY = blockBox.getY() + blockBox.getHeight() + 1;
        int below = Math.max(0, (bodyBottom() - belowY - 2) / SUGGESTION_HEIGHT);
        int above = Math.max(0, (blockBox.getY() - 5) / SUGGESTION_HEIGHT);
        boolean flip = below < Math.min(valueSuggestions.size(), 3) && above > below;
        int rows = Math.min(Math.min(6, valueSuggestions.size()), flip ? above : below);
        if (rows <= 0) return null;
        int y = flip ? blockBox.getY() - 3 - rows * SUGGESTION_HEIGHT : belowY;
        suggestionScroll = Math.clamp(suggestionScroll, 0, valueSuggestions.size() - rows);
        return new Popup(right - popupWidth, y, popupWidth, rows);
    }

    private void renderValueSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        Popup popup = suggestionPopup();
        if (popup == null) return;
        int x = popup.x(), y = popup.y(), w = popup.width(), rows = popup.rows();
        int h = rows * SUGGESTION_HEIGHT + 2;
        int hovered = popup.rowAt(mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, 0xFF152334);
        graphics.renderOutline(x, y, w, h, 0xFF4F7789);
        for (int i = 0; i < rows; i++) {
            int index = suggestionScroll + i;
            int top = y + 1 + i * SUGGESTION_HEIGHT;
            if (i == hovered || index == suggestionSelection) {
                graphics.fill(x + 1, top, x + w - 3, top + SUGGESTION_HEIGHT,
                        i == hovered ? 0xFF305163 : 0xFF233B4C);
            }
            String id = valueSuggestions.get(index);
            String label = id.startsWith("minecraft:") ? id.substring(10) : id;
            graphics.drawString(font, fitText(label, w - 13), x + 5, top + 3, 0xFFF1F5F9, false);
        }
        if (rows < valueSuggestions.size()) {
            int track = h - 2, thumb = Math.max(3, track * rows / valueSuggestions.size());
            int top = y + 1 + (track - thumb) * suggestionScroll / (valueSuggestions.size() - rows);
            graphics.fill(x + w - 3, top, x + w - 1, top + thumb, 0xFF6FBDCA);
        }
    }

    private void chooseSuggestion(int index) {
        String selected = valueSuggestions.get(index);
        blockBox.setValue(selected);
        blockBox.setCursorPosition(selected.length());
        showValueSuggestions = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (dragging != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                dragging = null;
                dragOriginalIndex = -1;
                setDragging(false);
            }
            return true;
        }
        Popup popup = suggestionPopup();
        if (popup != null) {
            switch (event.key()) {
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> {
                    int step = event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1;
                    suggestionSelection = Math.clamp(suggestionSelection + step, 0, valueSuggestions.size() - 1);
                    suggestionScroll = Math.clamp(suggestionScroll,
                            suggestionSelection - popup.rows() + 1, suggestionSelection);
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    chooseSuggestion(suggestionSelection);
                    return true;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    showValueSuggestions = false;
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int paletteWidth = paletteWidth(), bodyBottom = bodyBottom(), viewportTop = paletteViewportTop();
        if (dragging != null) return true;
        Popup popup = suggestionPopup();
        if (popup != null && popup.contains(mx, my)) {
            int row = popup.rowAt(mx, my);
            if (row >= 0 && event.button() == 0) chooseSuggestion(suggestionScroll + row);
            return true;
        }
        if (!blockBox.isMouseOver(mx, my)) showValueSuggestions = false;
        if (super.mouseClicked(event, doubleClick)) return true;
        List<WorkflowBlock.Type> types = filteredTypes();
        if (event.button() == 0 && mx >= 20 && mx < 12 + paletteWidth - 8 && my >= viewportTop && my < bodyBottom - 1) {
            WorkflowBlock.Type clickedType = null;
            int y = viewportTop + 2;
            for (int i = paletteScroll; i < types.size() && y < bodyBottom; i++) {
                WorkflowBlock block = preview(types.get(i));
                Chip visible = chip(block, paletteWidth - 16, false);
                if (my >= y && my < y + visible.height() && mx < 20 + visible.width()) {
                    clickedType = types.get(i);
                    break;
                }
                y += visible.height() + BLOCK_GAP;
            }
            if (clickedType != null) {
                WorkflowBlock.Type type = clickedType;
                String target = targetBox.getValue().trim();
                String value = blockBox.getValue().trim();
                if (type.needsTarget && !OperatorAutomation.isValidPlayerName(target)) {
                    setFocused(targetBox); notice("Előbb adj meg érvényes XY játékosnevet.", ChatFormatting.RED); return true;
                }
                if (type.needsValue && !WorkflowBlock.isValidBlockId(value)) {
                    setFocused(blockBox); notice("Adj meg blokkazonosítót, például: tnt vagy minecraft:stone", ChatFormatting.RED); return true;
                }
                beginDrag(WorkflowBlock.of(type, target, value), -1);
                return true;
            }
        }
        int index = sequenceIndexAt(mx, my);
        if (index >= 0 && index < blocks.size()) {
            if (event.button() == 1) {
                blocks.remove(index); save(); runArmed = false; refreshButtons();
                notice("Blokk törölve.", ChatFormatting.YELLOW); return true;
            }
            if (event.button() == 0) {
                beginDrag(blocks.get(index), index);
                return true;
            }
        }
        return false;
    }

    private void beginDrag(WorkflowBlock block, int source) {
        setFocused(null);
        dragging = block;
        dragOriginalIndex = source;
        setDragging(true);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging == null || event.button() != 0) return super.mouseReleased(event);
        if (event.x() >= workspaceX() && event.x() < width - 12
                && event.y() >= bodyTop() + 24 && event.y() < bodyBottom() - 1) {
            int dest = insertionIndex(event.y());
            boolean moving = dragOriginalIndex >= 0;
            if (moving) {
                blocks.remove(dragOriginalIndex);
                if (dragOriginalIndex < dest) dest--;
            }
            blocks.add(dest, dragging);
            runArmed = false;
            save();
            refreshButtons();
            notice(moving ? "Blokk áthelyezve." : "Blokk hozzáadva.", ChatFormatting.GREEN);
        }
        dragging = null;
        dragOriginalIndex = -1;
        setDragging(false);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = (int) Math.signum(verticalAmount);
        Popup popup = suggestionPopup();
        if (popup != null && popup.contains(mouseX, mouseY)) {
            suggestionScroll = Math.clamp(suggestionScroll - step, 0, valueSuggestions.size() - popup.rows());
            suggestionSelection = Math.clamp(suggestionSelection, suggestionScroll, suggestionScroll + popup.rows() - 1);
            return true;
        }
        if (mouseX >= 12 && mouseX < 12 + paletteWidth() && mouseY >= bodyTop() && mouseY < bodyBottom()) {
            paletteScroll -= step;
            clampPaletteScroll(filteredTypes().size());
            return true;
        }
        if (mouseX >= workspaceX() && mouseX < width - 12 && mouseY >= bodyTop() && mouseY < bodyBottom()) {
            sequenceScroll -= step;
            clampSequenceScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int sequenceIndexAt(double mx, double my) {
        int x = workspaceX() + 8;
        int y = bodyTop() + 24;
        if (mx < x || mx >= width - 20 || my < y || my >= bodyBottom() - 1) return -1;
        int maxWidth = width - workspaceX() - 28;
        for (int i = sequenceScroll; i < blocks.size() && y < bodyBottom() - 1; i++) {
            Chip visible = chip(blocks.get(i), maxWidth, true);
            if (my >= y && my < y + visible.height()) return mx < x + visible.width() ? i : -1;
            y += visible.height() + BLOCK_GAP;
        }
        return -1;
    }

    private record Insertion(int index, int y) { }

    private Insertion insertionAt(double mouseY) {
        int y = bodyTop() + 24;
        int maxWidth = width - workspaceX() - 28;
        int i = sequenceScroll;
        for (; i < blocks.size(); i++) {
            int h = blockHeight(blocks.get(i), maxWidth, true);
            if (mouseY < y + h / 2.0) break;
            y += h + BLOCK_GAP;
        }
        return new Insertion(i, Math.min(y, bodyBottom() - 2));
    }

    private int insertionIndex(double mouseY) { return insertionAt(mouseY).index(); }
    private int insertionY(double mouseY) { return insertionAt(mouseY).y(); }

    private int blockHeight(WorkflowBlock block, int maxWidth, boolean sequence) {
        return blockLines(block, maxWidth, sequence).size() > 1 ? 28 : BLOCK_HEIGHT;
    }

    private void clampSequenceScroll() {
        sequenceScroll = Math.clamp(sequenceScroll, 0, maxScroll(blocks.size(), bodyTop() + 24,
                i -> blockHeight(blocks.get(i), width - workspaceX() - 28, true)));
    }

    private void clampPaletteScroll(int count) {
        List<WorkflowBlock.Type> types = filteredTypes();
        paletteScroll = Math.clamp(paletteScroll, 0, maxScroll(count, paletteViewportTop() + 2,
                i -> blockHeight(preview(types.get(i)), paletteWidth() - 16, false)));
    }

    private int maxScroll(int count, int top, java.util.function.IntUnaryOperator heightAt) {
        if (count == 0) return 0;
        int remaining = bodyBottom() - 1 - top;
        int first = count - 1;
        remaining -= heightAt.applyAsInt(first);
        while (first > 0) {
            int step = heightAt.applyAsInt(first - 1) + BLOCK_GAP;
            if (remaining < step) break;
            remaining -= step;
            first--;
        }
        return first;
    }

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
