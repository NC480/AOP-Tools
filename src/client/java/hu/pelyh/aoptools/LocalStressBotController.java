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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

final class LocalStressBotController {
    private final BiConsumer<Integer, String> onExit;
    private Process process;
    private BufferedWriter control;
    private Path logFile;

    LocalStressBotController(BiConsumer<Integer, String> onExit) {
        this.onExit = onExit;
    }

    synchronized Result start(Minecraft client, String requestedHost, int port, int count,
                              int delaySeconds, int durationMinutes, String prefix,
                              List<String> requestedNames, int connectionTimeoutSeconds,
                              int retryCount, int retryDelaySeconds) {
        if (isRunning()) return Result.error("A helyi stresszteszt már fut.");
        if (port < 1 || port > 65535) return Result.error("Érvénytelen szerverport.");
        if (count < 1 || count > 10) return Result.error("A botszám csak 1 és 10 között lehet.");
        if (delaySeconds < 1 || delaySeconds > 60)
            return Result.error("A csatlakozások között 1-60 másodperc lehet.");
        if (durationMinutes < 1 || durationMinutes > 10)
            return Result.error("A futási idő 1-10 perc lehet.");
        if (connectionTimeoutSeconds < 3 || connectionTimeoutSeconds > 30)
            return Result.error("A kapcsolati timeout 3-30 másodperc lehet.");
        if (retryCount < 0 || retryCount > 3)
            return Result.error("Az újrapróbálások száma 0-3 lehet.");
        if (retryDelaySeconds < 1 || retryDelaySeconds > 30)
            return Result.error("Az újrapróbálási várakozás 1-30 másodperc lehet.");

        final List<String> botNames;
        try {
            botNames = StressBotNames.prepare(count, prefix, requestedNames);
        } catch (IllegalArgumentException exception) {
            return Result.error(exception.getMessage());
        }

        final InetAddress address;
        try {
            address = StressServerPolicy.parseLocalAddress(requestedHost);
        } catch (IOException | IllegalArgumentException exception) {
            return Result.error(exception.getMessage());
        }
        String endpoint = StressServerPolicy.endpoint(address, port);

        try {
            Path directory = client.gameDirectory.toPath().resolve("config").resolve("aop-tools");
            Files.createDirectories(directory);
            Path runner = extractRunner(directory);
            Path log = directory.resolve("stress-test.log");
            Path java = javaExecutable();

            List<String> command = new ArrayList<>();
            command.add(java.toString());
            command.add("-Dfile.encoding=UTF-8");
            command.add("-Xms64m");
            command.add("-Xmx256m");
            command.add("-jar");
            command.add(runner.toString());
            command.add(address.getHostAddress());
            command.add(Integer.toString(port));
            command.add(Integer.toString(count));
            command.add(Integer.toString(delaySeconds));
            command.add(Integer.toString(durationMinutes));
            command.add(Base64.getUrlEncoder().withoutPadding().encodeToString(
                    String.join("\n", botNames).getBytes(StandardCharsets.UTF_8)));
            command.add(Base64.getUrlEncoder().withoutPadding().encodeToString(
                    endpoint.getBytes(StandardCharsets.UTF_8)));
            command.add(Integer.toString(connectionTimeoutSeconds));
            command.add(Integer.toString(retryCount));
            command.add(Integer.toString(retryDelaySeconds));

            Process started = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            process = started;
            logFile = log;
            control = new BufferedWriter(new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8));
            started.onExit().thenRun(() -> client.execute(() -> finished(started)));
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

    private synchronized void finished(Process watched) {
        if (process != watched) return;
        int exitCode = watched.exitValue();
        String detail = exitCode == 0 ? "" : lastLogMessage();
        AopToolsAddon.LOGGER.info("A helyi stresszteszt kliens kilépett ({}).", exitCode);
        clear();
        onExit.accept(exitCode, detail);
    }

    private String lastLogMessage() {
        if (logFile == null || !Files.isRegularFile(logFile)) return "";
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            for (int index = lines.size() - 1; index >= 0; index--) {
                String line = lines.get(index).trim();
                if (!line.isEmpty()) return line.length() <= 180 ? line : line.substring(0, 177) + "...";
            }
        } catch (IOException ignored) { }
        return "";
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

    private synchronized void clear() {
        if (control != null) {
            try { control.close(); } catch (IOException ignored) { }
        }
        control = null;
        process = null;
        logFile = null;
    }

    record Result(boolean success, String message) {
        static Result success(String message) { return new Result(true, message); }
        static Result error(String message) { return new Result(false, message); }
    }
}
