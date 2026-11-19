package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

public final class OperatorAutomation {
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final AutoOpModule autoOp;
    private final JoinCommandsModule joinCommands;
    private final OpEveryoneModule opEveryone;
    private final BlockWorkflowModule blockWorkflow;
    private final Queue<QueuedCommand> commandQueue = new ArrayDeque<>();
    private int commandCooldown;
    private int joinCountdown = -1;

    public OperatorAutomation(AutoOpModule autoOp, JoinCommandsModule joinCommands, OpEveryoneModule opEveryone,
                              BlockWorkflowModule blockWorkflow) {
        this.autoOp = autoOp;
        this.joinCommands = joinCommands;
        this.opEveryone = opEveryone;
        this.blockWorkflow = blockWorkflow;
    }

    public void onJoin() {
        commandQueue.clear();
        commandCooldown = 0;

        int delay = -1;
        if (joinCommands.isActive()) delay = Math.max(delay, joinCommands.joinDelayTicks.get());
        if (autoOp.isActive()) delay = Math.max(delay, autoOp.joinDelayTicks.get());
        if (blockWorkflow.runOnJoin.get()) delay = Math.max(delay, blockWorkflow.joinDelayTicks.get());
        joinCountdown = delay;
    }

    public void onDisconnect() {
        commandQueue.clear();
        commandCooldown = 0;
        joinCountdown = -1;
    }

    public void tick(Minecraft client) {
        if (joinCountdown > 0) {
            joinCountdown--;
            if (joinCountdown == 0) prepareJoinCommands(client);
        }

        if (commandQueue.isEmpty()) return;
        if (client.getConnection() == null || client.player == null) {
            commandQueue.clear();
            return;
        }

        if (commandCooldown > 0) {
            commandCooldown--;
            return;
        }

        QueuedCommand queued = commandQueue.remove();
        client.getConnection().sendCommand(queued.command());
        commandCooldown = queued.delayAfterTicks();

        if (commandQueue.isEmpty()) tell(client, "A parancssor végrehajtva.", ChatFormatting.GREEN);
    }

    public Result opEveryone(Minecraft client) {
        if (!isConnected(client)) return Result.error("Nem csatlakozol szerverhez.");
        if (!canUseCommand(client, "op")) {
            return Result.error("A szerver nem adott hozzáférést az /op parancshoz. Valószínűleg nincs elég jogosultságod.");
        }
        if (!commandQueue.isEmpty()) return Result.error("Már fut egy parancssor; várd meg, amíg befejeződik.");

        Set<String> names = onlinePlayerNames(client, false);
        if (names.isEmpty()) return Result.error("Nem található játékos a kliens játékoslistájában.");
        names.forEach(name -> enqueue("op " + name, opEveryone.commandDelayTicks.get()));
        return Result.success(names.size() + " játékos OP-olása elindult.");
    }

    public Result runWorkflow(Minecraft client, boolean dangerousConfirmed) {
        if (!isConnected(client)) return Result.error("Nem csatlakozol szerverhez.");
        if (!commandQueue.isEmpty()) return Result.error("Már fut egy parancssor; várd meg, amíg befejeződik.");

        List<WorkflowBlock> blocks = blockWorkflow.blocks();
        if (blocks.isEmpty()) return Result.error("A blokkprogram üres.");
        if (blocks.stream().anyMatch(block -> block.type().dangerous) && !dangerousConfirmed) {
            return Result.error("A kill/kick/ban blokkokhoz külön megerősítés szükséges.");
        }

        String missing = firstMissingPermission(client, blocks, true);
        if (missing != null) return Result.error("Nincs hozzáférésed ehhez a szerverparancshoz: /" + missing);

        List<String> commands = expandWorkflow(client, blocks, true);
        if (commands.isEmpty()) return Result.error("A programból nem készült futtatható parancs.");
        commands.forEach(command -> enqueue(command, blockWorkflow.commandDelayTicks.get()));
        return Result.success(commands.size() + " parancs bekerült a futtatási sorba.");
    }

    public Result runWorkflowFromBind(Minecraft client, boolean allowDangerous) {
        if (!isConnected(client)) return Result.error("Nem csatlakozol szerverhez.");
        if (!commandQueue.isEmpty()) return Result.error("Már fut egy parancssor; várd meg, amíg befejeződik.");

        List<WorkflowBlock> blocks = blockWorkflow.blocks();
        if (blocks.isEmpty()) return Result.error("A blokkprogram üres. Nyisd meg a csempét és húzz be blokkokat.");

        String missing = firstMissingPermission(client, blocks, allowDangerous);
        if (missing != null) return Result.error("Nincs hozzáférésed ehhez a szerverparancshoz: /" + missing);

        boolean skippedDangerous = !allowDangerous
                && blocks.stream().anyMatch(block -> block.type().dangerous);
        List<String> commands = expandWorkflow(client, blocks, allowDangerous);
        if (commands.isEmpty()) {
            if (skippedDangerous) {
                return Result.error("Csak KILL/KICK/BAN blokkok vannak, de az Auto kill/kick/ban kapcsoló KI van kapcsolva.");
            }
            return Result.error("A programból nem készült futtatható parancs.");
        }

        commands.forEach(command -> enqueue(command, blockWorkflow.commandDelayTicks.get()));
        String suffix = skippedDangerous ? " A KILL/KICK/BAN blokkok kimaradtak." : "";
        return Result.success(commands.size() + " parancs elindult a bindről." + suffix);
    }

