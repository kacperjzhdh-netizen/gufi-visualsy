package pl.fazevisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class HudRenderer {
    private static float shownHealth = -1;
    private static float shownFood = -1;

    private HudRenderer() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        if (!LicenseManager.isActive()) {
            ctx.drawText(mc.textRenderer, "FazeVisuals: " + LicenseManager.statusText(), 6, 6, 0xFFFF5555, true);
            return;
        }
        if (!FazeVisualsClient.hudEnabled) return;

        // Hit flash (red screen overlay fading over 250 ms)
        long since = System.currentTimeMillis() - FazeVisualsClient.hitFlashStart;
        if (since < 250) {
            int alpha = (int) (70 * (1.0 - since / 250.0));
            ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (alpha << 24) | 0xFF2020);
        }

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float food = mc.player.getHungerManager().getFoodLevel();
        if (shownHealth < 0) { shownHealth = health; shownFood = food; }
        shownHealth += (health - shownHealth) * 0.12f;
        shownFood += (food - shownFood) * 0.12f;

        int x = 6, y = 6, w = 100, h = 8;

        ctx.fill(x - 3, y - 3, x + w + 3, y + 62, 0x66000000);
        ctx.drawText(mc.textRenderer, mc.getCurrentFps() + " FPS", x, y, 0xFFFFFFFF, true);
        ctx.drawText(mc.textRenderer, String.format("XYZ %.0f %.0f %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y + 12, 0xFFCCCCCC, true);

        bar(ctx, x, y + 26, w, h, shownHealth / maxHealth, 0xFFE53935);
        bar(ctx, x, y + 38, w, h, shownFood / 20f, 0xFFFFA726);
        ctx.drawText(mc.textRenderer, LicenseManager.timeLeftText(), x, y + 50, 0xFF88FF88, true);
    }

    private static void bar(DrawContext ctx, int x, int y, int w, int h, float ratio, int color) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        ctx.fill(x, y, x + w, y + h, 0x80000000);
        ctx.fill(x, y, x + (int) (w * ratio), y + h, color);
    }
}
