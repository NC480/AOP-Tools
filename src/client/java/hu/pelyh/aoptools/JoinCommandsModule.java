package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.List;

public final class JoinCommandsModule extends Module {
    private final SettingGroup sgWhere = settings.createGroup("Hol fusson?");
    private final SettingGroup sgCommands = settings.createGroup("Parancsok");
    private final SettingGroup sgTiming = settings.createGroup("Időzítés");

    public final Setting<List<String>> servers = sgWhere.add(new StringListSetting.Builder()
            .name("szerverek")
            .description("Pontos szervercímek, ahol a belépési parancsok lefuthatnak.")
            .defaultValue()
            .build());

    public final Setting<List<String>> loginAccounts = sgWhere.add(new StringListSetting.Builder()
            .name("belepesi-fiokok")
            .description("Azok a saját fióknevek, amelyekkel belépve a parancsok lefuthatnak.")
            .defaultValue()
            .build());

    public final Setting<List<String>> commands = sgCommands.add(new StringListSetting.Builder()
            .name("lefuttatando-parancsok")
            .description("Csatlakozáskor elküldendő szerverparancsok, soronként egy. A kezdő / jel nem kötelező.")
            .defaultValue()
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
            .description("Ennyi ticket várjon két egyéni parancs között.")
            .defaultValue(5)
            .range(1, 200)
            .sliderRange(1, 40)
            .build());

    public JoinCommandsModule() {
        super(AopToolsAddon.CATEGORY, "belepesi-parancsok",
                "Belépéskor a menüben megadott szerverparancsokat futtatja le.");
        autoSubscribe = false;
    }

    @Override
    public String getInfoString() {
        return commands.get().size() + " parancs";
    }
}
