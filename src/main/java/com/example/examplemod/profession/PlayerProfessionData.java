package com.example.examplemod.profession;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PlayerProfessionData implements INBTSerializable<CompoundTag> {

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerProfessionData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, data -> data.primary.getId(),
                    ByteBufCodecs.STRING_UTF8, data -> data.optional.getId(),
                    (primaryId, optionalId) -> {
                        PlayerProfessionData d = new PlayerProfessionData();
                        d.primary = Profession.byId(primaryId);
                        d.optional = Profession.byId(optionalId);
                        return d;
                    }
            );

    private Profession primary = Profession.NONE;
    private Profession optional = Profession.NONE;

    public Profession getPrimary() {
        return primary;
    }

    public void setPrimary(Profession primary) {
        this.primary = primary;
    }

    public Profession getOptional() {
        return optional;
    }

    public void setOptional(Profession optional) {
        this.optional = optional;
    }

    /** Есть ли у игрока роль — в основной или дополнительной. */
    public boolean hasProfession(Profession profession) {
        if (profession == Profession.NONE) return false;
        return primary == profession || optional == profession;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("primary", primary.getId());
        tag.putString("optional", optional.getId());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        primary = Profession.byId(tag.getString("primary"));
        optional = Profession.byId(tag.getString("optional"));
    }
}