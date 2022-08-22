package ru.galster.crashfabric.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.galster.crashfabric.fakes.EntityIsValidFaker;

public class DoublePlayerChunkMap {
    @Mixin(ChunkMap.class)
    public static class ChunkMapMixin {
        @Shadow @Final
        ServerLevel level;

        @Shadow @Final private Int2ObjectMap<Object> entityMap;

        @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
        public void addEntity(Entity entity, CallbackInfo ci) {
            if(!((EntityIsValidFaker) entity).isValid() || !entity.level.equals(this.level) || this.entityMap.containsKey(entity.getId())) {
                new Throwable("[ERROR] Illegal PlayerChunkMap::addEntity for world " + this.level.dimension().location().toString()
                        + ": " + entity  + (this.entityMap.containsKey(entity.getId()) ? " ALREADY CONTAINED (This would have crashed your server)" : ""))
                        .printStackTrace();

                ci.cancel();
            }
        }
    }

    @Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
    public static class EntityCallbacksMixin {

        @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
        public void onTrackingStart(Entity entity, CallbackInfo ci) {
            ((EntityIsValidFaker) entity).setValid(true);
        }

        @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
        public void onTrackingEnd(Entity entity, CallbackInfo ci) {
            ((EntityIsValidFaker) entity).setValid(false);
        }
    }

    @Mixin(Entity.class)
    public static class EntityMixin implements EntityIsValidFaker {
        @Unique
        public boolean valid = false;

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
