package ru.galster.crashfabric.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

public class DontCrashOnPlayerRemove {
    @Mixin(DistanceManager.class)
    public static abstract class DistanceManagerMixin {
        @Shadow @Final
        Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk;

        @Shadow @Final private ChunkTracker naturalSpawnChunkCounter;

        @Shadow @Final private ChunkTracker playerTicketManager;

        @Shadow @Final private TickingTracker tickingTicketsTracker;

        @Shadow protected abstract int getPlayerTicketLevel();

        /**
         * @author Gaslter
         * @reason CrashFabric - double check if object set for chunk exist to prevent crashes
         */
        @Overwrite
        public void removePlayer(SectionPos sectionPos, ServerPlayer serverPlayer) {
            ChunkPos chunkPos = sectionPos.chunk();
            long l = chunkPos.toLong();
            ObjectSet<ServerPlayer> objectSet = this.playersPerChunk.get(l);
            if(objectSet != null) {
                objectSet.remove(serverPlayer);

                if (objectSet.isEmpty()) {
                    this.playersPerChunk.remove(l);
                    this.naturalSpawnChunkCounter.update(l, Integer.MAX_VALUE, false);
                    this.playerTicketManager.update(l, Integer.MAX_VALUE, false);
                    this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, chunkPos, this.getPlayerTicketLevel(), chunkPos);
                }
            }
        }
    }
}
