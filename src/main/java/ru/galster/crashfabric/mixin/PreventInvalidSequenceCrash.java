package ru.galster.crashfabric.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

public class PreventInvalidSequenceCrash {
    @Mixin(targets = "net/minecraft/network/ConnectionProtocol$PacketSet")
    public static class ConnectionProtocolMixin<T extends PacketListener> {

        @Shadow @Final private List<Function<FriendlyByteBuf, ? extends Packet<T>>> idToDeserializer;

        @Inject(method = "createPacket", at = @At("HEAD"), cancellable = true)
        public void createPacket(int id, FriendlyByteBuf friendlyByteBuf, CallbackInfoReturnable<Packet<?>> cir) {
            if(id < 0 || id >= this.idToDeserializer.size()) {
                cir.setReturnValue(null);
            }
        }
    }

    @Mixin(Varint21FrameDecoder.class)
    public static class Varint21FrameDecoderMixin {
        @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
        public void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list, CallbackInfo ci) {
            if(!channelHandlerContext.channel().isActive()) {
                byteBuf.skipBytes(byteBuf.readableBytes());
                ci.cancel();
            }
        }
    }

    @Mixin(PacketUtils.class)
    public static class PacketUtilsMixin {
        private static final Component PROCESSING_ERROR = Component.literal("Packet processing error");

        @Shadow @Final private static Logger LOGGER;

        @SuppressWarnings("rawtypes")
        @Inject(method = "method_11072(Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/PacketListener;shouldPropagateHandlingExceptions()Z"), cancellable = true)
        private static void method_11072(PacketListener packetListener, Packet packet, CallbackInfo ci) {
            var connection = packetListener.getConnection();
            LOGGER.error("Error whilst processing packet {} for connection from {}", packet, connection.getRemoteAddress());

            connection.send(new ClientboundDisconnectPacket(PROCESSING_ERROR), PacketSendListener.thenRun(() -> connection.disconnect(PROCESSING_ERROR)));
            connection.setReadOnly();

            ci.cancel();
        }
    }
}
