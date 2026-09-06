package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import java.nio.file.Path;
import java.util.UUID;

public final class TastyFishMod implements ClientModInitializer {
    private TastyFishConfig config;
    private final FarmingUploader uploader=new FarmingUploader();
    private String sessionId=FarmingUploader.newSessionId();
    private long lastUploadMillis;
    private boolean connected;
    @Override public void onInitializeClient(){
        Path path=Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json");
        config=TastyFishConfig.load(path);
        TastyFishStandalone.get().register();
        TastyFishHud.register(config);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientCommandRegistrationCallback.EVENT.register((d,r)->{d.register(ClientCommands.literal("tf").executes(c->{TastyFishHud.open(config);return 1;}));d.register(ClientCommands.literal("tastyfish").executes(c->{TastyFishHud.open(config);return 1;}));});
        System.out.println("[TastyFish] Standalone farming tracker loaded. SkySoft is not required.");
    }
    private void tick(Minecraft mc){
        TastyFishStandalone.get().tick(mc);
        if(mc.player==null){connected=false;return;}
        if(!connected){connected=true;lastUploadMillis=0;sessionId=FarmingUploader.newSessionId();TastyFishVersionChecker.check(mc);}
        long now=System.currentTimeMillis(); if(now-lastUploadMillis<config.uploadIntervalSeconds*1000L)return; lastUploadMillis=now;
        TastyFishStandalone.Snapshot s=TastyFishStandalone.get().snapshot(); if(!s.valid())return;
        UUID uuid=mc.getUser().getProfileId(); uploader.upload(config,mc.getUser().getName(),uuid==null?"":uuid.toString(),"",sessionId,s);
    }
}
