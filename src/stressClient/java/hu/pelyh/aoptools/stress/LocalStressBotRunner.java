package hu.pelyh.aoptools.stress;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class LocalStressBotRunner {
    private static final Pattern PREFIX = Pattern.compile("[A-Za-z0-9_]{1,10}");

    private LocalStressBotRunner() { }

    public static void main(String[] args) {
        if (args.length != 7) fail("Hibás paraméterszám.");
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            int count = Integer.parseInt(args[2]);
            int delaySeconds = Integer.parseInt(args[3]);
            int durationMinutes = Integer.parseInt(args[4]);
            String prefix = args[5];
            Set<String> whitelist = Set.of(new String(Base64.getUrlDecoder().decode(args[6]),
                    StandardCharsets.UTF_8).split("\\R"));
            validate(address, port, count, delaySeconds, durationMinutes, prefix, whitelist);

            CountDownLatch stop = new CountDownLatch(1);
            List<ClientSession> sessions = java.util.Collections.synchronizedList(new ArrayList<>());
            java.util.concurrent.CompletableFuture.delayedExecutor(durationMinutes, TimeUnit.MINUTES)
                    .execute(stop::countDown);
            startControlReader(stop);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> disconnectAll(sessions), "aop-stress-shutdown"));

            SessionService sessionService = new SessionService();
            for (int index = 1; index <= count && stop.getCount() > 0; index++) {
                String username = username(prefix, index);
                ClientSession session = createSession(address, port, username, sessionService);
                sessions.add(session);
                System.out.println("Csatlakozás indítása: " + username);
                session.connect();
                if (index < count) stop.await(delaySeconds, TimeUnit.SECONDS);
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

    private static ClientSession createSession(InetAddress address, int port, String username,
                                               SessionService sessionService) {
        MinecraftProtocol protocol = new MinecraftProtocol(username);
        ClientSession client = ClientNetworkSessionFactory.factory()
                .setRemoteSocketAddress(new InetSocketAddress(address, port))
                .setProtocol(protocol)
                .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        client.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                System.out.println(username + " lecsatlakozott: " + event.getReason());
            }
        });
        return client;
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

    private static String username(String prefix, int index) {
        String suffix = String.format("%03d", index);
        int maxPrefix = 16 - suffix.length();
        return prefix.substring(0, Math.min(prefix.length(), maxPrefix)) + suffix;
    }

    private static void validate(InetAddress address, int port, int count, int delaySeconds,
                                 int durationMinutes, String prefix, Set<String> whitelist) {
        if (!address.isLoopbackAddress() && !address.isSiteLocalAddress() && !address.isLinkLocalAddress())
            fail("Csak loopback vagy privát LAN-cím engedélyezett.");
        if (port < 1 || port > 65535) fail("Érvénytelen port.");
        if (count < 1 || count > 10) fail("Legfeljebb 10 bot engedélyezett.");
        if (delaySeconds < 1 || delaySeconds > 10) fail("A csatlakozási időköz 1-10 másodperc lehet.");
        if (durationMinutes < 1 || durationMinutes > 10) fail("A futási idő 1-10 perc lehet.");
        if (!PREFIX.matcher(prefix).matches()) fail("Érvénytelen botnév-előtag.");
        String host = address.getHostAddress();
        String endpoint = (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
        if (!whitelist.contains(endpoint)) fail("A cél nincs a bot szerver-whitelistjén.");
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(2);
        throw new IllegalStateException(message);
    }
}
