package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.List;

public final class AutoOpModule extends Module {
    private final SettingGroup sgWhere = settings.createGroup("Hol fusson?");
    private final SettingGroup sgTargets = settings.createGroup("Kit OP-oljon?");
    private final SettingGroup sgTiming = settings.createGroup("Időzítés");

    public final Setting<List<String>> servers = sgWhere.add(new StringListSetting.Builder()
            .name("szerverek")
            .description("Pontos szervercímek, ahol az Automata OP lefuthat. Példa: pelda.aternos.me")
            .defaultValue()
            .build());

    public final Setting<List<String>> loginAccounts = sgWhere.add(new StringListSetting.Builder()
            .name("belepesi-fiokok")
            .description("Azok a saját fióknevek, amelyekkel belépve az automatika futhat.")
            .defaultValue()
            .build());

    public final Setting<List<String>> targetOperators = sgTargets.add(new StringListSetting.Builder()
            .name("celfiokok")
            .description("A csatlakozás után OP-olandó Minecraft-fiókok.")
            .defaultValue()
            .build());

    public final Setting<Boolean> deopLoginAccount = sgTargets.add(new BoolSetting.Builder()
            .name("belepo-fiok-deop")
            .description("A célfiókok és a belépési parancsok után deOP-olja a belépett fiókot.")
            .defaultValue(false)
            .build());

    public final Setting<Integer> joinDelayTicks = sgTiming.add(new IntSetting.Builder()
            .name("belepesi-varakozas")
            .description("Csatlakozás után ennyi tickkel készítse elő a parancsokat. 20 tick körülbelül 1 másodperc.")
            .defaultValue(60)
            .range(20, 1200)
            .sliderRange(20, 200)
            .build());

    public final Setting<Integer> commandDelayTicks = sgTiming.add(new IntSetting.Builder()
            .name("parancsok-kozotti-varakozas")
            .description("Ennyi ticket várjon két OP/deOP parancs között.")
            .defaultValue(5)
            .range(1, 200)
            .sliderRange(1, 40)
            .build());

    public AutoOpModule() {
        super(AopToolsAddon.CATEGORY, "automata-op",
                "Belépéskor OP-olja a megadott célfiókokat, majd kérésre deOP-olja a belépett fiókot.");
        autoSubscribe = false;
    }

    @Override
    public String getInfoString() {
        return targetOperators.get().size() + " cél";
    }
}
