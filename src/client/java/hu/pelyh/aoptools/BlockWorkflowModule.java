package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class BlockWorkflowModule extends Module {
    private final SettingGroup storage = settings.getDefaultGroup();

    public final Setting<List<String>> workflow = storage.add(new StringListSetting.Builder()
            .name("mentett-blokkok")
            .description("A blokkeditor mentett parancssora.")
            .defaultValue()
            .visible(() -> false)
            .build());

    public final Setting<String> server = storage.add(new StringSetting.Builder()
            .name("szerver")
            .description("A pontos szervercím a belépéskori futtatáshoz.")
            .defaultValue("")
            .visible(() -> false)
            .build());

    public final Setting<String> account = storage.add(new StringSetting.Builder()
            .name("belepesi-fiok")
            .description("A saját fióknév a belépéskori futtatáshoz.")
            .defaultValue("")
            .visible(() -> false)
            .build());

    public final Setting<Boolean> runOnJoin = storage.add(new BoolSetting.Builder()
            .name("futtatas-belepeskor")
            .description("A mentett blokkok automatikus futtatása belépéskor.")
            .defaultValue(false)
            .visible(() -> false)
            .build());

    public final Setting<Boolean> allowDangerousOnJoin = storage.add(new BoolSetting.Builder()
            .name("kick-ban-belepeskor")
            .description("Engedélyezi a kill/kick/ban blokkokat automatikus futtatásnál.")
            .defaultValue(false)
            .visible(() -> false)
            .build());

    public final Setting<Boolean> excludeSelf = storage.add(new BoolSetting.Builder()
            .name("sajat-fiok-kihagyasa")
            .description("Az összes játékost érintő blokkok kihagyják a belépett fiókot.")
            .defaultValue(true)
            .visible(() -> false)
            .build());

    public final Setting<Integer> joinDelayTicks = storage.add(new IntSetting.Builder()
            .name("belepesi-varakozas")
            .description("Belépés után ennyi tickkel induljon a blokkprogram.")
            .defaultValue(60)
            .range(20, 1200)
            .visible(() -> false)
            .build());

    public final Setting<Integer> commandDelayTicks = storage.add(new IntSetting.Builder()
            .name("parancs-varakozas")
            .description("Ennyi tick legyen két parancs között.")
            .defaultValue(5)
            .range(1, 200)
            .visible(() -> false)
            .build());

    private OperatorAutomation automation;

    public BlockWorkflowModule() {
        super(AopToolsAddon.CATEGORY, "parancs-blokkok",
                "A bind lefuttatja a mentett blokkprogramot; a Meteor csempéje megnyitja a szerkesztőt.");
        autoSubscribe = false;
        runInMainMenu = true;
    }

    void bind(OperatorAutomation automation) {
        this.automation = automation;
    }

    public List<WorkflowBlock> blocks() {
        List<WorkflowBlock> result = new ArrayList<>();
        for (String encoded : workflow.get()) {
            WorkflowBlock block = WorkflowBlock.decode(encoded);
            if (block != null) result.add(block);
        }
        return result;
    }

    public void saveBlocks(List<WorkflowBlock> blocks) {
        workflow.set(blocks.stream().map(WorkflowBlock::encode).toList());
    }

    public boolean containsDangerousBlock() {
        return blocks().stream().anyMatch(block -> block.type().dangerous);
    }

    @Override
    public void onActivate() {
        if (mc.screen != null) {
            // A Meteor ClickGUI csempéjére kattintva szerkesztünk.
            openEditor();
        } else if (automation != null) {
            // Játék közben a billentyű-/egérbind a mentett programot futtatja.
            OperatorAutomation.Result result = automation.runWorkflowFromBind(mc, allowDangerousOnJoin.get());
            if (result.success()) info(result.message());
            else error(result.message());
        }
        disable();
    }

    public void openEditor() {
        if (automation == null) return;

        if (server.get().isBlank() && mc.getCurrentServer() != null) {
            server.set(mc.getCurrentServer().ip);
        }
        if (account.get().isBlank() && mc.getUser() != null) {
            account.set(mc.getUser().getName());
        }

        Screen parent = mc.screen;
        mc.execute(() -> mc.setScreen(new BlockWorkflowScreen(this, automation, parent)));
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        table.add(theme.label("Bind: mentett program futtatása.", false)).expandX();
        table.row();
        table.add(theme.label("OP ALL • deOP ALL • KILL ALL • KICK ALL • BAN ALL", false)).expandX();
        table.row();
        table.add(theme.label("OP XY • deOP XY • KILL XY • KICK XY • BAN XY", false)).expandX();
        table.row();

        WButton open = table.add(theme.button("BLOKKEDITOR MEGNYITÁSA")).expandX().widget();
        open.action = this::openEditor;
        return table;
    }

    @Override
    public String getInfoString() {
        return blocks().size() + " blokk";
    }
}
