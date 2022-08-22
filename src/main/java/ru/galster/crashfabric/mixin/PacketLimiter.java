package ru.galster.crashfabric.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.galster.crashfabric.utils.IntervalledCounter;

public class PacketLimiter {
    @Mixin(Connection.class)
    public static abstract class ConnectionMixin {
        private static final Component KICK_MESSAGE = Component.translatable("disconnect.exceeded_packet_rate").withStyle(Style.EMPTY.withColor(0xff0000));
        private static final double MAX_PACKET_RATE = 500.0;

        @Shadow protected abstract void sendPacket(Packet<?> packet, @Nullable PacketSendListener packetSendListener);

        @Shadow public abstract void disconnect(Component component);

        @Shadow public abstract void setReadOnly();

        @Shadow private Channel channel;
        @Unique
        private boolean stopReadingPackets = false;
        @Unique
        private final IntervalledCounter allPacketsCount = new IntervalledCounter((long)7.0e9);
        private final Object PACKET_LIMIT_LOCK = new Object();

        private void kickForPacketSpam() {
            this.sendPacket(
                    new ClientboundDisconnectPacket(KICK_MESSAGE),
                    PacketSendListener.thenRun(() -> this.disconnect(KICK_MESSAGE))
            );
            this.setReadOnly();
            this.stopReadingPackets = true;
        }

        @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
        public void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
            if(!this.channel.isOpen()) {
                return;
            }

            if(this.stopReadingPackets) {
                ci.cancel();
                return;
            }

            long time = System.nanoTime();
            synchronized (PACKET_LIMIT_LOCK) {
                this.allPacketsCount.updateAndAdd(1, time);
                if(this.allPacketsCount.getRate() > MAX_PACKET_RATE) {
                    this.kickForPacketSpam();
                    ci.cancel();
                }
            }
        }
    }
}
