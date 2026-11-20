package hu.pelyh.aoptools;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AopToolsAddon extends MeteorAddon {
    public static final String MOD_ID = "aoptools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Category CATEGORY = new Category("AOP Manager");

    @Override
    public void onInitialize() {
        OpEveryoneModule opEveryone = new OpEveryoneModule();
        BlockWorkflowModule blockWorkflow = new BlockWorkflowModule();
        LocalStressTestModule localStressTest = new LocalStressTestModule();
        Modules.get().add(opEveryone);
        Modules.get().add(blockWorkflow);
        Modules.get().add(localStressTest);

        OperatorAutomation automation = new OperatorAutomation(opEveryone, blockWorkflow);
        opEveryone.bind(automation);
        blockWorkflow.bind(automation);
        new AopCommands(blockWorkflow, automation).register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> automation.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> automation.onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(automation::tick);
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> automation.allowGameMessage(message));

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
