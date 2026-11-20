package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

final class StressServerWhitelistScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private final StressServerWhitelistModule module;
    private final Screen parent;
    private final List<String> entries;
    private EditBox hostBox;
    private EditBox portBox;
    private int scroll;
    private String notice = "Csak saját localhost vagy privát LAN IP:port cím adható hozzá.";
    private int noticeColor = 0xFFBDBDBD;

    StressServerWhitelistScreen(StressServerWhitelistModule module, Screen parent) {
        super(Component.literal("Bot szerver-whitelist"));
        this.module = module;
        this.parent = parent;
        this.entries = new ArrayList<>(StressServerPolicy.canonicalEntries(module.entries()));
    }

    @Override
    protected void init() {
        int fieldY = 31;
        hostBox = addRenderableWidget(new EditBox(font, 12, fieldY, Math.max(130, width - 205), 20,
                Component.literal("Helyi IP")));
        hostBox.setMaxLength(45);
        hostBox.setValue("127.0.0.1");
        hostBox.setHint(Component.literal("127.0.0.1"));
        hostBox.setFilter(value -> value.isEmpty() || value.matches("[A-Za-z0-9.:\\[\\]-]+"));

        portBox = addRenderableWidget(new EditBox(font, width - 187, fieldY, 72, 20,
                Component.literal("Port")));
        portBox.setMaxLength(5);
        portBox.setValue("25565");
        portBox.setHint(Component.literal("25565"));
        portBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]+"));

        addRenderableWidget(Button.builder(Component.literal("Hozzáadás"), button -> addEntry())
                .bounds(width - 109, fieldY, 97, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Mentés és vissza"), button -> onClose())
                .bounds(width - 132, height - 25, 120, 20).build());
    }

    private void addEntry() {
        if (entries.size() >= 64) {
            notice("Legfeljebb 64 helyi szerver whitelistelhető.", ChatFormatting.RED);
            return;
        }
        try {
            int port = Integer.parseInt(portBox.getValue());
            String entry = StressServerPolicy.normalize(hostBox.getValue(), port);
            if (entries.contains(entry)) {
                notice("Ez a szerver már szerepel a whitelisten: " + entry, ChatFormatting.YELLOW);
                return;
            }
            entries.add(entry);
            module.setEntries(entries);
            notice("Engedélyezve: " + entry, ChatFormatting.GREEN);
        } catch (NumberFormatException exception) {
            notice("A port csak 1 és 65535 közötti szám lehet.", ChatFormatting.RED);
        } catch (IllegalArgumentException | UnknownHostException exception) {
            notice(exception.getMessage(), ChatFormatting.RED);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF0B0E14);
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFFFF);
        graphics.drawString(font, "ENGEDÉLYEZETT BOT-SZERVEREK", 12, 61, 0xFFFFFFFF);

        int top = 75;
        int bottom = height - 47;
        graphics.fill(12, top - 4, width - 12, bottom, 0xCC171A20);
        graphics.renderOutline(12, top - 4, width - 24, bottom - top + 4, 0xFF4B5563);
        clampScroll(bottom - top);
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        for (int row = 0; row < visible && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            int y = top + row * ROW_HEIGHT;
            graphics.fill(16, y, width - 16, y + 17, 0xFF263142);
            graphics.drawString(font, entries.get(index), 22, y + 5, 0xFFFFFFFF);
            graphics.fill(width - 42, y, width - 16, y + 17, 0xFF8E2F2F);
            graphics.drawCenteredString(font, "X", width - 29, y + 5, 0xFFFFFFFF);
        }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, "A whitelist üres – bot nem indítható.", width / 2,
                    top + 20, 0xFFFFA726);
        }
        graphics.drawString(font, notice, 12, height - 40, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        int top = 75;
        int bottom = height - 47;
        if (event.x() < width - 42 || event.x() > width - 16 || event.y() < top || event.y() >= bottom)
            return false;
        int row = ((int) event.y() - top) / ROW_HEIGHT;
        int index = scroll + row;
        if (index < 0 || index >= entries.size()) return false;
        String removed = entries.remove(index);
        module.setEntries(entries);
        clampScroll(bottom - top);
        notice("Eltávolítva: " + removed, ChatFormatting.YELLOW);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 71 && mouseY < height - 47) {
            scroll -= (int) Math.signum(verticalAmount);
            clampScroll(height - 122);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void clampScroll(int viewportHeight) {
        int visible = Math.max(1, viewportHeight / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - visible)));
    }

    private void notice(String message, ChatFormatting color) {
        notice = message == null || message.isBlank() ? "Ismeretlen hiba." : message;
        noticeColor = color.getColor() == null ? 0xFFFFFFFF : 0xFF000000 | color.getColor();
    }

    @Override
    public void onClose() {
        module.setEntries(entries);
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
