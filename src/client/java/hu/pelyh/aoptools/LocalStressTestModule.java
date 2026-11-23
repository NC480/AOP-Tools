package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.ArrayList;
import java.util.List;

public final class LocalStressTestModule extends Module {
    private final SettingGroup server = settings.createGroup("Cél szerver");
    private final SettingGroup identities = settings.createGroup("Botnevek");
    private final SettingGroup timing = settings.createGroup("Időzítés és újrapróbálás");
    private final StressServerWhitelistModule whitelist;
    private final LocalStressBotController controller = new LocalStressBotController(exitCode -> {
        if (isActive()) {
            if (exitCode == 0) info("A helyi stresszteszt befejeződött.");
            else error("A botok nem tudtak belépni. Részletek: config/aop-tools/stress-test.log");
            disable();
        }
    });

    private final Setting<Boolean> ownServer = server.add(new BoolSetting.Builder()
            .name("sajat-szerver-megerositese")
            .description("Csak akkor kapcsold be, ha a cél a saját helyi vagy LAN szervered.")
            .defaultValue(false)
            .build());

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
            .description("Egyszerre legfeljebb 10 tesztkliens engedélyezett.")
            .defaultValue(3)
            .range(1, 10)
            .sliderRange(1, 10)
            .build());

    private final Setting<List<String>> customNames = identities.add(new StringListSetting.Builder()
            .name("egyedi-botnevek")
            .description("Opcionális Minecraft-nevek, soronként egy. A lista első Botszám darab eleme használódik; a hiányzó neveket az előtag tölti ki.")
            .defaultValue(new ArrayList<>())
            .build());

    private final Setting<String> namePrefix = identities.add(new StringSetting.Builder()
            .name("bot-nev-elotag")
            .description("A hiányzó egyedi nevek képzése: 1-10 angol betű, szám vagy aláhúzás; a sorszám automatikus.")
            .defaultValue("AOPTest")
            .build());

    private final Setting<Integer> connectionDelaySeconds = timing.add(new IntSetting.Builder()
            .name("csatlakozasok-kozti-masodperc")
            .description("Milyen gyakran lépjen be a következő bot. Biztonsági minimum: 1 másodperc.")
            .defaultValue(1)
            .range(1, 60)
            .sliderRange(1, 60)
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

    public LocalStressTestModule(StressServerWhitelistModule whitelist) {
        super(AopToolsAddon.CATEGORY, "helyi-stresszteszt-botok",
                "Korlátozott offline tesztklienseket csatlakoztat kizárólag saját localhost/LAN Paper szerverhez.");
        this.whitelist = whitelist;
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
                durationMinutes.get(), namePrefix.get(), customNames.get(),
                connectionTimeoutSeconds.get(), retryCount.get(), retryDelaySeconds.get(),
                whitelist.entries());
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
