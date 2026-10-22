package com.example.examplemod.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimManager extends SavedData {
    private static final String DATA_NAME = "examplemod_claims";

    private final Map<UUID, ClaimRegion> regions = new HashMap<>();

    public static ClaimManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ClaimManager::new, ClaimManager::load),
                DATA_NAME
        );
    }

    public static ClaimManager load(CompoundTag tag, HolderLookup.Provider provider) {
        ClaimManager manager = new ClaimManager();

        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompound(i);
            try {
                UUID id = r.getUUID("id");
                String name = r.getString("name");
                UUID owner = r.getUUID("owner");
                String ownerName = r.getString("ownerName");
                long createdAt = r.getLong("createdAt");

                BlockPos min = BlockPos.of(r.getLong("min"));
                BlockPos max = BlockPos.of(r.getLong("max"));
                String dimension = r.getString("dimension");

                ClaimRegion region = ClaimRegion.fromNbt(
                        id, name, owner, ownerName, createdAt, min, max, dimension);

                ListTag buildersTag = r.getList("builders", Tag.TAG_STRING);
                for (int j = 0; j < buildersTag.size(); j++) {
                    region.getBuilders().add(UUID.fromString(buildersTag.getString(j)));
                }
                ListTag visitorsTag = r.getList("visitors", Tag.TAG_STRING);
                for (int j = 0; j < visitorsTag.size(); j++) {
                    region.getVisitors().add(UUID.fromString(visitorsTag.getString(j)));
                }

                // Разрешённые блоки
                ListTag allowedTag = r.getList("allowedBlocks", Tag.TAG_STRING);
                for (int j = 0; j < allowedTag.size(); j++) {
                    ResourceLocation rl = ResourceLocation.tryParse(allowedTag.getString(j));
                    if (rl != null) {
                        Block block = BuiltInRegistries.BLOCK.get(rl);
                        region.allowBlock(block);
                    }
                }

                // Деспавн агрессивных
                region.setDespawnAgressive(r.getBoolean("despawnAgressive"));

                manager.regions.put(id, region);
            } catch (Exception e) {
                // повреждённый регион — пропускаем
            }
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();

        for (ClaimRegion region : regions.values()) {
            CompoundTag r = new CompoundTag();
            r.putUUID("id", region.getId());
            r.putString("name", region.getName());
            r.putUUID("owner", region.getOwner());
            r.putString("ownerName", region.getOwnerName());
            r.putLong("createdAt", region.getCreatedAt());

            r.putLong("min", region.getMin().asLong());
            r.putLong("max", region.getMax().asLong());
            r.putString("dimension", region.getDimension());

            ListTag buildersTag = new ListTag();
            for (UUID b : region.getBuilders()) {
                buildersTag.add(StringTag.valueOf(b.toString()));
            }
            r.put("builders", buildersTag);

            ListTag visitorsTag = new ListTag();
            for (UUID v : region.getVisitors()) {
                visitorsTag.add(StringTag.valueOf(v.toString()));
            }
            r.put("visitors", visitorsTag);

            // Разрешённые блоки
            ListTag allowedTag = new ListTag();
            for (Block block : region.getAllowedBlocks()) {
                ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(block);
                allowedTag.add(StringTag.valueOf(rl.toString()));
            }
            r.put("allowedBlocks", allowedTag);

            // Деспавн агрессивных
            r.putBoolean("despawnAgressive", region.isDespawnAgressive());

            list.add(r);
        }

        tag.put("regions", list);
        return tag;
    }

    public void addRegion(ClaimRegion region) {
        regions.put(region.getId(), region);
        setDirty();
    }

    public ClaimRegion getRegionAt(ServerLevel level, BlockPos pos) {
        ClaimRegion deepest = null;
        int deepestVolume = Integer.MAX_VALUE;

        for (ClaimRegion region : regions.values()) {
            if (!region.contains(pos, level)) continue;

            int volume = (region.getMax().getX() - region.getMin().getX() + 1)
                    * (region.getMax().getY() - region.getMin().getY() + 1)
                    * (region.getMax().getZ() - region.getMin().getZ() + 1);

            if (volume < deepestVolume) {
                deepestVolume = volume;
                deepest = region;
            }
        }
        return deepest;
    }

    public ClaimRegion getRegionByName(String name) {
        for (ClaimRegion region : regions.values()) {
            if (region.getName().equalsIgnoreCase(name)) return region;
        }
        return null;
    }

    public Collection<ClaimRegion> getAllRegions() {
        return regions.values();
    }

    public void removeRegion(UUID id) {
        regions.remove(id);
        setDirty();
    }
}