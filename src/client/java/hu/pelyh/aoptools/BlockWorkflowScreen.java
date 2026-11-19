package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BlockWorkflowScreen extends Screen {
    private static final int BLOCK_HEIGHT = 17;
    private static final int BLOCK_GAP = 3;
    private static final int BODY_TOP = 47;

    private final BlockWorkflowModule module;
    private final OperatorAutomation automation;
    private final Screen parent;
    private final List<WorkflowBlock> blocks;
    private EditBox searchBox;
    private EditBox targetBox;
    private EditBox blockBox;
    private Button selfButton;
    private DelaySlider delaySlider;
    private Button runButton;
    private WorkflowBlock dragging;
    private int dragOriginalIndex = -1;
    private int paletteScroll;
    private int sequenceScroll;
    private boolean runArmed;
    private String notice = "Keresés, majd húzd a blokkot a programba. Jobb kattintás: törlés.";
    private int noticeColor = 0xFFBDBDBD;

    public BlockWorkflowScreen(BlockWorkflowModule module, OperatorAutomation automation, Screen parent) {
        super(Component.literal("AOP Parancs Blokkok"));
        this.module = module;
        this.automation = automation;
        this.parent = parent;
        this.blocks = new ArrayList<>(module.blocks());
    }

    @Override
    protected void init() {
        int optionWidth = Math.max(120, Math.min(190, (width - 36) / 2));
        selfButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.excludeSelf.set(!module.excludeSelf.get());
            refreshButtons();
        }).bounds(12, 22, optionWidth, 20).build());
        delaySlider = addRenderableWidget(new DelaySlider(width - 12 - optionWidth, 22, optionWidth, 20));

        int paletteX = 12;
        int paletteWidth = paletteWidth();
        int fieldWidth = paletteWidth - 16;
        searchBox = addRenderableWidget(new EditBox(font, paletteX + 8, BODY_TOP + 18, fieldWidth, 18, Component.literal("Keresés")));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.literal("Keresés: op, kill, blokk..."));
        searchBox.setResponder(value -> paletteScroll = 0);

        targetBox = addRenderableWidget(new EditBox(font, paletteX + 8, BODY_TOP + 39, fieldWidth, 18, Component.literal("XY játékos")));
        targetBox.setMaxLength(16);
        targetBox.setFilter(value -> value.isEmpty() || OperatorAutomation.isValidPlayerName(value));
        targetBox.setHint(Component.literal("XY játékosnév, pl. Steve"));

        blockBox = addRenderableWidget(new EditBox(font, paletteX + 8, BODY_TOP + 60, fieldWidth, 18, Component.literal("Blokk")));
        blockBox.setMaxLength(80);
        blockBox.setHint(Component.literal("Blokk, pl. tnt vagy minecraft:tnt"));

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
    private int paletteViewportTop() { return BODY_TOP + 82; }
    private int workspaceX() { return 22 + paletteWidth(); }

    private void refreshButtons() {
        selfButton.setMessage(Component.literal("Saját fiók: " + (module.excludeSelf.get() ? "KIHAGY" : "BENNE")));
        delaySlider.refreshFromSetting();
        runButton.setMessage(Component.literal(runArmed ? "BIZTOSAN FUTTATOD?" : "Futtatás / leállítás"));
    }

    private void runNow() {
        save();
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

    private void save() { module.saveBlocks(blocks); }
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
        int rows = visibleRows(bodyBottom, viewportTop);
        clampPaletteScroll(rows, types.size());
        int shownTypes = Math.min(Math.max(0, types.size() - paletteScroll), rows);
        graphics.drawString(font, "BLOKKOK • " + types.size() + " találat", paletteX + 8, BODY_TOP + 6, 0xFFFFFFFF);
        graphics.drawString(font, "PROGRAM – fentről lefelé", workspaceX + 8, BODY_TOP + 7, 0xFFFFFFFF);
        graphics.enableScissor(paletteX + 1, viewportTop, paletteX + paletteWidth - 1, bodyBottom - 1);
        for (int row = 0; row < shownTypes; row++) {
            WorkflowBlock.Type type = types.get(paletteScroll + row);
            int y = viewportTop + 2 + row * (BLOCK_HEIGHT + BLOCK_GAP);
            drawBlock(graphics, paletteX + 8, y, paletteWidth - 16, preview(type), false);
        }
        graphics.disableScissor();

        int sequenceY = BODY_TOP + 24;
        int available = Math.max(1, (bodyBottom - sequenceY - 5) / (BLOCK_HEIGHT + BLOCK_GAP));
        clampSequenceScroll(available);
        int shown = Math.min(Math.max(0, blocks.size() - sequenceScroll), available);
        for (int row = 0; row < shown; row++) {
            drawBlock(graphics, workspaceX + 8, sequenceY + row * (BLOCK_HEIGHT + BLOCK_GAP),
                    workspaceWidth - 16, blocks.get(sequenceScroll + row), true);
        }
        if (blocks.isEmpty()) graphics.drawCenteredString(font, "Húzz ide blokkokat", workspaceX + workspaceWidth / 2,
                BODY_TOP + 55, 0xFF7C8799);
        graphics.drawString(font, notice, 12, height - 41, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dragging != null) drawBlock(graphics, mouseX - 85, mouseY - BLOCK_HEIGHT / 2, 170, dragging, true);
    }

    private WorkflowBlock preview(WorkflowBlock.Type type) {
        String target = type.needsTarget ? (targetBox.getValue().isBlank() ? "XY" : targetBox.getValue()) : "";
        String value = type.needsValue ? (blockBox.getValue().isBlank() ? "minecraft:tnt" : blockBox.getValue()) : "";
        return WorkflowBlock.of(type, target, value);
    }

    private void drawBlock(GuiGraphics graphics, int x, int y, int blockWidth, WorkflowBlock block, boolean sequence) {
        int color = block.type().color;
        graphics.fill(x, y, x + blockWidth, y + BLOCK_HEIGHT, color);
        graphics.renderOutline(x, y, blockWidth, BLOCK_HEIGHT, 0xDDFFFFFF);
        graphics.drawString(font, (sequence ? "⋮⋮ " : "+ ") + block.label(), x + 6, y + 5, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y();
        int paletteWidth = paletteWidth(), bodyBottom = bodyBottom(), viewportTop = paletteViewportTop();
        List<WorkflowBlock.Type> types = filteredTypes();
        if (event.button() == 0 && mx >= 20 && mx < 12 + paletteWidth - 8 && my >= viewportTop && my < bodyBottom) {
            int relative = (int) my - (viewportTop + 2);
            int row = relative < 0 ? -1 : relative / (BLOCK_HEIGHT + BLOCK_GAP);
            int within = relative < 0 ? -1 : relative % (BLOCK_HEIGHT + BLOCK_GAP);
            int typeIndex = paletteScroll + row;
            if (row >= 0 && typeIndex < types.size() && within < BLOCK_HEIGHT) {
                WorkflowBlock.Type type = types.get(typeIndex);
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
            clampPaletteScroll(visibleRows(bodyBottom(), paletteViewportTop()), filteredTypes().size()); return true;
        }
        if (mouseX >= workspaceX() && mouseX <= width - 12 && mouseY >= BODY_TOP && mouseY <= bodyBottom()) {
            int available = Math.max(1, (bodyBottom() - (BODY_TOP + 24) - 5) / (BLOCK_HEIGHT + BLOCK_GAP));
            sequenceScroll -= (int) Math.signum(verticalAmount); clampSequenceScroll(available); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int sequenceIndexAt(double mx, double my) {
        if (mx < workspaceX() + 8 || mx > width - 20) return -1;
        int y = (int) my - (BODY_TOP + 24);
        if (y < 0) return -1;
        return y % (BLOCK_HEIGHT + BLOCK_GAP) < BLOCK_HEIGHT ? sequenceScroll + y / (BLOCK_HEIGHT + BLOCK_GAP) : -1;
    }
    private int insertionIndex(double my) {
        int y = Math.max(0, (int) my - (BODY_TOP + 24));
        return Math.max(0, Math.min(blocks.size(), sequenceScroll + (y + 10) / (BLOCK_HEIGHT + BLOCK_GAP)));
    }
    private static int visibleRows(int bottom, int top) { return Math.max(1, (bottom - top - 3) / (BLOCK_HEIGHT + BLOCK_GAP)); }
    private void clampSequenceScroll(int rows) { sequenceScroll = Math.max(0, Math.min(sequenceScroll, Math.max(0, blocks.size() - rows))); }
    private void clampPaletteScroll(int rows, int count) { paletteScroll = Math.max(0, Math.min(paletteScroll, Math.max(0, count - rows))); }

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
