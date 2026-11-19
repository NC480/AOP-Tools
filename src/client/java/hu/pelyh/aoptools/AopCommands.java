package hu.pelyh.aoptools;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AopCommands {
    private final AutoOpModule autoOp;
    private final JoinCommandsModule joinCommands;
    private final BlockWorkflowModule blockWorkflow;
    private final OperatorAutomation automation;

    public AopCommands(AutoOpModule autoOp, JoinCommandsModule joinCommands,
                       BlockWorkflowModule blockWorkflow, OperatorAutomation automation) {
        this.autoOp = autoOp;
        this.joinCommands = joinCommands;
        this.blockWorkflow = blockWorkflow;
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
                        .then(literal("auto")
                                .then(argument("érték", BoolArgumentType.bool())
                                        .executes(context -> setModule(context.getSource(), autoOp,
                                                BoolArgumentType.getBool(context, "érték"), "Automata OP"))))
                        .then(literal("join-commands")
                                .then(argument("érték", BoolArgumentType.bool())
                                        .executes(context -> setModule(context.getSource(), joinCommands,
                                                BoolArgumentType.getBool(context, "érték"), "Belépési Parancsok"))))
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
        reply(source, "/aop auto <true|false> – Automata OP be/ki", ChatFormatting.GRAY);
        reply(source, "/aop join-commands <true|false> – Belépési Parancsok be/ki", ChatFormatting.GRAY);
        reply(source, "/aop cancel – várakozó parancsok törlése", ChatFormatting.GRAY);
        return 1;
    }

    private int status(FabricClientCommandSource source) {
        reply(source, "Meteor GUI: AOP Manager kategória", ChatFormatting.AQUA);
        reply(source, "Automata OP: " + onOff(autoOp.isActive())
                + " | célfiókok: " + autoOp.targetOperators.get().size(), ChatFormatting.GRAY);
        reply(source, "Belépési Parancsok: " + onOff(joinCommands.isActive())
                + " | parancsok: " + joinCommands.commands.get().size(), ChatFormatting.GRAY);
        reply(source, "Parancs Blokkok: " + blockWorkflow.blocks().size()
                + " blokk | belépéskor: " + onOff(blockWorkflow.runOnJoin.get()), ChatFormatting.GRAY);
        reply(source, "Futó parancssor: " + onOff(automation.isBusy()), ChatFormatting.GRAY);
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

    private int setModule(FabricClientCommandSource source,
                          meteordevelopment.meteorclient.systems.modules.Module module,
                          boolean enabled,
                          String label) {
        if (enabled) module.enable();
        else module.disable();
        reply(source, label + ": " + onOff(enabled), ChatFormatting.GREEN);
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "BE" : "KI";
    }

    private static void reply(FabricClientCommandSource source, String message, ChatFormatting color) {
        source.sendFeedback(Component.literal("[AOP] " + message).withStyle(color));
    }
}
