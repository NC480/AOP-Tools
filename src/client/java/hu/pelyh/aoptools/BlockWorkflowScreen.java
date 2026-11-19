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
import java.util.List;
import java.util.Locale;

public final class BlockWorkflowScreen extends Screen {
    private static final int BLOCK_HEIGHT = 25;
    private static final int BLOCK_GAP = 5;

    private final BlockWorkflowModule module;
    private final OperatorAutomation automation;
    private final Screen parent;
    private final List<WorkflowBlock> blocks;
    private EditBox serverBox;
    private EditBox accountBox;
    private EditBox targetBox;
    private Button joinButton;
    private Button dangerousButton;
    private Button selfButton;
    private DelaySlider delaySlider;
    private Button runButton;
    private WorkflowBlock dragging;
    private int dragOriginalIndex = -1;
    private int paletteScroll;
    private int sequenceScroll;
    private boolean runArmed;
    private String notice = "Húzz blokkokat balról a programba. Jobb kattintás: törlés.";
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
        int fieldWidth = Math.max(110, Math.min(190, (width - 56) / 3));
        serverBox = addRenderableWidget(new EditBox(font, 12, 30, fieldWidth, 20, Component.literal("Szerver")));
        serverBox.setMaxLength(255);
        serverBox.setValue(module.server.get());
        serverBox.setHint(Component.literal("pelda.aternos.me"));
        serverBox.setResponder(value -> module.server.set(value.trim()));

        accountBox = addRenderableWidget(new EditBox(font, 20 + fieldWidth, 30, fieldWidth, 20, Component.literal("Belépési fiók")));
        accountBox.setMaxLength(16);
        accountBox.setValue(module.account.get());
        accountBox.setHint(Component.literal("saját fióknév"));
        accountBox.setResponder(value -> module.account.set(value.trim()));

