package com.epic60869.tastyfish;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.Locale;

public final class TastyFishRngHud {
    private static final Identifier ID=Identifier.fromNamespaceAndPath("tastyfish-mod","farming_rng");
    private static TastyFishConfig config;
    private TastyFishRngHud(){}
    public static void register(TastyFishConfig cfg){config=cfg;HudElementRegistry.addLast(ID,TastyFishRngHud::extract);}
    private static void extract(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker d){if(config==null||!config.farmingRngEnabled||Minecraft.getInstance().player==null)return;FarmingRngTracker.Drop drop=FarmingRngTracker.get().active();if(drop!=null)render(g,drop,config.farmingRngX,config.farmingRngY);}
    public static int width(){return 230;} public static int height(){return 48;}
    public static void renderPreview(GuiGraphicsExtractor g,int x,int y){render(g,new FarmingRngTracker.Drop(1,"Overgrown Grass","RARE DROP",125000,Long.MAX_VALUE),x,y);}
    private static void render(GuiGraphicsExtractor g,FarmingRngTracker.Drop d,int x,int y){if(config!=null&&config.farmingRngBackground)g.fill(x-5,y-5,x+width(),y+height(),0xB5000000);g.text(Minecraft.getInstance().font,d.rarity(),x,y,0xFFFFFF55,true);g.text(Minecraft.getInstance().font,(d.amount()>1?d.amount()+"x ":"")+d.name(),x,y+15,0xFFFFFFFF,true);String p=d.unitPrice()<0?"—":format(d.unitPrice()*d.amount());g.text(Minecraft.getInstance().font,p.equals("—")?p:p+" coins",x,y+30,0xFFAAAAAA,true);}
    private static String format(long v){if(v>=1000000000L)return String.format(Locale.ROOT,"%.2fB",v/1e9);if(v>=1000000L)return String.format(Locale.ROOT,"%.2fM",v/1e6);if(v>=1000L)return String.format(Locale.ROOT,"%.1fk",v/1e3);return Long.toString(v);}
}
