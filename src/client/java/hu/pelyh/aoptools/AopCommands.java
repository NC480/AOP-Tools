package hu.pelyh.aoptools;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AopCommands {
    private final BlockWorkflowModule blockWorkflow;
    private final LocalStressTestModule localStressTest;
    private final OperatorAutomation automation;

    public AopCommands(BlockWorkflowModule blockWorkflow, LocalStressTestModule localStressTest,
                       OperatorAutomation automation) {
        this.blockWorkflow = blockWorkflow;
        this.localStressTest = localStressTest;
        this.automation = automation;
    }

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("aop")
                        .executes(context -> runAop(context.getSource()))
                        .then(literal("help").executes(context -> help(context.getSource())))
                        .then(literal("status").executes(context -> status(context.getSource())))
                        .then(literal("editor").executes(context -> openEditor(context.getSource())))
                        .then(literal("cancel").executes(context -> cancel(context.getSource())))
                        .then(literal("bots")
                                .executes(context -> startBots(context.getSource()))
                                .then(literal("stop").executes(context -> stopBots(context.getSource())))
                                .then(literal("status").executes(context -> botStatus(context.getSource()))))
        ));
    }

    private int runAop(FabricClientCommandSource source) {
        OperatorAutomation.Result result = automation.opEveryone(source.getClient());
        reply(source, result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        return result.success() ? 1 : 0;
    }

    private int help(FabricClientCommandSource source) {
        reply(source, "/aop – minden látható online játékos OP-olása", ChatFormatting.AQUA);
        reply(source, "/aop status – a menüpontok állapota", ChatFormatting.GRAY);
        reply(source, "/aop editor – a húzható Parancs Blokkok editor megnyitása", ChatFormatting.GRAY);
        reply(source, "/aop cancel – várakozó és folyamatos futtatás leállítása", ChatFormatting.GRAY);
        reply(source, "/aop bots – helyi tesztbotok indítása", ChatFormatting.GRAY);
        reply(source, "/aop bots stop – helyi tesztbotok leállítása", ChatFormatting.GRAY);
        return 1;
    }

    private int status(FabricClientCommandSource source) {
        reply(source, "Meteor GUI: AOP Manager kategória", ChatFormatting.AQUA);
        reply(source, "Presetek: " + blockWorkflow.presets().size() + " | aktív: "
                + blockWorkflow.activePreset.get() + " | " + blockWorkflow.blocks().size()
                + " blokk | autorun: " + onOff(blockWorkflow.autoRun.get()), ChatFormatting.GRAY);
        reply(source, "Futó parancssor: " + onOff(automation.isBusy()), ChatFormatting.GRAY);
        reply(source, "Helyi tesztbotok: " + onOff(localStressTest.botsRunning()), ChatFormatting.GRAY);
        return 1;
    }

    private int cancel(FabricClientCommandSource source) {
        int removed = automation.cancelQueuedCommands();
        reply(source, removed == 0 ? "Nem fut parancssor." : removed + " várakozó parancs törölve.",
                removed == 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
        return 1;
    }

    private int openEditor(FabricClientCommandSource source) {
        blockWorkflow.openEditor();
        reply(source, "A Parancs Blokkok editor megnyitva.", ChatFormatting.GREEN);
        return 1;
    }

    private int startBots(FabricClientCommandSource source) {
        LocalStressBotController.Result result = localStressTest.startNow();
        reply(source, result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        return result.success() ? 1 : 0;
    }

    private int stopBots(FabricClientCommandSource source) {
        boolean stopped = localStressTest.stopNow();
        reply(source, stopped ? "A helyi tesztbotok leállítása elindult." : "Nem fut helyi botfolyamat.",
                stopped ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        return stopped ? 1 : 0;
    }

    private int botStatus(FabricClientCommandSource source) {
        reply(source, "Helyi tesztbotok: " + onOff(localStressTest.botsRunning()), ChatFormatting.AQUA);
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "BE" : "KI";
    }

    private static void reply(FabricClientCommandSource source, String message, ChatFormatting color) {
        source.sendFeedback(Component.literal("[AOP] " + message).withStyle(color));
    }
}
