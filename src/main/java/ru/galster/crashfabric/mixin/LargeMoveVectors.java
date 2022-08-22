package ru.galster.crashfabric.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static ru.galster.crashfabric.ServerModInitializer.LOGGER;
import static ru.galster.crashfabric.mixin.accessors.ServerGamePacketListenerImplAccessor.*;

public class LargeMoveVectors {
    @SuppressWarnings("DuplicatedCode")
    @Mixin(ServerGamePacketListenerImpl.class)
    public static abstract class ServerGamePacketListenerImplMixin {
        @Shadow public abstract void disconnect(Component component);

        @Shadow public ServerPlayer player;

        @Shadow @Nullable private Entity lastVehicle;

        @Shadow private double vehicleFirstGoodX;

        @Shadow private double vehicleFirstGoodY;

        @Shadow private double vehicleFirstGoodZ;

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        @Shadow protected abstract boolean isSingleplayerOwner();

        @Shadow private double vehicleLastGoodX;

        @Shadow private double vehicleLastGoodY;

        @Shadow private double vehicleLastGoodZ;

        @Shadow @Final public Connection connection;

        @Shadow private boolean clientVehicleIsFloating;

        @Shadow @Final private MinecraftServer server;

        @Shadow protected abstract boolean noBlocksAround(Entity entity);

        @Shadow private int tickCount;

        @Shadow public abstract void resetPosition();

        @Shadow @Nullable private Vec3 awaitingPositionFromClient;

        @Shadow private int awaitingTeleportTime;

        @Shadow public abstract void teleport(double d, double e, double f, float g, float h);

        @Shadow private double firstGoodX;

        @Shadow private double firstGoodY;

        @Shadow private double firstGoodZ;

        @Shadow private int receivedMovePacketCount;

        @Shadow private int knownMovePacketCount;

        @Shadow private double lastGoodX;

        @Shadow private double lastGoodY;

        @Shadow private double lastGoodZ;

        @Shadow private boolean clientIsFloating;

        @Shadow protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader levelReader, AABB aABB);

        /**
         * @author Galster
         * @reason CrashFabric - Proper move check to prevent crashes
         */
        @Overwrite
        public void handleMoveVehicle(ServerboundMoveVehiclePacket serverboundMoveVehiclePacket) {
            PacketUtils.ensureRunningOnSameThread(serverboundMoveVehiclePacket, (ServerGamePacketListener) this, this.player.getLevel());
            if (containsInvalidValues(serverboundMoveVehiclePacket.getX(), serverboundMoveVehiclePacket.getY(), serverboundMoveVehiclePacket.getZ(), serverboundMoveVehiclePacket.getYRot(), serverboundMoveVehiclePacket.getXRot())) {
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
            } else {
                Entity entity = this.player.getRootVehicle();
                if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                    ServerLevel serverLevel = this.player.getLevel();
                    double fromX = entity.getX();
                    double fromY = entity.getY();
                    double fromZ = entity.getZ();
                    double toX = clampHorizontal(serverboundMoveVehiclePacket.getX());
                    double toY = clampVertical(serverboundMoveVehiclePacket.getY());
                    double toZ = clampHorizontal(serverboundMoveVehiclePacket.getZ());
                    float j = Mth.wrapDegrees(serverboundMoveVehiclePacket.getYRot());
                    float k = Mth.wrapDegrees(serverboundMoveVehiclePacket.getXRot());
                    double l = toX - this.vehicleFirstGoodX;
                    double m = toY - this.vehicleFirstGoodY;
                    double n = toZ - this.vehicleFirstGoodZ;
                    double o = entity.getDeltaMovement().lengthSqr();

                    double currDeltaX = toX - fromX;
                    double currDeltaY = toY - fromY;
                    double currDeltaZ = toZ - fromZ;
                    double p = Math.max(l * l + m * m + n * n, (currDeltaX * currDeltaX + currDeltaY * currDeltaY + currDeltaZ * currDeltaZ) - 1);

                    double otherDeltaX = toX - this.vehicleLastGoodX;
                    double otherDeltaY = toY - this.vehicleLastGoodY - 1.0E-6D;
                    double otherDeltaZ = toZ - this.vehicleLastGoodZ;
                    p = Math.max(p, (otherDeltaX * otherDeltaX + otherDeltaY * otherDeltaY + otherDeltaZ * otherDeltaZ) - 1);

                    if (p - o > 100.0 && !this.isSingleplayerOwner()) {
                        LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), l, m, n);
                        this.connection.send(new ClientboundMoveVehiclePacket(entity));
                        return;
                    }

