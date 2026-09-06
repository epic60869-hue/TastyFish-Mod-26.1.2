package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class TastyFishScreen extends Screen {
    private final TastyFishConfig config;
    private boolean farmingPage;
    private float toggleProgress;
    private long lastFrame;
    public TastyFishScreen(TastyFishConfig config){super(Component.literal("TastyFish Settings"));this.config=config;this.farmingPage=true;this.toggleProgress=config.farmingRngEnabled?1f:0f;}
    @Override protected void init(){clearWidgets();addRenderableWidget(Button.builder(Component.literal("Farming"),b->{farmingPage=true;rebuild();}).bounds(12,48,96,22).build());addRenderableWidget(Button.builder(Component.literal("GUI"),b->Minecraft.getInstance().setScreen(new TastyFishGuiEditor(config))).bounds(12,76,96,22).build());addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width-92,height-32,80,20).build());}
    private void rebuild(){clearWidgets();init();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);long now=System.nanoTime();float step=lastFrame==0?0.016f:Math.min(.05f,(now-lastFrame)/1_000_000_000f);lastFrame=now;float target=config.farmingRngEnabled?1f:0f;toggleProgress+=(target-toggleProgress)*Math.min(1f,step*12f);g.fill(0,0,width,height,0xFF101014);g.fill(0,0,120,height,0xFF17171C);g.text(font,"TastyFish",14,16,0xFFFFFF55,true);g.text(font,"Settings",14,30,0xFF888890,false);g.fill(112,0,113,height,0xFF29292F);if(farmingPage)renderFarming(g);}
    private void renderFarming(GuiGraphicsExtractor g){int left=145;g.text(font,"Farming",left,28,0xFFFFFFFF,true);g.text(font,"Farming RNG Overlay",left,56,0xFFFFFFFF,true);g.text(font,"Show rare farming drops on screen",left,70,0xFF9999A2,false);int tx=width-92,ty=51;drawToggle(g,tx,ty,toggleProgress);g.text(font,"Background",left,102,0xFFFFFFFF,true);g.text(font,"Show a black background behind the overlay",left,116,0xFF9999A2,false);g.fill(tx,96,tx+44,118,0xFF303038);if(config.farmingRngBackground)g.fill(tx+22,96,tx+44,118,0xFF6E6E78);g.fill(tx+(config.farmingRngBackground?24:2),98,tx+(config.farmingRngBackground?42:20),116,0xFFE8E8EA);g.text(font,"Preview",left,154,0xFF777780,false);TastyFishRngHud.renderPreview(g,left,172);}
    private void drawToggle(GuiGraphicsExtractor g,int x,int y,float p){g.fill(x,y,x+44,y+22,0xFF303038);if(p>.5f)g.fill(x+22,y,x+44,y+22,0xFF6E6E78);int k=x+2+Math.round(p*20f);g.fill(k,y+2,k+20,y+20,0xFFE8E8EA);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dbl){if(e.button()==0&&farmingPage){int x=(int)e.x(),y=(int)e.y();if(x>=width-100&&x<=width-45&&y>=45&&y<=82){config.farmingRngEnabled=!config.farmingRngEnabled;save();return true;}if(x>=width-100&&x<=width-45&&y>=91&&y<=126){config.farmingRngBackground=!config.farmingRngBackground;save();return true;}}return super.mouseClicked(e,dbl);}
    private void save(){Minecraft mc=Minecraft.getInstance();config.save(mc.gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json"));}
}