    public boolean isBusy() {
        return !commandQueue.isEmpty();
    }

    public int cancelQueuedCommands() {
        int count = commandQueue.size();
        commandQueue.clear();
        commandCooldown = 0;
        return count;
    }

    public static boolean isValidPlayerName(String name) {
        return name != null && VALID_PLAYER_NAME.matcher(name).matches();
    }

    private void prepareJoinCommands(Minecraft client) {
        if (!isConnected(client)) return;

        String currentName = client.getUser().getName();
        String serverAddress = currentServerAddress(client);
        boolean preparedAnything = false;

        if (joinCommands.isActive()
                && matches(joinCommands.servers.get(), joinCommands.loginAccounts.get(), serverAddress, currentName)) {
            int validCount = 0;
            for (String configured : joinCommands.commands.get()) {
                String command = cleanCommand(configured);
                if (command != null) {
                    enqueue(command, joinCommands.commandDelayTicks.get());
                    validCount++;
                }
            }
            if (validCount > 0) {
                tell(client, validCount + " belépési parancs előkészítve.", ChatFormatting.YELLOW);
                preparedAnything = true;
            }
        }

        if (blockWorkflow.runOnJoin.get()
                && matchesSingle(blockWorkflow.server.get(), blockWorkflow.account.get(), serverAddress, currentName)) {
            List<WorkflowBlock> configured = blockWorkflow.blocks();
            if (!configured.isEmpty()) {
                boolean allowDangerous = blockWorkflow.allowDangerousOnJoin.get();
                String missing = firstMissingPermission(client, configured, allowDangerous);
                if (missing != null) {
                    tell(client, "A blokkprogram kimaradt: nincs hozzáférésed ehhez: /" + missing, ChatFormatting.RED);
                } else {
                    List<String> commands = expandWorkflow(client, configured, allowDangerous);
                    for (String command : commands) enqueue(command, blockWorkflow.commandDelayTicks.get());
                    if (!commands.isEmpty()) {
                        tell(client, commands.size() + " blokkparancs előkészítve.", ChatFormatting.YELLOW);
                        preparedAnything = true;
                    }
                    if (!allowDangerous && configured.stream().anyMatch(block -> block.type().dangerous)) {
                        tell(client, "A kill/kick/ban blokkok kihagyva, mert az automatikus veszélyes futtatás KI.",
                                ChatFormatting.GOLD);
                    }
                }
            }
        }

        if (autoOp.isActive() && matches(autoOp.servers.get(), autoOp.loginAccounts.get(), serverAddress, currentName)) {
            if (!canUseCommand(client, "op")) {
                tell(client, "Az Automata OP kimaradt: nincs hozzáférésed az /op parancshoz.", ChatFormatting.RED);
            } else {
                List<String> validTargets = new ArrayList<>();
                for (String target : autoOp.targetOperators.get()) {
                    if (isValidPlayerName(target) && !containsIgnoreCase(validTargets, target)) validTargets.add(target);
                }

                for (String target : validTargets) enqueue("op " + target, autoOp.commandDelayTicks.get());
                if (!validTargets.isEmpty()) {
                    tell(client, validTargets.size() + " célfiók OP-olása előkészítve.", ChatFormatting.YELLOW);
                    preparedAnything = true;
                }

                if (autoOp.deopLoginAccount.get() && isValidPlayerName(currentName)) {
                    enqueue("deop " + currentName, autoOp.commandDelayTicks.get());
                    preparedAnything = true;
                }
            }
        }

        if (!preparedAnything) {
            tell(client, "Nem futott belépési művelet: ellenőrizd a szerver-, fiók- és parancsbeállításokat.",
                    ChatFormatting.GRAY);
        }
    }

    private List<String> expandWorkflow(Minecraft client, List<WorkflowBlock> blocks, boolean allowDangerous) {
        List<String> commands = new ArrayList<>();
        Set<String> online = onlinePlayerNames(client, blockWorkflow.excludeSelf.get());

        for (WorkflowBlock block : blocks) {
            if (block.type().dangerous && !allowDangerous) continue;
            switch (block.type()) {
                case OP_ALL -> online.forEach(name -> commands.add("op " + name));
                case DEOP_ALL -> online.forEach(name -> commands.add("deop " + name));
                case KILL_ALL -> online.forEach(name -> commands.add("kill " + name));
                case KICK_ALL -> online.forEach(name -> commands.add("kick " + name + " AOP blokkprogram"));
                case BAN_ALL -> online.forEach(name -> commands.add("ban " + name + " AOP blokkprogram"));
                case OP_PLAYER -> {
                    if (isValidPlayerName(block.target())) commands.add("op " + block.target());
                }
                case DEOP_PLAYER -> {
                    if (isValidPlayerName(block.target())) commands.add("deop " + block.target());
                }
                case KILL_PLAYER -> {
                    if (isValidPlayerName(block.target())) commands.add("kill " + block.target());
                }
                case KICK_PLAYER -> {
                    if (isValidPlayerName(block.target())) {
                        commands.add("kick " + block.target() + " AOP blokkprogram");
                    }
                }
                case BAN_PLAYER -> {
                    if (isValidPlayerName(block.target())) {
                        commands.add("ban " + block.target() + " AOP blokkprogram");
                    }
                }
            }
        }
        return commands;
    }

