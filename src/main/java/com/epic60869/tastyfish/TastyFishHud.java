package com.epic60869.tastyfish;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class TastyFishHud {
    private static final Identifier PROFIT=Identifier.fromNamespaceAndPath("tastyfish-mod","profit"),RNG=Identifier.fromNamespaceAndPath("tastyfish-mod","rng");
    private static TastyFishConfig config; private static int px,py,rx,ry; private TastyFishHud(){}
    public static void register(TastyFishConfig cfg){config=cfg;px=cfg.profitHudX;py=cfg.profitHudY;rx=cfg.rngHudX;ry=cfg.rngHudY;HudElementRegistry.addLast(PROFIT,TastyFishHud::profit);HudElementRegistry.addLast(RNG,TastyFishHud::rng);}
    public static int profitX(){return px;} public static int profitY(){return py;} public static int profitW(){return 200;} public static int profitH(){return 65;} public static int rngX(){return rx;} public static int rngY(){return ry;} public static int rngW(){return 240;} public static int rngH(){return 52;}
    public static void setProfitPosition(int x,int y){px=Math.max(0,x);py=Math.max(0,y);save();}
    public static void setRngPosition(int x,int y){rx=Math.max(0,x);ry=Math.max(0,y);save();}
    private static void save(){if(config!=null){config.profitHudX=px;config.profitHudY=py;config.rngHudX=rx;config.rngHudY=ry;config.save();}}
    private static void profit(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker d){Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;TastyFishStandalone t=TastyFishStandalone.get();renderProfitPreview(g,px,py,false,false);if(mouseOver(mc,px,py+47,profitW(),18)){List<TastyFishStandalone.PestRow> rows=t.pestRows();int y=py+64;g.fill(px-4,y,px+180,y+10+rows.size()*12,0xEE111111);bold(g,"Pests Vacuumed",px,y+2,0xFFFFFF55);y+=14;for(var row:rows){String p=String.format(Locale.ROOT,"%.1f",row.percentage()).replace(".0","");bold(g,row.name()+" "+row.count()+" ("+p+"%)",px,y,0xFFDDDDDD);y+=12;}}}
    private static void rng(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker d){Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;TastyFishStandalone.Drop drop=TastyFishStandalone.get().activeDrop();if(drop==null)return;renderRngPreview(g,rx,ry,false,false);}
    public static void renderProfitPreview(GuiGraphicsExtractor g,int x,int y,boolean selected,boolean editor){TastyFishStandalone t=TastyFishStandalone.get();g.fill(x-3,y-3,x+profitW(),y+profitH(),0x88000000);bold(g,"TastyFish Farming",x,y,0xFFFFFF55);bold(g,"Total Profit: "+coins(t.profit()),x,y+13,0xFFFFFFFF);bold(g,"Profit/h: "+coins(t.profitPerHour()),x,y+25,0xFFAAAAAA);bold(g,"Items Tracked: "+t.items().values().stream().mapToLong(Long::longValue).sum(),x,y+37,0xFFAAAAAA);bold(g,"Pests Killed: "+t.pests().values().stream().mapToLong(Long::longValue).sum(),x,y+49,0xFFFFFFFF);if(editor){int o=selected?0xFFFFFF55:0xFF777777;g.fill(x-4,y-4,x+profitW()+1,y-3,o);bold(g,"Drag this HUD element",x,y+62,0xFFDDDDDD);}}
    public static void renderRngPreview(GuiGraphicsExtractor g,int x,int y,boolean selected,boolean editor){TastyFishStandalone.Drop drop=TastyFishStandalone.get().activeDrop();String name=drop==null?"RNG drop preview":drop.amount()+"x "+drop.name();int c=drop==null?0xFFFFFF55:switch(drop.rarity()){case"RNGESUS"->0xFFAA00AA;case"CRAZY RARE"->0xFFFF55FF;case"RARE CROP"->0xFF5555FF;default->0xFFFFFF55;};String v;if(drop==null)v="Price unavailable";else if(drop.name().equalsIgnoreCase("Seasoning"))v="—";else{double unit=drop.unitPrice()>0?drop.unitPrice():ItemPriceResolver.valueByName(drop.name());v=unit>0?coins(unit*drop.amount()):"Price unavailable";}g.fill(x-3,y-3,x+rngW(),y+rngH(),0x88000000);bold(g,"Farming RNG",x,y,0xFFFFFF55);bold(g,name,x,y+13,c);bold(g,v,x,y+26,0xFFFFAA00);bold(g,"Price: NPC / Bazaar / LBIN",x,y+39,0xFFAAAAAA);if(editor){int o=selected?0xFFFFFF55:0xFF777777;g.fill(x-4,y-4,x+rngW()+1,y-3,o);bold(g,"Drag this HUD element",x,y+52,0xFFDDDDDD);}}
    public static void open(TastyFishConfig cfg){Minecraft.getInstance().setScreen(new TastyFishScreen(cfg));}
    private static boolean mouseOver(Minecraft mc,int x,int y,int w,int h){try{double sx=((Number)mc.mouseHandler.getClass().getMethod("xpos").invoke(mc.mouseHandler)).doubleValue(),sy=((Number)mc.mouseHandler.getClass().getMethod("ypos").invoke(mc.mouseHandler)).doubleValue();return sx*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth()>=x&&sx*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth()<=x+w&&sy*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight()>=y&&sy*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight()<=y+h;}catch(Throwable e){return false;}}
    private static void bold(GuiGraphicsExtractor g,String s,int x,int y,int c){g.text(Minecraft.getInstance().font,s,x,y,c,true);}
    private static String coins(double v){if(v>=1e9)return String.format(Locale.ROOT,"%.2fB coins",v/1e9);if(v>=1e6)return String.format(Locale.ROOT,"%.2fM coins",v/1e6);if(v>=1e3)return String.format(Locale.ROOT,"%.1fk coins",v/1e3);return String.format(Locale.ROOT,"%.0f coins",v);}
}
