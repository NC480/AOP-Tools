package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

public final class LocalStressTestModule extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final LocalStressBotController controller = new LocalStressBotController(() -> mc.execute(() -> {
        if (isActive()) {
            info("A helyi stresszteszt befejeződött vagy a segédkliens kilépett.");
            disable();
        }
    }));

    private final Setting<Boolean> ownServer = general.add(new BoolSetting.Builder()
            .name("sajat-szerver-megerositese")
            .description("Csak akkor kapcsold be, ha a cél a saját helyi vagy LAN szervered.")
            .defaultValue(false)
            .build());

    private final Setting<String> host = general.add(new StringSetting.Builder()
            .name("helyi-szerver-cime")
            .description("Csak localhost, loopback vagy privát LAN IP-cím engedélyezett.")
            .defaultValue("127.0.0.1")
            .build());

    private final Setting<Integer> port = general.add(new IntSetting.Builder()
            .name("port")
            .description("A helyi Paper szerver portja.")
            .defaultValue(25565)
            .range(1, 65535)
            .sliderRange(1024, 65535)
            .build());

    private final Setting<Integer> botCount = general.add(new IntSetting.Builder()
            .name("botok-szama")
            .description("Egyszerre legfeljebb 10 tesztkliens engedélyezett.")
            .defaultValue(3)
            .range(1, 10)
            .sliderRange(1, 10)
            .build());

    private final Setting<Integer> connectionDelaySeconds = general.add(new IntSetting.Builder()
            .name("csatlakozasok-kozti-masodperc")
            .description("Biztonsági korlát: két bot között legalább egy másodperc telik el.")
            .defaultValue(1)
            .range(1, 10)
            .sliderRange(1, 10)
            .build());

    private final Setting<Integer> durationMinutes = general.add(new IntSetting.Builder()
            .name("maximalis-futasi-ido-perc")
            .description("Az idő lejártakor minden bot automatikusan lecsatlakozik.")
            .defaultValue(3)
            .range(1, 10)
            .sliderRange(1, 10)
            .build());

    private final Setting<String> namePrefix = general.add(new StringSetting.Builder()
            .name("bot-nev-elotag")
            .description("1-10 angol betű, szám vagy aláhúzás; a sorszám automatikus.")
            .defaultValue("AOPTest")
            .build());

    public LocalStressTestModule() {
        super(AopToolsAddon.CATEGORY, "helyi-stresszteszt-botok",
                "Korlátozott offline tesztklienseket csatlakoztat kizárólag saját localhost/LAN Paper szerverhez.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        if (!ownServer.get()) {
            error("Előbb kapcsold be a Saját szerver megerősítése beállítást.");
            disable();
            return;
        }

        LocalStressBotController.Result result = controller.start(mc,
                host.get(), port.get(), botCount.get(), connectionDelaySeconds.get(),
                durationMinutes.get(), namePrefix.get());
        if (!result.success()) {
            error(result.message());
            disable();
            return;
        }
        info(result.message());
    }

    @Override
    public void onDeactivate() {
        if (controller.stop()) info("A helyi stresszteszt botjainak leállítása elindult.");
    }

    @Override
    public String getInfoString() {
        return controller.isRunning() ? botCount.get() + " bot • helyi" : null;
    }
}