                    boolean bl = serverLevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                    l = toX - this.vehicleLastGoodX;
                    m = toY - this.vehicleLastGoodY - 1.0E-6;
                    n = toZ - this.vehicleLastGoodZ;
                    boolean bl2 = entity.verticalCollisionBelow;
                    entity.move(MoverType.PLAYER, new Vec3(l, m, n));
                    double q = m;
                    l = toX - entity.getX();
                    m = toY - entity.getY();
                    n = toZ - entity.getZ();

                    if (m > -0.5 || m < 0.5) {
                        m = 0.0;
                    }

                    p = l * l + m * m + n * n;
                    boolean bl3 = false;
                    if (p > 0.0625) {
                        bl3 = true;
                        LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getName().getString(), this.player.getName().getString(), Math.sqrt(p));
                    }

                    entity.absMoveTo(toX, toY, toZ, j, k);
                    boolean bl4 = serverLevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                    if (bl && (bl3 || !bl4)) {
                        entity.absMoveTo(fromX, fromY, fromZ, j, k);
                        this.connection.send(new ClientboundMoveVehiclePacket(entity));
                        return;
                    }

                    this.player.getLevel().getChunkSource().move(this.player);
                    this.player.checkMovementStatistics(this.player.getX() - fromX, this.player.getY() - fromY, this.player.getZ() - fromZ);
                    this.clientVehicleIsFloating = q >= -0.03125 && !bl2 && !this.server.isFlightAllowed() && !entity.isNoGravity() && this.noBlocksAround(entity);
                    this.vehicleLastGoodX = entity.getX();
                    this.vehicleLastGoodY = entity.getY();
                    this.vehicleLastGoodZ = entity.getZ();
                }
            }
        }

        /**
         * @author Galster
         * @reason CrashFabric - Proper move check to prevent crashes
         */
        @Overwrite
        public void handleMovePlayer(ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            PacketUtils.ensureRunningOnSameThread(serverboundMovePlayerPacket, (ServerGamePacketListener) this, this.player.getLevel());
            if (containsInvalidValues(serverboundMovePlayerPacket.getX(0.0), serverboundMovePlayerPacket.getY(0.0), serverboundMovePlayerPacket.getZ(0.0), serverboundMovePlayerPacket.getYRot(0.0F), serverboundMovePlayerPacket.getXRot(0.0F))) {
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
            } else {
                ServerLevel serverLevel = this.player.getLevel();
                if (!this.player.wonGame) {
                    if (this.tickCount == 0) {
                        this.resetPosition();
                    }

                    if (this.awaitingPositionFromClient != null) {
                        if (this.tickCount - this.awaitingTeleportTime > 20) {
                            this.awaitingTeleportTime = this.tickCount;
                            this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
                        }

                    } else {
                        this.awaitingTeleportTime = this.tickCount;
                        double toX = clampHorizontal(serverboundMovePlayerPacket.getX(this.player.getX()));
                        double toY = clampVertical(serverboundMovePlayerPacket.getY(this.player.getY()));
                        double toZ = clampHorizontal(serverboundMovePlayerPacket.getZ(this.player.getZ()));
                        float g = Mth.wrapDegrees(serverboundMovePlayerPacket.getYRot(this.player.getYRot()));
                        float h = Mth.wrapDegrees(serverboundMovePlayerPacket.getXRot(this.player.getXRot()));
                        if (this.player.isPassenger()) {
                            this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
                            this.player.getLevel().getChunkSource().move(this.player);
                        } else {
                            double prevX = this.player.getX();
                            double prevY = this.player.getY();
                            double prevZ = this.player.getZ();
                            double l = this.player.getY();
                            double m = toX - this.firstGoodX;
                            double n = toY - this.firstGoodY;
                            double o = toZ - this.firstGoodZ;
                            double p = this.player.getDeltaMovement().lengthSqr();

                            double currDeltaX = toX - prevX;
                            double currDeltaY = toY - prevY;
                            double currDeltaZ = toZ - prevZ;
                            double q = Math.max(m * m + n * n + o * o, (currDeltaX * currDeltaX + currDeltaY * currDeltaY + currDeltaZ * currDeltaZ) - 1);

                            double otherDeltaX = toX - this.lastGoodX;
                            double otherDeltaY = toY - this.lastGoodY;
                            double otherDeltaZ = toZ - this.lastGoodZ;
                            q = Math.max(q, (otherDeltaX * otherDeltaX + otherDeltaY * otherDeltaY + otherDeltaZ * otherDeltaZ) - 1);

                            if (this.player.isSleeping()) {
                                if (q > 1.0) {
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
                                }

                            } else {
                                ++this.receivedMovePacketCount;
                                int r = this.receivedMovePacketCount - this.knownMovePacketCount;
                                if (r > 5) {
                                    LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), r);
                                    r = 1;
                                }

                                if (!this.player.isChangingDimension() && (!this.player.getLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isFallFlying())) {
                                    float s = this.player.isFallFlying() ? 300.0F : 100.0F;
                                    if (q - p > (double)(s * (float)r) && !this.isSingleplayerOwner()) {
                                        LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), m, n, o);
                                        this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                                        return;
                                    }
                                }

                                AABB aABB = this.player.getBoundingBox();
                                m = toX - this.lastGoodX;
                                n = toY - this.lastGoodY;
                                o = toZ - this.lastGoodZ;
                                boolean bl = n > 0.0;
                                if (this.player.isOnGround() && !serverboundMovePlayerPacket.isOnGround() && bl) {
                                    this.player.jumpFromGround();
                                }

                                boolean bl2 = this.player.verticalCollisionBelow;
                                this.player.move(MoverType.PLAYER, new Vec3(m, n, o));
                                double t = n;
                                m = toX - this.player.getX();
                                n = toY - this.player.getY();
                                if (n > -0.5 || n < 0.5) {
                                    n = 0.0;
                                }

                                o = toZ - this.player.getZ();
                                q = m * m + n * n + o * o;
                                boolean bl3 = false;
                                if (!this.player.isChangingDimension() && q > 0.0625 && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                    bl3 = true;
                                    LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                                }

                                this.player.absMoveTo(toX, toY, toZ, g, h);
                                if (this.player.noPhysics || this.player.isSleeping() || (!bl3 || !serverLevel.noCollision(this.player, aABB)) && !this.isPlayerCollidingWithAnythingNew(serverLevel, aABB)) {
                                    this.clientIsFloating = t >= -0.03125 && !bl2 && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR && !this.server.isFlightAllowed() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !this.player.isFallFlying() && !this.player.isAutoSpinAttack() && this.noBlocksAround(this.player);
                                    this.player.getLevel().getChunkSource().move(this.player);
                                    this.player.doCheckFallDamage(this.player.getY() - l, serverboundMovePlayerPacket.isOnGround());
                                    this.player.setOnGround(serverboundMovePlayerPacket.isOnGround());
                                    if (bl) {
                                        this.player.resetFallDistance();
                                    }

                                    this.player.checkMovementStatistics(this.player.getX() - prevX, this.player.getY() - prevY, this.player.getZ() - prevZ);
                                    this.lastGoodX = this.player.getX();
                                    this.lastGoodY = this.player.getY();
                                    this.lastGoodZ = this.player.getZ();
                                } else {
                                    this.teleport(prevX, prevY, prevZ, g, h);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
