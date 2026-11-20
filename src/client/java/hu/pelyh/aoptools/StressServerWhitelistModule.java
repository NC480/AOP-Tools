package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.List;

public final class StressServerWhitelistModule extends Module {
    private final SettingGroup storage = settings.getDefaultGroup();
    private final Setting<List<String>> allowedServers = storage.add(new StringListSetting.Builder()
            .name("engedelyezett-helyi-szerverek")
            .description("A botok kizárólag az itt szereplő privát IP:port címekhez csatlakozhatnak.")
            .defaultValue("127.0.0.1:25565")
            .visible(() -> false)
            .build());

    public StressServerWhitelistModule() {
        super(AopToolsAddon.CATEGORY, "bot-szerver-whitelist",
                "Külön szerkesztő a helyi stresszteszt botjai számára engedélyezett szerverekhez.");
        autoSubscribe = false;
        runInMainMenu = true;
    }

    List<String> entries() {
        return List.copyOf(allowedServers.get());
    }

    void setEntries(List<String> entries) {
        allowedServers.set(List.copyOf(entries));
    }

    @Override
    public void onActivate() {
        var parent = mc.screen;
        mc.execute(() -> mc.setScreen(new StressServerWhitelistScreen(this, parent)));
        disable();
    }

    @Override
    public String getInfoString() {
        return StressServerPolicy.canonicalEntries(entries()).size() + " szerver";
    }
}
