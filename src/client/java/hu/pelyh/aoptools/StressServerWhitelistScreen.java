package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

final class StressServerWhitelistScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 93;
    private static final int PANEL = 0xFF182332;
    private static final int BORDER = 0xFF354558;
    private static final int MUTED = 0xFFADBDCC;
    private static final int ACCENT = 0xFF62D5BF;
    private final StressServerWhitelistModule module;
    private final Screen parent;
    private final List<String> entries;
    private EditBox hostBox;
    private EditBox portBox;
    private final List<Button> removeButtons = new ArrayList<>();
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
        String host = hostBox == null ? "127.0.0.1" : hostBox.getValue();
        String port = portBox == null ? "25565" : portBox.getValue();
        int fieldY = 45;
        int addX = right() - 8 - 74;
        int portX = addX - 6 - 48;
        int hostX = left() + 8;
        hostBox = addRenderableWidget(new EditBox(font, hostX, fieldY, portX - 6 - hostX, 20,
                Component.literal("Helyi IP")));
        hostBox.setMaxLength(45);
        hostBox.setValue(host);
        hostBox.setHint(Component.literal("127.0.0.1"));
        hostBox.setFilter(value -> value.isEmpty() || value.matches("[A-Za-z0-9.:\\[\\]-]+"));

        portBox = addRenderableWidget(new EditBox(font, portX, fieldY, 48, 20,
                Component.literal("Port")));
        portBox.setMaxLength(5);
        portBox.setValue(port);
        portBox.setHint(Component.literal("25565"));
        portBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]+"));

        addRenderableWidget(Button.builder(Component.literal("Hozzáadás"), button -> addEntry())
                .bounds(addX, fieldY, 74, 20).build());
        removeButtons.clear();
        for (int row = 0; row < visibleRows(); row++) {
            int slot = row;
            Button remove = Button.builder(Component.literal("Törlés"), button -> removeEntry(slot))
                    .bounds(right() - 65, LIST_TOP + row * ROW_HEIGHT, 54, 18).build();
            removeButtons.add(addRenderableWidget(remove));
        }
        refreshRows();
        addRenderableWidget(Button.builder(Component.literal("Mentés és vissza"), button -> onClose())
                .bounds(right() - 118, height - 25, 118, 20).build());
    }

    private int left() { return Math.max(8, (width - 520) / 2); }
    private int right() { return width - left(); }
    private int listBottom() { return height - 50; }
    private int visibleRows() { return Math.max(0, (listBottom() - LIST_TOP) / ROW_HEIGHT); }

    private void refreshRows() {
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - visibleRows()));
        for (int row = 0; row < removeButtons.size(); row++) {
            Button remove = removeButtons.get(row);
            int index = scroll + row;
            remove.visible = index < entries.size();
            remove.active = remove.visible;
            remove.setTooltip(remove.visible ? Tooltip.create(Component.literal(entries.get(index))) : null);
        }
    }

    private void removeEntry(int row) {
        String removed = entries.remove(scroll + row);
        module.setEntries(entries);
        refreshRows();
        notice("Eltávolítva: " + removed, ChatFormatting.YELLOW);
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
            scroll = entries.size() - visibleRows();
            refreshRows();
            notice("Engedélyezve: " + entry, ChatFormatting.GREEN);
        } catch (NumberFormatException exception) {
            notice("A port csak 1 és 65535 közötti szám lehet.", ChatFormatting.RED);
        } catch (IllegalArgumentException | UnknownHostException exception) {
            notice(exception.getMessage(), ChatFormatting.RED);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = left(), end = right(), bottom = listBottom();
        graphics.fill(0, 0, width, height, 0xFF101722);
        graphics.fill(x, 7, x + 3, 21, ACCENT);
        graphics.drawString(font, title, x + 8, 10, 0xFFF1F6FA);
        graphics.fill(x, 26, end, 71, PANEL);
        graphics.drawString(font, "Helyi IP-cím", hostBox.getX(), 32, MUTED);
        graphics.drawString(font, "Port", portBox.getX(), 32, MUTED);

        graphics.fill(x, 74, end, bottom, PANEL);
        graphics.drawString(font, "Engedélyezett szerverek", x + 8, 79, 0xFFF1F6FA);
        String count = entries.size() + " / 64";
        graphics.drawString(font, count, end - 10 - font.width(count), 79, MUTED);
        for (int row = 0; row < visibleRows() && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            int y = LIST_TOP + row * ROW_HEIGHT;
            boolean hover = mouseX >= x + 5 && mouseX < end - 8 && mouseY >= y && mouseY < y + 18;
            graphics.fill(x + 5, y, end - 8, y + 18, hover ? 0xFF304354 : 0xFF223141);
            graphics.drawString(font, clipped(entries.get(index), end - x - 84), x + 12, y + 5, 0xFFF1F6FA);
        }
        if (visibleRows() > 0 && entries.size() > visibleRows()) {
            int track = bottom - LIST_TOP - 3;
            int thumb = Math.max(7, track * visibleRows() / entries.size());
            int y = LIST_TOP + (track - thumb) * scroll / (entries.size() - visibleRows());
            graphics.fill(end - 5, LIST_TOP, end - 3, LIST_TOP + track, BORDER);
            graphics.fill(end - 5, y, end - 3, y + thumb, ACCENT);
        }
        if (entries.isEmpty() && visibleRows() > 0) {
            graphics.drawCenteredString(font, "Adj hozzá egy helyi szervert fent.", width / 2,
                    (LIST_TOP + bottom) / 2 - 4, MUTED);
        }
        graphics.drawString(font, clipped(notice, end - x), x, bottom + 7, noticeColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String clipped(String text, int pixels) {
        return font.width(text) <= pixels ? text
                : font.plainSubstrByWidth(text, Math.max(0, pixels - font.width("…"))) + "…";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= left() && mouseX < right() && mouseY >= LIST_TOP && mouseY < listBottom()
                && entries.size() > visibleRows() && verticalAmount != 0) {
            scroll -= (int) Math.signum(verticalAmount);
            refreshRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
