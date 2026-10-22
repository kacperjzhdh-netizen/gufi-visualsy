package pl.fazevisuals;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class FazeVisualsClient implements ClientModInitializer {
    public static boolean hudEnabled = true;
    public static volatile long hitFlashStart = 0;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        LicenseManager.init();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fazevisuals.toggle", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.fazevisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) hudEnabled = !hudEnabled;
            LicenseManager.tick();
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && LicenseManager.isActive() && entity instanceof LivingEntity) {
                hitFlashStart = System.currentTimeMillis();
                MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.6f));
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register(HudRenderer::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fazevisuals")
                        .then(ClientCommandManager.literal("key")
                                .then(ClientCommandManager.argument("klucz", StringArgumentType.word())
                                        .executes(ctx -> {
                                            LicenseManager.setKey(StringArgumentType.getString(ctx, "klucz"));
                                            ctx.getSource().sendFeedback(Text.literal("Sprawdzam klucz..."));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(Text.literal(LicenseManager.statusText()));
                                    return 1;
                                }))));
    }
}
