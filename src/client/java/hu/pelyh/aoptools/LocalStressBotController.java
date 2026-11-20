package hu.pelyh.aoptools;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

final class LocalStressBotController {
    private static final Pattern PREFIX = Pattern.compile("[A-Za-z0-9_]{1,10}");
    private final Runnable onExit;
    private Process process;
    private BufferedWriter control;

    LocalStressBotController(Runnable onExit) {
        this.onExit = onExit;
    }

    synchronized Result start(Minecraft client, String requestedHost, int port, int count,
                              int delaySeconds, int durationMinutes, String prefix) {
        if (isRunning()) return Result.error("A helyi stresszteszt már fut.");
        if (port < 1 || port > 65535) return Result.error("Érvénytelen szerverport.");
        if (count < 1 || count > 10) return Result.error("A botszám csak 1 és 10 között lehet.");
        if (delaySeconds < 1 || delaySeconds > 10)
            return Result.error("A csatlakozások között 1-10 másodperc lehet.");
        if (durationMinutes < 1 || durationMinutes > 10)
            return Result.error("A futási idő 1-10 perc lehet.");
        if (prefix == null || !PREFIX.matcher(prefix.trim()).matches())
            return Result.error("A névelőtag 1-10 angol betűt, számot vagy aláhúzást tartalmazhat.");

        final InetAddress address;
        try {
            address = resolvePrivateAddress(requestedHost);
        } catch (IOException exception) {
            return Result.error(exception.getMessage());
        }

        try {
            Path directory = client.gameDirectory.toPath().resolve("config").resolve("aop-tools");
            Files.createDirectories(directory);
            Path runner = extractRunner(directory);
            Path log = directory.resolve("stress-test.log");
            Path java = javaExecutable();

            List<String> command = new ArrayList<>();
            command.add(java.toString());
            command.add("-Xms64m");
            command.add("-Xmx256m");
            command.add("-jar");
            command.add(runner.toString());
            command.add(address.getHostAddress());
            command.add(Integer.toString(port));
            command.add(Integer.toString(count));
            command.add(Integer.toString(delaySeconds));
            command.add(Integer.toString(durationMinutes));
            command.add(prefix.trim());

            Process started = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            process = started;
            control = new BufferedWriter(new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8));
            Thread.ofVirtual().name("aop-stress-client-watch").start(() -> watch(started));
            return Result.success(count + " helyi tesztbot indítása elkezdődött. Napló: " + log);
        } catch (IOException exception) {
            AopToolsAddon.LOGGER.error("A helyi stresszteszt nem indítható.", exception);
            clear();
            return Result.error("Nem sikerült elindítani a helyi tesztklienst: " + exception.getMessage());
        }
    }

    synchronized boolean stop() {
        if (!isRunning()) {
            clear();
            return false;
        }
        Process current = process;
        try {
            control.write("STOP");
            control.newLine();
            control.flush();
        } catch (IOException exception) {
            current.destroy();
        }
        Thread.ofVirtual().name("aop-stress-client-stop").start(() -> {
            try {
                if (!current.waitFor(5, TimeUnit.SECONDS)) current.destroyForcibly();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                current.destroyForcibly();
            }
        });
        return true;
    }

    synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    private void watch(Process watched) {
        try {
            int exitCode = watched.waitFor();
            AopToolsAddon.LOGGER.info("A helyi stresszteszt kliens kilépett ({}).", exitCode);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (this) {
                if (process == watched) clear();
            }
            onExit.run();
        }
    }

    private Path extractRunner(Path directory) throws IOException {
        String version = FabricLoader.getInstance().getModContainer(AopToolsAddon.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        Path target = directory.resolve("aop-stress-client-" + version + ".jar");
        Path temporary = directory.resolve("aop-stress-client-" + version + ".tmp");
        try (InputStream input = LocalStressBotController.class.getResourceAsStream("/aoptools/aop-stress-client.jar")) {
            if (input == null) throw new IOException("A beépített stresszteszt-kliens hiányzik a mod JAR-ból.");
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static Path javaExecutable() throws IOException {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "javaw.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        if (!Files.isRegularFile(java)) throw new IOException("Nem található a Java futtatókörnyezet: " + java);
        return java;
    }

    private static InetAddress resolvePrivateAddress(String requestedHost) throws IOException {
        String host = requestedHost == null ? "" : requestedHost.trim();
        if (host.isEmpty()) throw new IOException("A szervercím üres.");
        if (!host.equalsIgnoreCase("localhost") && !looksLikeIpLiteral(host))
            throw new IOException("Biztonsági okból csak localhost vagy közvetlen privát IP-cím használható.");
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress() && !address.isSiteLocalAddress() && !address.isLinkLocalAddress())
            throw new IOException("A cél nem loopback vagy privát LAN-cím; a stresszteszt megtagadva.");
        return address;
    }

    private static boolean looksLikeIpLiteral(String host) {
        return host.matches("[0-9.]+") || host.matches("[0-9A-Fa-f:]+") || host.startsWith("[") && host.endsWith("]");
    }

    private synchronized void clear() {
        if (control != null) {
            try { control.close(); } catch (IOException ignored) { }
        }
        control = null;
        process = null;
    }

    record Result(boolean success, String message) {
        static Result success(String message) { return new Result(true, message); }
        static Result error(String message) { return new Result(false, message); }
    }
}
