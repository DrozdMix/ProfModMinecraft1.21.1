package com.example.examplemod.profession;

import net.minecraft.network.chat.Component;

public enum Profession {
    NONE("none", '\u0000'),
    MINER("miner", '\uE000'),
    BUILDER("builder", '\uE002'),
    FARMER_COOK("farmer_cook", '\uE003'),
    BLACKSMITH("blacksmith", '\uE004'),
    ENCHANTER_ALCHEMIST("enchanter_alchemist", '\uE005'),
    LUMBERJACK("lumberjack", '\uE001'),
    FISHERMAN("fisherman", '\uE006'),
    WARRIOR("warrior", '\uE008'),
    MAYOR("mayor", '\uE007');

    private final String id;
    private final char badge;

    Profession(String id, char badge) {
        this.id = id;
        this.badge = badge;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("profession.examplemod." + id);
    }

    /** Символ-значок профессии для табличного списка игроков (Tab). */
    public char getBadge() {
        return badge;
    }

    /** Можно ли эту роль поставить в дополнительный слот. */
    public boolean isOptional() {
        return this == FISHERMAN || this == LUMBERJACK || this == MAYOR;
    }

    public static Profession byId(String id) {
        for (Profession p : values()) {
            if (p.id.equals(id)) return p;
        }
        return NONE;
    }
}