package com.example.examplemod.command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimDeleteConfirm {
    private static final Map<UUID, String> PENDING = new HashMap<>();

    public static void setPending(UUID uuid, String regionName) {
        PENDING.put(uuid, regionName);
    }

    public static String getPending(UUID uuid) {
        return PENDING.get(uuid);
    }

    public static boolean isPending(UUID uuid, String regionName) {
        return regionName.equals(PENDING.get(uuid));
    }

    public static void clear(UUID uuid) {
        PENDING.remove(uuid);
    }
}