package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public final class OpEveryoneModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> commandDelayTicks = sgGeneral.add(new IntSetting.Builder()
            .name("parancsok-kozotti-varakozas")
            .description("Ennyi ticket várjon két /op parancs között.")
            .defaultValue(5)
            .range(1, 200)
            .sliderRange(1, 40)
            .build());

    private OperatorAutomation automation;

    public OpEveryoneModule() {
        super(AopToolsAddon.CATEGORY, "op-mindenkinek",
                "Bekapcsoláskor egyszer OP-olja a játékoslistán látható összes játékost, majd kikapcsol.");
        autoSubscribe = false;
    }

    void bind(OperatorAutomation automation) {
        this.automation = automation;
    }

    @Override
    public void onActivate() {
        if (automation != null) {
            OperatorAutomation.Result result = automation.opEveryone(mc);
            if (result.success()) info(result.message());
            else error(result.message());
        }
        disable();
    }
}
