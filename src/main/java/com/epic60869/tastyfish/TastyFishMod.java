package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TastyFishMod implements ClientModInitializer {
    private TastyFishConfig config;
    private final FarmingUploader uploader=new FarmingUploader();
    private String sessionId=FarmingUploader.newSessionId();
    private long lastUploadMillis=0L,lastActiveMillis=-1L;
    private boolean wasConnected=false;
    @Override public void onInitializeClient(){Minecraft mc=Minecraft.getInstance();Path p=mc.gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json");config=TastyFishConfig.load(p);FarmingRngTracker.get().register();ClientTickEvents.END_CLIENT_TICK.register(this::tick);ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->{dispatcher.register(ClientCommands.literal("tf").executes(c->openMenu()));dispatcher.register(ClientCommands.literal("tastyfish").executes(c->openMenu()));});System.out.println("[TastyFish] SkySoft integration and farming RNG overlay loaded.");}
    private int openMenu(){Minecraft.getInstance().execute(()->Minecraft.getInstance().setScreen(new TastyFishScreen(config)));return 1;}
    private void tick(Minecraft mc){if(mc.player==null){wasConnected=false;return;}if(!wasConnected){lastUploadMillis=0L;wasConnected=true;TastyFishVersionChecker.check(mc);}long now=System.currentTimeMillis();if(now-lastUploadMillis<config.uploadIntervalSeconds*1000L)return;lastUploadMillis=now;SkysoftSessionReader.Snapshot s=SkysoftSessionReader.read();if(!s.valid())return;if(lastActiveMillis>=0&&s.activeMillis()<lastActiveMillis)sessionId=FarmingUploader.newSessionId();lastActiveMillis=s.activeMillis();UUID uuid=mc.getUser().getProfileId();uploader.upload(config,mc.getUser().getName(),uuid==null?"":uuid.toString(),currentSkysoftProfile(),sessionId,s);}
    private String currentSkysoftProfile(){try{Class<?> api=Class.forName("com.skysoft.data.hypixel.SkyBlockProfileApi");Field f=api.getDeclaredField("currentProfileKey");f.setAccessible(true);Object v=f.get(null);return v==null?"":v.toString();}catch(Throwable ignored){return "";}}
}
