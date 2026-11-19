package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AopToolsAddon extends MeteorAddon {
    public static final String MOD_ID = "aoptools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Category CATEGORY = new Category("AOP Manager");

    @Override
    public void onInitialize() {
        AutoOpModule autoOp = new AutoOpModule();
        JoinCommandsModule joinCommands = new JoinCommandsModule();
        OpEveryoneModule opEveryone = new OpEveryoneModule();
        BlockWorkflowModule blockWorkflow = new BlockWorkflowModule();
        Modules.get().add(autoOp);
        Modules.get().add(joinCommands);
        Modules.get().add(opEveryone);
        Modules.get().add(blockWorkflow);

        OperatorAutomation automation = new OperatorAutomation(autoOp, joinCommands, opEveryone, blockWorkflow);
        opEveryone.bind(automation);
        blockWorkflow.bind(automation);
        new AopCommands(autoOp, joinCommands, blockWorkflow, automation).register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> automation.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> automation.onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(automation::tick);

        LOGGER.info("AOP Tools Meteor addon initialized.");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "hu.pelyh.aoptools";
    }
}
