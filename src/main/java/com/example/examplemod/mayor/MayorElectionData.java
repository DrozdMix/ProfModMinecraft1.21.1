package com.example.examplemod.mayor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MayorElectionData extends SavedData {
    private static final String DATA_NAME = "examplemod_mayor";
    private static final long WEEK_MS = 7L * 24L * 60L * 60L * 1000L;
    /** Через сколько выборы можно закончить без администратора. */
    public static final long DAY_MS = 24L * 60L * 60L * 1000L;
    /** Окно для сбора двух голосов за recall. */
    public static final long RECALL_WINDOW_MS = 60L * 1000L;
    /** Минимум голосов для подтверждения recall. */
    public static final int RECALL_MIN_VOTES = 2;

    private UUID currentMayor = null;
    private UUID pendingMayor = null;
    private long lastElectionTime = 0L;
    private boolean electionActive = false;
    private long electionStartedAt = 0L;
    private boolean recall = false;
    private long recallStartedAt = 0L;

    private final Map<UUID, UUID> votes = new HashMap<>();

    public static MayorElectionData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MayorElectionData::new, MayorElectionData::load),
                DATA_NAME
        );
    }

    public static MayorElectionData load(CompoundTag tag, HolderLookup.Provider provider) {
        MayorElectionData data = new MayorElectionData();
        if (tag.hasUUID("currentMayor")) data.currentMayor = tag.getUUID("currentMayor");
        if (tag.hasUUID("pendingMayor")) data.pendingMayor = tag.getUUID("pendingMayor");
        data.lastElectionTime = tag.getLong("lastElectionTime");
        data.electionActive = tag.getBoolean("electionActive");
        data.electionStartedAt = tag.getLong("electionStartedAt");
        data.recall = tag.getBoolean("recall");
        data.recallStartedAt = tag.getLong("recallStartedAt");

        ListTag votesTag = tag.getList("votes", Tag.TAG_COMPOUND);
        for (int i = 0; i < votesTag.size(); i++) {
            CompoundTag v = votesTag.getCompound(i);
            if (v.hasUUID("voter") && v.hasUUID("candidate")) {
                data.votes.put(v.getUUID("voter"), v.getUUID("candidate"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (currentMayor != null) tag.putUUID("currentMayor", currentMayor);
        if (pendingMayor != null) tag.putUUID("pendingMayor", pendingMayor);
        tag.putLong("lastElectionTime", lastElectionTime);
        tag.putBoolean("electionActive", electionActive);
        tag.putLong("electionStartedAt", electionStartedAt);
        tag.putBoolean("recall", recall);
        tag.putLong("recallStartedAt", recallStartedAt);

        ListTag votesTag = new ListTag();
        for (Map.Entry<UUID, UUID> e : votes.entrySet()) {
            CompoundTag v = new CompoundTag();
            v.putUUID("voter", e.getKey());
            v.putUUID("candidate", e.getValue());
            votesTag.add(v);
        }
        tag.put("votes", votesTag);
        return tag;
    }

    public boolean isElectionDue() {
        return System.currentTimeMillis() - lastElectionTime > WEEK_MS;
    }

    public boolean isElectionActive() { return electionActive; }
    public boolean isRecall() { return recall; }
    public long getElectionStartedAt() { return electionStartedAt; }

    public boolean isDayPassedSinceStart() {
        return electionActive && System.currentTimeMillis() - electionStartedAt >= DAY_MS;
    }

    public long getTimeUntilElection() {
        long remaining = WEEK_MS - (System.currentTimeMillis() - lastElectionTime);
        return Math.max(0, remaining);
    }

    public UUID getCurrentMayor() { return currentMayor; }
    public UUID getPendingMayor() { return pendingMayor; }

    public void setPendingMayor(UUID mayor) {
        this.pendingMayor = mayor;
        setDirty();
    }

    public void clearPendingMayor() {
        this.pendingMayor = null;
        setDirty();
    }

    public void setMayor(UUID mayor) {
        this.currentMayor = mayor;
        this.pendingMayor = null;
        this.lastElectionTime = System.currentTimeMillis();
        this.electionActive = false;
        this.recall = false;
        this.recallStartedAt = 0L;
        votes.clear();
        setDirty();
    }

    public void clearMayor() {
        this.currentMayor = null;
        setDirty();
    }

    public void startElection(boolean recall) {
        this.electionActive = true;
        this.electionStartedAt = System.currentTimeMillis();
        this.recall = recall;
        this.recallStartedAt = recall ? System.currentTimeMillis() : 0L;
        votes.clear();
        setDirty();
    }

    /** Отменить текущее голосование (например, recall не набрал 2 голоса). */
    public void cancelElection() {
        this.electionActive = false;
        this.recall = false;
        this.recallStartedAt = 0L;
        votes.clear();
        setDirty();
    }

    public void vote(UUID voter, UUID candidate) {
        votes.put(voter, candidate);
        setDirty();
    }

    public int getVoteCount() {
        return votes.size();
    }

    public Map<UUID, UUID> getVotes() {
        return votes;
    }

    public long getRecallStartedAt() { return recallStartedAt; }

    public void touchRecallWindow() {
        this.recallStartedAt = System.currentTimeMillis();
        setDirty();
    }

    public boolean isRecallWindowExpired() {
        return recall && System.currentTimeMillis() - recallStartedAt > RECALL_WINDOW_MS;
    }

    public UUID getWinner() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID candidate : votes.values()) {
            counts.merge(candidate, 1, Integer::sum);
        }
        UUID winner = null;
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }
        return winner;
    }

    public int getVotesFor(UUID candidate) {
        int count = 0;
        for (UUID c : votes.values()) {
            if (c.equals(candidate)) count++;
        }
        return count;
    }
}