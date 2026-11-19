package hu.pelyh.aoptools;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

public final class OperatorAutomation {
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private final OpEveryoneModule opEveryone;
    private final BlockWorkflowModule blockWorkflow;
    private final Queue<QueuedCommand> commandQueue = new ArrayDeque<>();
    private List<WorkflowBlock> continuousBlocks = List.of();
    private int commandCooldown;
    private int continuousCountdown;
    private int autoRunCountdown = -1;

    public OperatorAutomation(OpEveryoneModule opEveryone, BlockWorkflowModule blockWorkflow) {
        this.opEveryone = opEveryone;
        this.blockWorkflow = blockWorkflow;
    }

    public void onJoin() {
        cancelAll();
        autoRunCountdown = blockWorkflow.autoRun.get() ? 60 : -1;
    }
    public void onDisconnect() { cancelAll(); }

    public void tick(Minecraft client) {
        if (!isConnected(client)) { cancelAll(); return; }
        if (autoRunCountdown > 0) {
            autoRunCountdown--;
            if (autoRunCountdown == 0) {
                Result result = toggleWorkflow(client, true, false);
                tell(client, "Autorun: " + result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
            }
        }
        if (!continuousBlocks.isEmpty()) {
            if (continuousCountdown > 0) continuousCountdown--;
            else {
                for (WorkflowBlock block : continuousBlocks) {
                    String command = continuousCommand(client, block);
                    if (command != null) client.getConnection().sendCommand(command);
                }
                continuousCountdown = Math.max(1, blockWorkflow.commandDelayTicks.get());
            }
        }
        if (commandQueue.isEmpty()) return;
        if (commandCooldown > 0) { commandCooldown--; return; }
        QueuedCommand queued = commandQueue.remove();
        client.getConnection().sendCommand(queued.command());
        commandCooldown = queued.delayAfterTicks();
        if (commandQueue.isEmpty()) tell(client, "A parancssor végrehajtva.", ChatFormatting.GREEN);
    }

    public Result opEveryone(Minecraft client) {
        if (!isConnected(client)) return Result.error("Nem csatlakozol szerverhez.");
        if (!canUseCommand(client, "op")) return Result.error("Nincs hozzáférésed az /op parancshoz.");
        if (!commandQueue.isEmpty()) return Result.error("Már fut egy parancssor.");
        Set<String> names = onlinePlayerNames(client, false);
        if (names.isEmpty()) return Result.error("Nem található játékos a játékoslistában.");
        names.forEach(name -> enqueue("op " + name));
        return Result.success(names.size() + " játékos OP-olása elindult.");
    }

    public Result runWorkflow(Minecraft client, boolean dangerousConfirmed) {
        return toggleWorkflow(client, dangerousConfirmed, false);
    }

    public Result runWorkflowFromBind(Minecraft client, boolean allowDangerous) {
        return toggleWorkflow(client, allowDangerous, true);
    }

    private Result toggleWorkflow(Minecraft client, boolean allowDangerous, boolean fromBind) {
        if (!isConnected(client)) return Result.error("Nem csatlakozol szerverhez.");
        if (!continuousBlocks.isEmpty()) {
            continuousBlocks = List.of();
            continuousCountdown = 0;
            return Result.success("A folyamatos láb-alá blokk futtatás leállt.");
        }
        if (!commandQueue.isEmpty()) return Result.error("Már fut egy parancssor.");
        List<WorkflowBlock> blocks = blockWorkflow.blocks();
        if (blocks.isEmpty()) return Result.error("A blokkprogram üres.");
        if (blocks.stream().anyMatch(block -> block.type().dangerous) && !allowDangerous)
            return Result.error("A KILL/KICK/BAN blokkokhoz megerősítés szükséges.");
        String missing = firstMissingPermission(client, blocks, allowDangerous);
        if (missing != null) return Result.error("Nincs hozzáférésed ehhez a szerverparancshoz: /" + missing);

        List<WorkflowBlock> continuous = blocks.stream().filter(block -> block.type().continuous()).toList();
        List<String> commands = expandOneShot(client, blocks, allowDangerous);
        commands.forEach(this::enqueue);
        if (!continuous.isEmpty()) { continuousBlocks = List.copyOf(continuous); continuousCountdown = 0; }
        if (commands.isEmpty() && continuous.isEmpty()) return Result.error("Nincs futtatható blokk.");
        String origin = fromBind ? " a bindről" : "";
        String ongoing = continuous.isEmpty() ? "" : " A láb-alá blokkok folyamatosan futnak; ugyanazzal a binddel leállíthatók.";
        return Result.success(commands.size() + " egyszeri parancs elindult" + origin + "." + ongoing);
    }

    public boolean isBusy() { return !commandQueue.isEmpty() || !continuousBlocks.isEmpty(); }

    public int cancelQueuedCommands() {
        int count = commandQueue.size() + continuousBlocks.size();
        cancelAll();
        return count;
    }

    private void cancelAll() {
        commandQueue.clear(); continuousBlocks = List.of(); commandCooldown = 0; continuousCountdown = 0; autoRunCountdown = -1;
    }

    public static boolean isValidPlayerName(String name) {
        return name != null && VALID_PLAYER_NAME.matcher(name).matches();
    }

    private List<String> expandOneShot(Minecraft client, List<WorkflowBlock> blocks, boolean allowDangerous) {
        List<String> commands = new ArrayList<>();
        Set<String> online = onlinePlayerNames(client, blockWorkflow.excludeSelf.get());
        for (WorkflowBlock block : blocks) {
            if (block.type().continuous() || block.type().dangerous && !allowDangerous) continue;
            switch (block.type()) {
                case OP_ALL -> online.forEach(name -> commands.add("op " + name));
                case DEOP_ALL -> online.forEach(name -> commands.add("deop " + name));
                case KILL_ALL -> online.forEach(name -> commands.add("kill " + name));
                case KICK_ALL -> online.forEach(name -> commands.add("kick " + name + " AOP blokkprogram"));
                case BAN_ALL -> online.forEach(name -> commands.add("ban " + name + " AOP blokkprogram"));
                case OP_PLAYER -> commands.add("op " + block.target());
                case DEOP_PLAYER -> commands.add("deop " + block.target());
                case KILL_PLAYER -> commands.add("kill " + block.target());
                case KICK_PLAYER -> commands.add("kick " + block.target() + " AOP blokkprogram");
                case BAN_PLAYER -> commands.add("ban " + block.target() + " AOP blokkprogram");
                case BLOCK_UNDER_ALL, BLOCK_UNDER_PLAYER -> { }
            }
        }
        return commands;
    }

    private String continuousCommand(Minecraft client, WorkflowBlock block) {
        String value = WorkflowBlock.normalizeBlockId(block.value());
        if (!WorkflowBlock.isValidBlockId(value)) return null;
        return switch (block.type()) {
            case BLOCK_UNDER_ALL -> {
                String selector = "@a";
                String own = client.getUser().getName();
                if (blockWorkflow.excludeSelf.get() && isValidPlayerName(own)) selector = "@a[name=!" + own + "]";
                yield "execute as " + selector + " at @s run setblock ~ ~-1 ~ " + value + " replace";
            }
            case BLOCK_UNDER_PLAYER -> "execute at " + block.target() + " run setblock ~ ~-1 ~ " + value + " replace";
            default -> null;
        };
    }

    private String firstMissingPermission(Minecraft client, List<WorkflowBlock> blocks, boolean includeDangerous) {
        Set<String> checked = new LinkedHashSet<>();
        for (WorkflowBlock block : blocks) {
            if (block.type().dangerous && !includeDangerous) continue;
            String command = switch (block.type()) {
                case OP_ALL, OP_PLAYER -> "op";
                case DEOP_ALL, DEOP_PLAYER -> "deop";
                case KILL_ALL, KILL_PLAYER -> "kill";
                case KICK_ALL, KICK_PLAYER -> "kick";
                case BAN_ALL, BAN_PLAYER -> "ban";
                case BLOCK_UNDER_ALL, BLOCK_UNDER_PLAYER -> "execute";
            };
            if (checked.add(command) && !canUseCommand(client, command)) return command;
        }
        return null;
    }

    private void enqueue(String command) {
        commandQueue.add(new QueuedCommand(command, Math.max(1, blockWorkflow.commandDelayTicks.get())));
    }

    private static Set<String> onlinePlayerNames(Minecraft client, boolean excludeSelf) {
        Set<String> names = new LinkedHashSet<>();
        if (client.getConnection() == null) return names;
        String own = client.getUser().getName();
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            String name = info.getProfile().name();
            if (isValidPlayerName(name) && (!excludeSelf || !name.equalsIgnoreCase(own))) names.add(name);
        }
        return names;
    }

    private static boolean isConnected(Minecraft client) { return client.getConnection() != null && client.player != null; }
    private static boolean canUseCommand(Minecraft client, String command) {
        return client.getConnection() != null && client.getConnection().getCommands().getRoot().getChild(command) != null;
    }
    private static void tell(Minecraft client, String message, ChatFormatting color) {
        if (client.player != null) client.player.displayClientMessage(Component.literal("[AOP] " + message).withStyle(color), false);
    }

    private record QueuedCommand(String command, int delayAfterTicks) { }
    public record Result(boolean success, String message) {
        public static Result success(String message) { return new Result(true, message); }
        public static Result error(String message) { return new Result(false, message); }
    }
}
