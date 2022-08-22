package ru.galster.crashfabric.mixin;

import net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class FixCocoaDecoratorGeneration {
    @Mixin(CocoaDecorator.class)
    public static class CocoaDecoratorMixin {
        @Inject(method = "place", at = @At("HEAD"), cancellable = true)
        public void place(TreeDecorator.Context context, CallbackInfo ci) {
            if(context.logs().isEmpty()) {
                ci.cancel();
            }
        }
    }
}
