package ru.galster.crashfabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.galster.crashfabric.ServerModInitializer;

import java.util.function.Consumer;

public class PreventEntityCrash {
    @Mixin(Level.class)
    public static class LevelMixin<T extends Entity> {
        @Inject(method = "guardEntityTick", at = @At("HEAD"))
        public void guardEntityTick(Consumer<T> consumer, T entity, CallbackInfo ci) {
            try {
                consumer.accept(entity);
            } catch (Throwable throwable) {
                final String msg = String.format("Entity threw exception at %s:%s,%s,%s", entity.level.dimension().location().toString(), entity.getX(), entity.getY(), entity.getZ());
                ServerModInitializer.LOGGER.error(msg, throwable);
                entity.discard();
            }
        }
    }

    // --------------
    // BlockEntity fix is not needed in the newest versions (1.19 at the moment of writing)
    // --------------

    @Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
    public static abstract class LevelChunkMixin {
        @Shadow public abstract BlockPos getPos();

        @Final
        @Shadow
        LevelChunk field_27223;

        @Inject(method = "tick", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"), cancellable = true)
        public void tick(CallbackInfo ci) {
//            final String msg = String.format("BlockEntity threw exception at %s:%s,%s,%s", thisChunk.getLevel().dimension().location().toString(), this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
//            ServerModInitializer.LOGGER.error(msg, throwable);
            this.field_27223.removeBlockEntity(this.getPos());
            ci.cancel();
        }
    }
}
