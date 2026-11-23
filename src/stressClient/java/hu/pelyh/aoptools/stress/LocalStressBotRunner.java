package hu.pelyh.aoptools.stress;

import hu.pelyh.aoptools.StressServerPolicy;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalStressBotRunner {
    private LocalStressBotRunner() { }

    public static void main(String[] args) {
        if (args.length != 10) fail("Hibás paraméterszám.");
        try {
            InetAddress address = StressServerPolicy.parseLocalAddress(args[0]);
            int port = Integer.parseInt(args[1]);
            int count = Integer.parseInt(args[2]);
            int delaySeconds = Integer.parseInt(args[3]);
            int durationMinutes = Integer.parseInt(args[4]);
            List<String> names = decodeLines(args[5]);
            String authorizedEndpoint = decodeText(args[6]);
            int connectionTimeoutSeconds = Integer.parseInt(args[7]);
            int retryCount = Integer.parseInt(args[8]);
            int retryDelaySeconds = Integer.parseInt(args[9]);
            validate(address, port, count, delaySeconds, durationMinutes, names, authorizedEndpoint,
                    connectionTimeoutSeconds, retryCount, retryDelaySeconds);

            CountDownLatch stop = new CountDownLatch(1);
            List<ClientSession> sessions = java.util.Collections.synchronizedList(new ArrayList<>());
            java.util.concurrent.CompletableFuture.delayedExecutor(durationMinutes, TimeUnit.MINUTES)
                    .execute(stop::countDown);
            startControlReader(stop);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> disconnectAll(sessions), "aop-stress-shutdown"));

            SessionService sessionService = new SessionService();
            for (int index = 0; index < count && stop.getCount() > 0; index++) {
                String username = names.get(index);
                ClientSession session = connect(address, port, username, sessionService, stop,
                        connectionTimeoutSeconds, retryCount, retryDelaySeconds);
                if (session != null) sessions.add(session);
                if (index + 1 < count) stop.await(delaySeconds, TimeUnit.SECONDS);
            }

            if (sessions.isEmpty()) {
                if (stop.getCount() > 0) {
                    fail("Egyetlen bot sem tudott belépni. Ellenőrizd, hogy a szerver fut, offline-mode-ban van, nincs tele, és a cím/port helyes.");
                }
                System.out.println("A helyi stresszteszt a belépés előtt leállt.");
            } else {
                System.out.println("Sikeresen bent lévő botok: " + sessions.size() + "/" + count);
            }

            stop.await();
            disconnectAll(sessions);
            TimeUnit.MILLISECONDS.sleep(500);
            System.out.println("A helyi stresszteszt befejeződött.");
            System.exit(0);
        } catch (Exception exception) {
            exception.printStackTrace(System.out);
            System.exit(2);
        }
    }

    private static ClientSession connect(InetAddress address, int port, String username,
                                         SessionService sessionService, CountDownLatch stop,
                                         int timeoutSeconds, int retryCount, int retryDelaySeconds)
            throws InterruptedException {
        for (int attempt = 1; attempt <= retryCount + 1 && stop.getCount() > 0; attempt++) {
            AtomicBoolean disconnected = new AtomicBoolean();
            ClientSession session = createSession(address, port, username, sessionService, disconnected,
                    timeoutSeconds);
            System.out.println("Csatlakozás indítása: " + username + " (" + attempt + "/" + (retryCount + 1) + ")");
            session.connect();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (stop.getCount() > 0 && System.nanoTime() < deadline && !disconnected.get()) {
                if (inGame(session)) {
                    stop.await(250, TimeUnit.MILLISECONDS);
                    if (inGame(session)) {
                        System.out.println(username + " sikeresen belépett.");
                        return session;
                    }
                }
                stop.await(50, TimeUnit.MILLISECONDS);
            }
            if (session.isConnected()) session.disconnect(Component.text("AOP kapcsolati próba lejárt"));
            if (attempt <= retryCount && stop.getCount() > 0) {
                System.out.println(username + " újrapróbálás " + retryDelaySeconds + " másodperc múlva.");
                stop.await(retryDelaySeconds, TimeUnit.SECONDS);
            }
        }
        System.out.println(username + " nem tudott belépni.");
        return null;
    }

    private static ClientSession createSession(InetAddress address, int port, String username,
                                               SessionService sessionService, AtomicBoolean disconnected,
                                               int timeoutSeconds) {
        MinecraftProtocol protocol = new MinecraftProtocol(username);
        ClientSession client = ClientNetworkSessionFactory.factory()
                .setRemoteSocketAddress(new InetSocketAddress(address, port))
                .setProtocol(protocol)
                .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        // A server transfer would open a second connection outside the approved endpoint.
        client.setFlag(MinecraftConstants.FOLLOW_TRANSFERS, false);
        client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        client.setFlag(BuiltinFlags.CLIENT_CONNECT_TIMEOUT, timeoutSeconds);
        client.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnected.set(true);
                System.out.println(username + " lecsatlakozott: " + event.getReason());
            }
        });
        return client;
    }

    private static boolean inGame(ClientSession session) {
        return session.isConnected()
                && session.getPacketProtocol().getInboundState() == ProtocolState.GAME
                && session.getPacketProtocol().getOutboundState() == ProtocolState.GAME;
    }

    private static void disconnectAll(List<ClientSession> sessions) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.isConnected()) session.disconnect(Component.text("AOP helyi stresszteszt leállítva"));
            }
        }
    }

    private static void startControlReader(CountDownLatch stop) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equalsIgnoreCase("STOP")) break;
                }
            } catch (Exception ignored) {
            } finally {
                stop.countDown();
            }
        }, "aop-stress-control");
        thread.setDaemon(true);
        thread.start();
    }

    private static List<String> decodeLines(String encoded) {
        String decoded = decodeText(encoded);
        if (decoded.isEmpty()) return List.of();
        return List.of(decoded.split("\\R"));
    }

    private static String decodeText(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static void validate(InetAddress address, int port, int count, int delaySeconds,
                                 int durationMinutes, List<String> names, String authorizedEndpoint,
                                 int connectionTimeoutSeconds, int retryCount, int retryDelaySeconds) {
        if (port < 1 || port > 65535) fail("Érvénytelen port.");
        if (count < 1 || count > 10) fail("Legfeljebb 10 bot engedélyezett.");
        if (delaySeconds < 1 || delaySeconds > 60) fail("A csatlakozási időköz 1-60 másodperc lehet.");
        if (durationMinutes < 1 || durationMinutes > 10) fail("A futási idő 1-10 perc lehet.");
        if (names.size() != count || !hu.pelyh.aoptools.StressBotNames.prepare(count, "AOPTest", names).equals(names))
            fail("Érvénytelen vagy duplikált botnévlista.");
        if (connectionTimeoutSeconds < 3 || connectionTimeoutSeconds > 30) fail("A timeout 3-30 másodperc lehet.");
        if (retryCount < 0 || retryCount > 3) fail("Az újrapróbálások száma 0-3 lehet.");
        if (retryDelaySeconds < 1 || retryDelaySeconds > 30) fail("Az újrapróbálási várakozás 1-30 másodperc lehet.");
        if (!StressServerPolicy.authorizes(address, port, authorizedEndpoint))
            fail("A háttérfolyamat célcíme nem egyezik az ellenőrzött helyi végponttal.");
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(2);
        throw new IllegalStateException(message);
    }
}