        int toggleX = 28 + fieldWidth * 2;
        int toggleWidth = Math.max(92, Math.min(145, width - toggleX - 12));
        joinButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.runOnJoin.set(!module.runOnJoin.get());
            refreshButtons();
        }).bounds(toggleX, 30, toggleWidth, 20).build());

        int optionWidth = Math.min(160, Math.max(90, (width - 32) / 3));
        dangerousButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.allowDangerousOnJoin.set(!module.allowDangerousOnJoin.get());
            refreshButtons();
        }).bounds(12, 55, optionWidth, 20).build());

        selfButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            module.excludeSelf.set(!module.excludeSelf.get());
            refreshButtons();
        }).bounds(16 + optionWidth, 55, optionWidth, 20).build());

        delaySlider = addRenderableWidget(new DelaySlider(20 + optionWidth * 2, 55, optionWidth, 20));

        int bottomY = height - 29;
        int bottomWidth = Math.min(145, Math.max(90, (width - 32) / 3));
        runButton = addRenderableWidget(Button.builder(Component.literal("Futtatás most"), button -> runNow())
                .bounds(12, bottomY, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Program törlése"), button -> {
            blocks.clear();
            save();
            runArmed = false;
            notice("A blokkprogram törölve.", ChatFormatting.YELLOW);
            refreshButtons();
        }).bounds(16 + bottomWidth, bottomY, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Mentés és vissza"), button -> onClose())
                .bounds(width - 12 - bottomWidth, bottomY, bottomWidth, 20).build());

        int paletteWidth = Math.max(176, Math.min(230, width / 3));
        int targetY = 115;
        targetBox = addRenderableWidget(new EditBox(font, 20, targetY, paletteWidth - 16, 20,
                Component.literal("XY játékosnév")));
        targetBox.setMaxLength(16);
        targetBox.setFilter(value -> value.isEmpty() || OperatorAutomation.isValidPlayerName(value));
        targetBox.setHint(Component.literal("Steve"));

        refreshButtons();
    }

    private void refreshButtons() {
        joinButton.setMessage(Component.literal("Belépéskor: " + onOff(module.runOnJoin.get())));
        dangerousButton.setMessage(Component.literal("Auto kill/kick/ban: " + onOff(module.allowDangerousOnJoin.get())));
        selfButton.setMessage(Component.literal("Saját fiók: " + (module.excludeSelf.get() ? "KIHAGY" : "BENNE")));
        delaySlider.refreshFromSetting();
        runButton.setMessage(Component.literal(runArmed ? "BIZTOSAN FUTTATOD?" : "Futtatás most"));
    }

    private static String onOff(boolean value) {
        return value ? "BE" : "KI";
    }

    private void runNow() {
        save();
        if (module.containsDangerousBlock() && !runArmed) {
            runArmed = true;
            notice("A program kill/kick/ban blokkot tartalmaz. Kattints még egyszer a futtatáshoz!", ChatFormatting.RED);
            refreshButtons();
            return;
        }

        OperatorAutomation.Result result = automation.runWorkflow(minecraft, true);
        runArmed = false;
        notice(result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        refreshButtons();
    }

    private void save() {
        module.saveBlocks(blocks);
    }

    private void notice(String message, ChatFormatting color) {
        notice = message;
        noticeColor = color.getColor() == null ? 0xFFFFFFFF : 0xFF000000 | color.getColor();
    }

    @Override
    public void onClose() {
        save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Meteor already renders a blurred ClickGUI background. Minecraft 1.21.11
        // throws if a second screen requests blur in the same frame, so this editor
        // deliberately uses its own opaque background.
        graphics.fill(0, 0, width, height, 0xFF0B0E14);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.drawString(font, "Szerver (auto futtatáshoz)", 12, 20, 0xFFBDBDBD);
        graphics.drawString(font, "Belépési fiók", accountBox.getX(), 20, 0xFFBDBDBD);

        int bodyTop = 86;
        int bodyBottom = height - 49;
        int paletteX = 12;
        int paletteWidth = Math.max(176, Math.min(230, width / 3));
        int workspaceX = paletteX + paletteWidth + 10;
        int workspaceWidth = width - workspaceX - 12;

        graphics.fill(paletteX, bodyTop, paletteX + paletteWidth, bodyBottom, 0xCC171A20);
        graphics.renderOutline(paletteX, bodyTop, paletteWidth, bodyBottom - bodyTop, 0xFF4B5563);
        graphics.fill(workspaceX, bodyTop, workspaceX + workspaceWidth, bodyBottom, 0xCC111827);
        graphics.renderOutline(workspaceX, bodyTop, workspaceWidth, bodyBottom - bodyTop, 0xFF4B5563);
        int paletteViewportTop = bodyTop + 57;
        int paletteRows = paletteVisibleRows(bodyBottom, paletteViewportTop);
        clampPaletteScroll(paletteRows);
        int paletteShown = Math.min(WorkflowBlock.Type.values().length - paletteScroll, paletteRows);
        graphics.drawString(font, "BLOKKOK • görgess  " + (paletteScroll + 1) + "–"
                + (paletteScroll + paletteShown) + "/" + WorkflowBlock.Type.values().length,
                paletteX + 8, bodyTop + 7, 0xFFFFFFFF);
        graphics.drawString(font, "PROGRAM – fentről lefelé fut", workspaceX + 8, bodyTop + 7, 0xFFFFFFFF);

        graphics.drawString(font, "XY név (behúzás előtt):", paletteX + 8, bodyTop + 20, 0xFFBDBDBD);
        graphics.enableScissor(paletteX + 1, paletteViewportTop, paletteX + paletteWidth - 1, bodyBottom - 2);
        for (int row = 0; row < paletteShown; row++) {
            WorkflowBlock.Type type = WorkflowBlock.Type.values()[paletteScroll + row];
            int paletteY = paletteViewportTop + 3 + row * (BLOCK_HEIGHT + BLOCK_GAP);
            drawBlock(graphics, paletteX + 8, paletteY, paletteWidth - 16, WorkflowBlock.of(type, "XY"), false);
        }
        graphics.disableScissor();

        int sequenceY = bodyTop + 25;
        int available = Math.max(1, (bodyBottom - sequenceY - 6) / (BLOCK_HEIGHT + BLOCK_GAP));
        clampScroll(available);
        int shown = Math.min(blocks.size() - sequenceScroll, available);
        for (int row = 0; row < shown; row++) {
            int index = sequenceScroll + row;
            drawBlock(graphics, workspaceX + 10, sequenceY + row * (BLOCK_HEIGHT + BLOCK_GAP),
                    workspaceWidth - 20, blocks.get(index), true);
        }
        if (blocks.size() > available) {
            graphics.drawString(font, "Görgess a programon • " + (sequenceScroll + 1) + "–"
                            + (sequenceScroll + shown) + " / " + blocks.size(),
                    workspaceX + 12, bodyBottom - 13, 0xFFFFC107);
        }
        if (blocks.isEmpty()) {
            graphics.drawCenteredString(font, "Húzz ide egy blokkot", workspaceX + workspaceWidth / 2,
                    bodyTop + 54, 0xFF7C8799);
        }

        graphics.drawString(font, notice, 12, height - 43, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (dragging != null) {
            drawBlock(graphics, mouseX - 70, mouseY - BLOCK_HEIGHT / 2, 140, dragging, true);
        }
    }

    private void drawBlock(GuiGraphics graphics, int x, int y, int blockWidth, WorkflowBlock block, boolean sequence) {
        int color = block.type().color;
        graphics.fill(x, y, x + blockWidth, y + BLOCK_HEIGHT, color);
        graphics.fill(x + 4, y - 3, x + 25, y + 2, color);
        graphics.renderOutline(x, y, blockWidth, BLOCK_HEIGHT, 0xDDFFFFFF);
        String prefix = sequence ? "⋮⋮  " : "+  ";
        graphics.drawString(font, prefix + block.label(), x + 8, y + 8, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        double mouseX = event.x();
        double mouseY = event.y();
        int bodyTop = 86;
        int paletteX = 12;
        int paletteWidth = Math.max(176, Math.min(230, width / 3));
        int workspaceX = paletteX + paletteWidth + 10;
        int bodyBottom = height - 49;
        int paletteViewportTop = bodyTop + 57;

        if (event.button() == 0 && mouseX >= paletteX + 8 && mouseX < paletteX + paletteWidth - 8
                && mouseY >= paletteViewportTop && mouseY < bodyBottom) {
            int relativeY = (int) mouseY - (paletteViewportTop + 3);
            int index = relativeY < 0 ? -1 : relativeY / (BLOCK_HEIGHT + BLOCK_GAP);
            int within = relativeY < 0 ? -1 : relativeY % (BLOCK_HEIGHT + BLOCK_GAP);
            int typeIndex = paletteScroll + index;
            if (index >= 0 && typeIndex < WorkflowBlock.Type.values().length && within < BLOCK_HEIGHT) {
                WorkflowBlock.Type type = WorkflowBlock.Type.values()[typeIndex];
                String target = targetBox.getValue().trim();
                if (type.needsTarget && !OperatorAutomation.isValidPlayerName(target)) {
                    targetBox.setFocused(true);
                    notice("Előbb írd be az XY nevet fent, utána húzd be az OP XY/deOP XY blokkot.",
                            ChatFormatting.RED);
                    return true;
                }
                dragging = WorkflowBlock.of(type, target);
                dragOriginalIndex = -1;
                setDragging(true);
                return true;
            }
        }

        int index = sequenceIndexAt(mouseX, mouseY, workspaceX, bodyTop);
        if (index >= 0 && index < blocks.size()) {
            if (event.button() == 1) {
                blocks.remove(index);
                save();
                runArmed = false;
                refreshButtons();
                notice("Blokk törölve.", ChatFormatting.YELLOW);
                return true;
            }
            if (event.button() == 0) {
                dragging = blocks.remove(index);
                dragOriginalIndex = index;
                setDragging(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            int bodyTop = 86;
            int paletteWidth = Math.max(176, Math.min(230, width / 3));
            int workspaceX = 22 + paletteWidth;
            int bodyBottom = height - 49;
            if (event.x() >= workspaceX && event.x() <= width - 12 && event.y() >= bodyTop && event.y() <= bodyBottom) {
                int index = insertionIndex(event.y(), bodyTop);
                blocks.add(Math.min(index, blocks.size()), dragging);
                notice("Blokk hozzáadva. A program automatikusan mentve.", ChatFormatting.GREEN);
            } else if (dragOriginalIndex >= 0) {
                blocks.add(Math.min(dragOriginalIndex, blocks.size()), dragging);
                notice("A blokk visszakerült az eredeti helyére.", ChatFormatting.GRAY);
            }
            dragging = null;
            dragOriginalIndex = -1;
            setDragging(false);
            runArmed = false;
            save();
            refreshButtons();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int paletteWidth = Math.max(176, Math.min(230, width / 3));
        int workspaceX = 22 + paletteWidth;
        int bodyTop = 86;
        int bodyBottom = height - 49;
        if (mouseX >= 12 && mouseX < 12 + paletteWidth && mouseY >= bodyTop && mouseY <= bodyBottom) {
            int paletteViewportTop = bodyTop + 57;
            int visibleRows = paletteVisibleRows(bodyBottom, paletteViewportTop);
            paletteScroll -= (int) Math.signum(verticalAmount);
            clampPaletteScroll(visibleRows);
            return true;
        }
        if (mouseX >= workspaceX && mouseX <= width - 12 && mouseY >= bodyTop && mouseY <= bodyBottom) {
            int available = Math.max(1, (bodyBottom - (bodyTop + 25) - 6) / (BLOCK_HEIGHT + BLOCK_GAP));
            sequenceScroll -= (int) Math.signum(verticalAmount);
            clampScroll(available);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int sequenceIndexAt(double mouseX, double mouseY, int workspaceX, int bodyTop) {
        if (mouseX < workspaceX + 10 || mouseX > width - 22) return -1;
        int y = (int) mouseY - (bodyTop + 25);
        if (y < 0) return -1;
        int index = y / (BLOCK_HEIGHT + BLOCK_GAP);
        int within = y % (BLOCK_HEIGHT + BLOCK_GAP);
        return within < BLOCK_HEIGHT ? sequenceScroll + index : -1;
    }

    private int insertionIndex(double mouseY, int bodyTop) {
        int y = Math.max(0, (int) mouseY - (bodyTop + 25));
        int index = sequenceScroll + (y + (BLOCK_HEIGHT + BLOCK_GAP) / 2) / (BLOCK_HEIGHT + BLOCK_GAP);
        return Math.max(0, Math.min(blocks.size(), index));
    }

    private void clampScroll(int visibleRows) {
        sequenceScroll = Math.max(0, Math.min(sequenceScroll, Math.max(0, blocks.size() - visibleRows)));
    }

    private static int paletteVisibleRows(int bodyBottom, int paletteViewportTop) {
        return Math.max(1, (bodyBottom - paletteViewportTop - 4) / (BLOCK_HEIGHT + BLOCK_GAP));
    }

    private void clampPaletteScroll(int visibleRows) {
        paletteScroll = Math.max(0, Math.min(paletteScroll,
                Math.max(0, WorkflowBlock.Type.values().length - visibleRows)));
    }

    private final class DelaySlider extends AbstractSliderButton {
        private static final int MIN_TICKS = 1;
        private static final int MAX_TICKS = 200;

        private DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), toSliderValue(module.commandDelayTicks.get()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double seconds = module.commandDelayTicks.get() / 20.0;
            setMessage(Component.literal("Parancsköz: "
                    + String.format(Locale.ROOT, seconds < 1 ? "%.2f" : "%.1f", seconds) + " mp"));
        }

        @Override
        protected void applyValue() {
            int ticks = MIN_TICKS + (int) Math.round(value * (MAX_TICKS - MIN_TICKS));
            module.commandDelayTicks.set(ticks);
            updateMessage();
        }

        private void refreshFromSetting() {
            value = toSliderValue(module.commandDelayTicks.get());
            updateMessage();
        }

        private static double toSliderValue(int ticks) {
            int clamped = Math.max(MIN_TICKS, Math.min(MAX_TICKS, ticks));
            return (clamped - MIN_TICKS) / (double) (MAX_TICKS - MIN_TICKS);
        }
    }
}
