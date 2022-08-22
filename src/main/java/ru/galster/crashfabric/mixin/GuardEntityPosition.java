package ru.galster.crashfabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class GuardEntityPosition {
    @Mixin(Entity.class)
    public static abstract class EntityMixin {
        @Shadow @Final private static Logger LOGGER;

        @Shadow private Vec3 position;

        @Shadow public abstract void setBoundingBox(AABB aABB);

        @Shadow protected abstract AABB makeBoundingBox();

        private static boolean checkPosition(Entity entity, double newX, double newY, double newZ) {
            if(Double.isFinite(newX) && Double.isFinite(newY) && Double.isFinite(newZ)) {
                return true;
            }

            String entityInfo;
            try {
                entityInfo = entity.toString();
            } catch (Exception ex) {
                entityInfo = "[Entity info unavailable]";
            }

            LOGGER.error("New entity position is invalid! Tried to set invalid position (" + newX + "," + newY + "," + newZ + ") for entity " + entity.getClass().getName() + " located at " + entity.position() + ", entity info: " + entityInfo, new Throwable());
            return false;
        }

        @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
        public void setPosRaw(double x, double y, double z, CallbackInfo ci) {
            if(!checkPosition((Entity)(Object) this, x, y, z)) {
                ci.cancel();
            }
        }

        @Inject(method = "setPosRaw", at = @At("TAIL"))
        public void setPosRaw_updateBoundingBox(double x, double y, double z, CallbackInfo ci) {
            //noinspection ConstantConditions
            if(!(((Entity)(Object)this) instanceof HangingEntity) && (this.position.x != x || this.position.y != y || this.position.z != z)) {
                this.setBoundingBox(this.makeBoundingBox());
            }
        }
    }
}
