package ru.galster.crashfabric.mixin.fakes;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Invoker("containsInvalidValues")
    static boolean containsInvalidValues(double d, double e, double f, float g, float h) {
        throw new AssertionError();
    }

    @Invoker("clampHorizontal")
    static double clampHorizontal(double d) {
        throw new AssertionError();
    }

    @Invoker("clampVertical")
    static double clampVertical(double d) {
        throw new AssertionError();
    }
}
