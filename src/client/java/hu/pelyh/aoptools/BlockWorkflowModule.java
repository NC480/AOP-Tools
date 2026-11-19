package hu.pelyh.aoptools;

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

    public final Setting<List<String>> presetStorage = storage.add(new StringListSetting.Builder()
            .name("presetek")
            .description("A blokkeditor mentett preset-konténerei.")
            .defaultValue()
            .visible(() -> false)
            .build());

    public final Setting<String> activePreset = storage.add(new StringSetting.Builder()
            .name("aktiv-preset")
            .description("A bind és az autorun által futtatott preset neve.")
            .defaultValue("Alap")
            .visible(() -> false)
            .build());

    public final Setting<Boolean> autoRun = storage.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
            .name("aktiv-preset-autorun")
            .description("Belépéskor automatikusan lefuttatja az aktív presetet.")
            .defaultValue(false)
            .visible(() -> false)
            .build());

    public final Setting<Boolean> excludeSelf = storage.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
            .name("sajat-fiok-kihagyasa")
            .description("Az összes játékost érintő blokkok kihagyják a belépett fiókot.")
            .defaultValue(true)
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
        List<WorkflowPreset> presets = presets();
        for (WorkflowPreset preset : presets) {
            if (preset.name().equalsIgnoreCase(activePreset.get())) return preset.blocks();
        }
        return presets.getFirst().blocks();
    }

    public List<WorkflowPreset> presets() {
        List<WorkflowPreset> presets = new ArrayList<>();
        for (String encoded : presetStorage.get()) {
            WorkflowPreset preset = WorkflowPreset.decode(encoded);
            if (preset != null) presets.add(preset);
        }
        if (!presets.isEmpty()) return presets;

        List<WorkflowBlock> result = new ArrayList<>();
        for (String encoded : workflow.get()) {
            WorkflowBlock block = WorkflowBlock.decode(encoded);
            if (block != null) result.add(block);
        }
        return List.of(new WorkflowPreset("Alap", result));
    }

    public void saveBlocks(List<WorkflowBlock> blocks) {
        List<WorkflowPreset> presets = new ArrayList<>(presets());
        int index = activePresetIndex(presets);
        presets.set(index, new WorkflowPreset(presets.get(index).name(), blocks));
        savePresets(presets);
    }

    public void savePresets(List<WorkflowPreset> presets) {
        List<WorkflowPreset> safe = presets == null || presets.isEmpty()
                ? List.of(WorkflowPreset.empty("Alap")) : List.copyOf(presets);
        presetStorage.set(safe.stream().map(WorkflowPreset::encode).toList());
        if (safe.stream().noneMatch(p -> p.name().equalsIgnoreCase(activePreset.get()))) {
            activePreset.set(safe.getFirst().name());
        }
        workflow.set(List.of());
    }

    public int activePresetIndex(List<WorkflowPreset> presets) {
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).name().equalsIgnoreCase(activePreset.get())) return i;
        }
        return 0;
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
            OperatorAutomation.Result result = automation.runWorkflowFromBind(mc, true);
            if (result.success()) info(result.message());
            else error(result.message());
        }
        disable();
    }

    public void openEditor() {
        if (automation == null) return;

        Screen parent = mc.screen;
        mc.execute(() -> mc.setScreen(new BlockWorkflowScreen(this, automation, parent)));
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        table.add(theme.label("Bind: mentett program futtatása.", false)).expandX();
        table.row();
        table.add(theme.label("OP/deOP/KILL/KICK/BAN • ALL és XY", false)).expandX();
        table.row();
        table.add(theme.label("Folyamatos blokk ALL/XY lába alá • bind: indítás/leállítás", false)).expandX();
        table.row();
        table.add(theme.label("Presetek: " + presets().size() + " • aktív: " + activePreset.get()
                + " • autorun: " + (autoRun.get() ? "BE" : "KI"), false)).expandX();
        table.row();

        WButton open = table.add(theme.button("BLOKKEDITOR MEGNYITÁSA")).expandX().widget();
        open.action = this::openEditor;
        return table;
    }

    @Override
    public String getInfoString() {
        return activePreset.get() + " • " + blocks().size() + " blokk";
    }
}
