package org.chubby.github;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client → Server packet for staff-free dragon control.
 *
 * Registered on DragonMod.NETWORK_WRAPPER at index 1.
 *
 * Actions:
 *   0  SIT_TOGGLE   – toggle the mounted dragon's sit/follow command
 *   1  MOUNT        – mount the nearest tamed dragon owned by this player
 *   2  DISMOUNT     – dismount from the current vehicle
 *   3  BITE         – trigger a bite attack on the mounted dragon
 */
public class PacketDragonControl {

    public static final byte SIT_TOGGLE = 0;
    public static final byte MOUNT      = 1;
    public static final byte DISMOUNT   = 2;
    public static final byte BITE       = 3;

    /** Search radius (blocks) for MOUNT action. */
    private static final double MOUNT_SEARCH_RADIUS = 12.0;

    public final byte action;

    public PacketDragonControl(byte action) {
        this.action = action;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    public static void encode(PacketDragonControl pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.action);
    }

    public static PacketDragonControl decode(FriendlyByteBuf buf) {
        return new PacketDragonControl(buf.readByte());
    }

    // ── Server-side handler ───────────────────────────────────────────────────

    public static void handle(PacketDragonControl pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            switch (pkt.action) {

                case SIT_TOGGLE -> {
                    Entity vehicle = player.getVehicle();
                    if (!(vehicle instanceof EntityCustomDragon dragon)) return;
                    if (!dragon.isTame()) return;
                    if (!player.getUUID().equals(dragon.getOwnerUUID())) return;
                    // Toggle between follow (0) and sit (1)
                    int newCmd = dragon.getCommand() == 1 ? 0 : 1;
                    dragon.setCommand(newCmd);
                }

                case MOUNT -> {
                    if (player.getVehicle() != null) return; // already riding
                    ServerLevel level = player.serverLevel();
                    AABB searchBox = player.getBoundingBox().inflate(MOUNT_SEARCH_RADIUS);
                    List<EntityCustomDragon> candidates = level.getEntitiesOfClass(
                            EntityCustomDragon.class, searchBox,
                            d -> d.isTame()
                                    && player.getUUID().equals(d.getOwnerUUID())
                                    && d.getControllingPassenger() == null
                                    && !d.isModelDead()
                                    && !d.isOrderedToSit());
                    if (candidates.isEmpty()) return;
                    // Pick the closest one.
                    candidates.sort(Comparator.comparingDouble(d -> d.distanceToSqr(player)));
                    EntityCustomDragon target = candidates.get(0);
                    player.startRiding(target, true);
                }

                case DISMOUNT -> {
                    if (player.getVehicle() instanceof EntityCustomDragon) {
                        player.stopRiding();
                    }
                }

                case BITE -> {
                    Entity vehicle = player.getVehicle();
                    if (!(vehicle instanceof EntityDragon dragon)) return;
                    if (!dragon.isTame()) return;
                    if (!player.getUUID().equals(dragon.getOwnerUUID())) return;
                    // Flag is consumed in EntityDragon.aiStep() on the next server tick.
                    dragon.riderBiteRequest = true;
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
