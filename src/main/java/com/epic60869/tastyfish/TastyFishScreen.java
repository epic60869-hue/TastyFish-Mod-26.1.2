package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Standalone TastyFish settings and HUD editor for Minecraft 26.1.2. */
public final class TastyFishScreen extends Screen {
    private final TastyFishConfig config;
    private final boolean editor;
    private String dragging;

    public TastyFishScreen(TastyFishConfig config) { this(config, false); }
    private TastyFishScreen(TastyFishConfig config, boolean editor) {
        super(Component.literal("TastyFish"));
        this.config = config;
        this.editor = editor;
    }

    @Override
    protected void init() {
        if (editor) {
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> Minecraft.getInstance().setScreen(new TastyFishScreen(config)))
                .bounds(width / 2 - 50, height - 32, 100, 20).build());
            return;
        }
        addRenderableWidget(Button.builder(Component.literal("GUI Editor"), b -> Minecraft.getInstance().setScreen(new TastyFishScreen(config, true)))
            .bounds(width / 2 - 60, height / 2 - 10, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset Farming Data"), b -> TastyFishStandalone.get().reset())
            .bounds(width / 2 - 60, height / 2 + 16, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(width / 2 - 60, height / 2 + 42, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (!editor) {
            g.centeredText(font, "TastyFish", width / 2, height / 2 - 60, 0xFFFFFF55);
            g.centeredText(font, "Standalone farming tracker", width / 2, height / 2 - 45, 0xFFAAAAAA);
            return;
        }
        g.centeredText(font, "GUI Editor", width / 2, 14, 0xFFFFFF55);
        g.centeredText(font, "Hover an element, then drag it. Empty space does nothing.", width / 2, 27, 0xFFAAAAAA);
        TastyFishHud.renderProfitPreview(g, TastyFishHud.profitX(), TastyFishHud.profitY(),
            over(mouseX, mouseY, TastyFishHud.profitX(), TastyFishHud.profitY(), TastyFishHud.profitW(), TastyFishHud.profitH()), true);
        TastyFishHud.renderRngPreview(g, TastyFishHud.rngX(), TastyFishHud.rngY(),
            over(mouseX, mouseY, TastyFishHud.rngX(), TastyFishHud.rngY(), TastyFishHud.rngW(), TastyFishHud.rngH()), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (editor && e.button() == 0) {
            if (over(e.x(), e.y(), TastyFishHud.profitX(), TastyFishHud.profitY(), TastyFishHud.profitW(), TastyFishHud.profitH())) { dragging = "profit"; return true; }
            if (over(e.x(), e.y(), TastyFishHud.rngX(), TastyFishHud.rngY(), TastyFishHud.rngW(), TastyFishHud.rngH())) { dragging = "rng"; return true; }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (editor && dragging != null && e.button() == 0) {
            if (dragging.equals("profit")) {
                TastyFishHud.setProfitPosition((int)e.x() - TastyFishHud.profitW() / 2, (int)e.y() - 30);
            } else {
                TastyFishHud.setRngPosition((int)e.x() - TastyFishHud.rngW() / 2, (int)e.y() - 26);
            }
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (editor && e.button() == 0 && dragging != null) { dragging = null; return true; }
        return super.mouseReleased(e);
    }

    @Override
    public void onClose() {
        config.save();
        super.onClose();
    }

    private static boolean over(double x, double y, int left, int top, int w, int h) {
        return x >= left && x <= left + w && y >= top && y <= top + h;
    }
}