    private static Set<String> onlinePlayerNames(Minecraft client, boolean excludeSelf) {
        Set<String> names = new LinkedHashSet<>();
        if (client.getConnection() == null) return names;
        String ownName = client.getUser().getName();
        for (PlayerInfo playerInfo : client.getConnection().getListedOnlinePlayers()) {
            String name = playerInfo.getProfile().name();
            if (!isValidPlayerName(name)) continue;
            if (excludeSelf && name.equalsIgnoreCase(ownName)) continue;
            names.add(name);
        }
        return names;
    }

    private static String firstMissingPermission(Minecraft client, List<WorkflowBlock> blocks, boolean includeDangerous) {
        Set<String> checked = new LinkedHashSet<>();
        for (WorkflowBlock block : blocks) {
            if (block.type().dangerous && !includeDangerous) continue;
            String command = switch (block.type()) {
                case OP_ALL, OP_PLAYER -> "op";
                case DEOP_ALL, DEOP_PLAYER -> "deop";
                case KILL_ALL, KILL_PLAYER -> "kill";
                case KICK_ALL, KICK_PLAYER -> "kick";
                case BAN_ALL, BAN_PLAYER -> "ban";
            };
            if (checked.add(command) && !canUseCommand(client, command)) return command;
        }
        return null;
    }

    private void enqueue(String command, int delay) {
        commandQueue.add(new QueuedCommand(command, Math.max(1, delay)));
    }

    private static String cleanCommand(String configured) {
        if (configured == null) return null;
        String command = configured.trim();
        while (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isEmpty() || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) return null;
        return command;
    }

    private static boolean matches(List<String> servers, List<String> accounts, String server, String account) {
        return !servers.isEmpty() && !accounts.isEmpty()
                && containsServer(servers, server)
                && containsIgnoreCase(accounts, account);
    }

    private static boolean matchesSingle(String configuredServer, String configuredAccount,
                                         String server, String account) {
        return configuredServer != null && configuredAccount != null
                && !configuredServer.isBlank() && !configuredAccount.isBlank()
                && serverMatches(configuredServer, server)
                && configuredAccount.trim().equalsIgnoreCase(account);
    }

    private static boolean containsServer(Collection<String> configuredServers, String currentServer) {
        for (String configured : configuredServers) {
            if (serverMatches(configured, currentServer)) return true;
        }
        return false;
    }

    private static boolean serverMatches(String configured, String current) {
        if (configured == null || current == null) return false;
        String left = normalizeServer(configured);
        String right = normalizeServer(current);
        if (left.equals(right)) return true;

        boolean leftHasPort = hasPort(left);
        boolean rightHasPort = hasPort(right);
        return (!leftHasPort || !rightHasPort) && hostOnly(left).equals(hostOnly(right));
    }

    private static String normalizeServer(String address) {
        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("minecraft://")) value = value.substring("minecraft://".length());
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static boolean hasPort(String address) {
        int colon = address.lastIndexOf(':');
        if (colon <= 0 || address.indexOf(':') != colon || colon == address.length() - 1) return false;
        for (int i = colon + 1; i < address.length(); i++) {
            if (!Character.isDigit(address.charAt(i))) return false;
        }
        return true;
    }

    private static String hostOnly(String address) {
        return hasPort(address) ? address.substring(0, address.lastIndexOf(':')) : address;
    }

    private static boolean isConnected(Minecraft client) {
        return client.getConnection() != null && client.player != null;
    }

    private static boolean canUseCommand(Minecraft client, String command) {
        return client.getConnection() != null
                && client.getConnection().getCommands().getRoot().getChild(command) != null;
    }

    private static String currentServerAddress(Minecraft client) {
        return client.getCurrentServer() == null ? "" : client.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(Collection<String> values, String wanted) {
        if (wanted == null) return false;
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(wanted)) return true;
        }
        return false;
    }

    private static void tell(Minecraft client, String message, ChatFormatting color) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[AOP] " + message).withStyle(color), false);
        }
    }

    private record QueuedCommand(String command, int delayAfterTicks) {}

    public record Result(boolean success, String message) {
        public static Result success(String message) { return new Result(true, message); }
        public static Result error(String message) { return new Result(false, message); }
    }
}
