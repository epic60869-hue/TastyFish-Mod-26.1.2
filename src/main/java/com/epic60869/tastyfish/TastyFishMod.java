package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.UUID;

public final class TastyFishMod implements ClientModInitializer {
    private TastyFishConfig config;
    private final FarmingUploader uploader = new FarmingUploader();
    private String sessionId = FarmingUploader.newSessionId();
    private long lastUploadMillis = 0L;
    private long lastActiveMillis = -1L;
    private boolean wasConnected = false;

    @Override
    public void onInitializeClient() {
        Path configPath = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("tastyfish-mod.json");
        config = TastyFishConfig.load(configPath);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        System.out.println("[TastyFish] Farming leaderboard uploader loaded.");
    }

    private void tick(Minecraft minecraft) {
        boolean connected = minecraft.player != null;
        if (!connected) {
            wasConnected = false;
            return;
        }

        if (!wasConnected) {
            lastUploadMillis = 0L;
            wasConnected = true;
            TastyFishVersionChecker.check(minecraft);
        }

        long now = System.currentTimeMillis();
        if (now - lastUploadMillis < config.uploadIntervalSeconds * 1000L) return;
        lastUploadMillis = now;

        SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.read();

        if (!snapshot.valid()) {
            System.err.println("[TastyFish] Skysoft farming snapshot is not ready; upload skipped.");
            return;
        }

        if (lastActiveMillis >= 0L && snapshot.activeMillis() < lastActiveMillis) {
            sessionId = FarmingUploader.newSessionId();
            System.out.println("[TastyFish] Skysoft active time reset; started a new leaderboard session.");
        }
        lastActiveMillis = snapshot.activeMillis();

        String username = minecraft.getUser().getName();
        UUID uuid = minecraft.getUser().getProfileId();
        String profile = currentSkysoftProfile();

        uploader.upload(
            config,
            username,
            uuid == null ? "" : uuid.toString(),
            profile,
            sessionId,
            snapshot
        );
    }

    private String currentSkysoftProfile() {
        try {
            Class<?> api = Class.forName("com.skysoft.data.hypixel.SkyBlockProfileApi");
            Field field = api.getDeclaredField("currentProfileKey");
            field.setAccessible(true);
            Object value = field.get(null);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }
}
