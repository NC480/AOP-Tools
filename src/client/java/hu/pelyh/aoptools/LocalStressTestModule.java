package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.ArrayList;
import java.util.List;

public final class LocalStressTestModule extends Module {
    private final SettingGroup server = settings.createGroup("Cél szerver");
    private final SettingGroup identities = settings.createGroup("Botnevek");
    private final SettingGroup timing = settings.createGroup("Időzítés és újrapróbálás");
    private final LocalStressBotController controller = new LocalStressBotController((exitCode, detail) -> {
        if (exitCode == 0) info("A helyi stresszteszt befejeződött.");
        else error(detail.isBlank()
                ? "A botok nem tudtak belépni. Részletek: config/aop-tools/stress-test.log"
                : detail);
        if (isActive()) disable();
    });

    private final Setting<String> host = server.add(new StringSetting.Builder()
            .name("helyi-szerver-cime")
            .description("Csak localhost, loopback vagy privát LAN IP-cím engedélyezett.")
            .defaultValue("127.0.0.1")
            .build());

    private final Setting<Integer> port = server.add(new IntSetting.Builder()
            .name("port")
            .description("A helyi Paper szerver portja.")
            .defaultValue(25565)
            .range(1, 65535)
            .sliderRange(1024, 65535)
            .build());

    private final Setting<Integer> botCount = identities.add(new IntSetting.Builder()
            .name("botok-szama")
            .description("Közvetlen localhost szerveren legfeljebb 30, privát LAN-címen legfeljebb 10 tesztkliens engedélyezett.")
            .defaultValue(3)
            .range(1, 30)
            .sliderRange(1, 30)
            .build());

    private final Setting<List<String>> customNames = identities.add(new StringListSetting.Builder()
            .name("egyedi-botnevek")
            .description("Legfeljebb 30 opcionális Minecraft-név, soronként egy. A hiányzó neveket az előtag tölti ki.")
            .defaultValue(new ArrayList<>())
            .build());

    private final Setting<String> namePrefix = identities.add(new StringSetting.Builder()
            .name("bot-nev-elotag")
            .description("A hiányzó egyedi nevek képzése: 1-10 angol betű, szám vagy aláhúzás; a sorszám automatikus.")
            .defaultValue("AOPTest")
            .build());

    private final Setting<Double> connectionDelaySeconds = timing.add(new DoubleSetting.Builder()
            .name("botok-belepesi-ideje-masodperc")
            .description("Két bot belépése közti idő. 1 másodperc alatti érték csak közvetlen localhost szerveren engedélyezett.")
            .defaultValue(1.0)
            .range(0.1, 60.0)
            .sliderRange(0.1, 10.0)
            .decimalPlaces(1)
            .build());

    private final Setting<Integer> durationMinutes = timing.add(new IntSetting.Builder()
            .name("maximalis-futasi-ido-perc")
            .description("Az idő lejártakor minden bot automatikusan lecsatlakozik.")
            .defaultValue(3)
            .range(1, 10)
            .sliderRange(1, 10)
            .build());

    private final Setting<Integer> connectionTimeoutSeconds = timing.add(new IntSetting.Builder()
            .name("kapcsolati-timeout-masodperc")
            .description("Ennyi ideig vár egy bot arra, hogy a szerver elfogadja a kapcsolatot.")
            .defaultValue(10)
            .range(3, 30)
            .sliderRange(3, 30)
            .build());

    private final Setting<Integer> retryCount = timing.add(new IntSetting.Builder()
            .name("ujraprobalkozasok")
            .description("Sikertelen első belépés után botonként ennyiszer próbálkozik újra.")
            .defaultValue(1)
            .range(0, 3)
            .sliderRange(0, 3)
            .build());

    private final Setting<Integer> retryDelaySeconds = timing.add(new IntSetting.Builder()
            .name("ujraprobalas-varakozas-masodperc")
            .description("Két sikertelen kapcsolódási próba közötti várakozás.")
            .defaultValue(3)
            .range(1, 30)
            .sliderRange(1, 30)
            .visible(() -> retryCount.get() > 0)
            .build());

    public LocalStressTestModule() {
        super(AopToolsAddon.CATEGORY, "helyi-stresszteszt-botok",
                "Korlátozott offline tesztklienseket csatlakoztat kizárólag saját localhost/LAN Paper szerverhez.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        AopToolsAddon.LOGGER.info("A Helyi stresszteszt botok modul aktiválása meghívódott.");
        LocalStressBotController.Result result = startNow();
        if (!result.success()) disable();
    }

    LocalStressBotController.Result startNow() {
        LocalStressBotController.Result result = controller.start(mc,
                host.get(), port.get(), botCount.get(), connectionDelaySeconds.get(),
                durationMinutes.get(), namePrefix.get(), customNames.get(),
                connectionTimeoutSeconds.get(), retryCount.get(), retryDelaySeconds.get());
        if (!result.success()) {
            error(result.message());
            AopToolsAddon.LOGGER.warn("A botindítás elutasítva: {}", result.message());
            return result;
        }
        info(result.message());
        AopToolsAddon.LOGGER.info("A botsegéd folyamat elindult: {}", result.message());
        return result;
    }

    @Override
    public void onDeactivate() {
        stopNow();
    }

    boolean stopNow() {
        boolean stopped = controller.stop();
        if (stopped) info("A helyi stresszteszt botjainak leállítása elindult.");
        return stopped;
    }

    boolean botsRunning() {
        return controller.isRunning();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        table.add(theme.label("A csempe bal kattintással is indít. Kézi vezérlés:", false)).expandX();
        table.row();
        WButton start = table.add(theme.button("BOTOK INDÍTÁSA")).expandX().widget();
        start.action = this::startNow;
        table.row();
        WButton stop = table.add(theme.button("BOTOK LEÁLLÍTÁSA")).expandX().widget();
        stop.action = this::stopNow;
        return table;
    }

    @Override
    public String getInfoString() {
        return controller.isRunning() ? botCount.get() + " bot • helyi" : null;
    }
}
