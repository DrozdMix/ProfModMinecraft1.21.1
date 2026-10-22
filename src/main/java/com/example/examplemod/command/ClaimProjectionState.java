package com.example.examplemod.command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimProjectionState {
    private static final Set<UUID> ENABLED = new HashSet<>();

    public static boolean isEnabled(UUID uuid) { return ENABLED.contains(uuid); }
    public static void enable(UUID uuid) { ENABLED.add(uuid); }
    public static void disable(UUID uuid) { ENABLED.remove(uuid); }
}