package com.moulberry.flashback.cache;

import net.minecraft.client.player.LocalPlayer;

public class PlayerCache {
    private static LocalPlayer cachedPlayer;
    private static String nearestPlayerName;

    public static void setPlayer(LocalPlayer player) {
        cachedPlayer = player;
    }

    public static LocalPlayer getPlayer() {
        return cachedPlayer;
    }

    public static void setNearestPlayerName(String name) {
        nearestPlayerName = name;
    }

    public static String getNearestPlayerName() {
        return nearestPlayerName;
    }

    public static void reset() {
        nearestPlayerName = null;
    }
}
