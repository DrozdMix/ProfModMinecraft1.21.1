package com.example.examplemod.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimRegion {
    private final UUID id;
    private final String name;
    private UUID owner;
    private String ownerName;
    private final long createdAt;
    private final Set<UUID> builders = new HashSet<>();
    private final Set<UUID> visitors = new HashSet<>();

    /** Блоки, которые ЛЮБОЙ игрок может использовать на этой территории. */
    private final Set<Block> allowedBlocks = new HashSet<>();

    /** Удалять ли враждебных мобов на территории. */
    private boolean despawnAgressive = false;

    private final BlockPos min;
    private final BlockPos max;
    private final String dimension;

    // Конструктор для создания нового региона
    public ClaimRegion(String name, BlockPos corner1, BlockPos corner2,
                       UUID owner, String ownerName, Level level) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.ownerName = ownerName;
        this.createdAt = System.currentTimeMillis();

        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);
        this.dimension = level.dimension().location().toString();
    }

    // Конструктор для загрузки из NBT
    private ClaimRegion(UUID id, String name, UUID owner, String ownerName,
                        long createdAt, BlockPos min, BlockPos max, String dimension) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
        this.min = min;
        this.max = max;
        this.dimension = dimension;
    }

    public static ClaimRegion fromNbt(UUID id, String name, UUID owner, String ownerName,
                                      long createdAt, BlockPos min, BlockPos max, String dimension) {
        return new ClaimRegion(id, name, owner, ownerName, createdAt, min, max, dimension);
    }

    public boolean contains(BlockPos pos, Level level) {
        if (!level.dimension().location().toString().equals(dimension)) return false;
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean intersects(BlockPos otherMin, BlockPos otherMax, String otherDimension) {
        if (!dimension.equals(otherDimension)) return false;
        return min.getX() <= otherMax.getX() && max.getX() >= otherMin.getX()
                && min.getY() <= otherMax.getY() && max.getY() >= otherMin.getY()
                && min.getZ() <= otherMax.getZ() && max.getZ() >= otherMin.getZ();
    }

    public boolean isOwner(UUID player) { return owner.equals(player); }
    public boolean canBuild(UUID player) { return isOwner(player) || builders.contains(player); }
    public boolean canVisit(UUID player) { return canBuild(player) || visitors.contains(player); }

    public void setOwner(UUID newOwner, String newOwnerName) {
        this.owner = newOwner;
        this.ownerName = newOwnerName;
    }

    public String getFormattedDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return fmt.format(new Date(createdAt));
    }

    // ===== Разрешённые блоки =====

    public Set<Block> getAllowedBlocks() {
        return allowedBlocks;
    }

    public void allowBlock(Block block) {
        allowedBlocks.add(block);
    }

    public void disallowBlock(Block block) {
        allowedBlocks.remove(block);
    }

    public boolean isBlockAllowed(Block block) {
        return allowedBlocks.contains(block);
    }

    // ===== Деспавн агрессивных мобов =====

    public boolean isDespawnAgressive() {
        return despawnAgressive;
    }

    public void setDespawnAgressive(boolean value) {
        this.despawnAgressive = value;
    }

    // ===== Геттеры =====

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public long getCreatedAt() { return createdAt; }
    public Set<UUID> getBuilders() { return builders; }
    public Set<UUID> getVisitors() { return visitors; }
    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    public String getDimension() { return dimension; }

    /** Утилита: ResourceLocation -> Block (для команд). */
    public static Block blockFromId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (rl == null) return null;
        return BuiltInRegistries.BLOCK.get(rl);
    }
}