package org.chubby.github;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server packet that carries a rider's fire-mode input.
 *
 * Registered on DragonMod.NETWORK_WRAPPER at index 0.
 *
 * Fire modes
 *   0  player released keys → stop fire
 *   1  FIRE_BREATH held     → sustain continuous fire-breath stream
 *   2  FIRE_BALL tapped     → spawn one EntityDragonFireCharge projectile
 */
public class PacketDragonFireInput {

    /** Server-side entity ID of the dragon being ridden. */
    public final int dragonId;

    /**
     * 0 = stop fire
     * 1 = breath stream (sent every tick while key held)
     * 2 = fireball      (sent once per key-press)
     */
    public final byte fireMode;

    public PacketDragonFireInput(int dragonId, byte fireMode) {
        this.dragonId = dragonId;
        this.fireMode = fireMode;
    }

    // ── Serialisation ────────────────────────────────────────────────────────

    public static void encode(PacketDragonFireInput pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.dragonId);
        buf.writeByte(pkt.fireMode);
    }

    public static PacketDragonFireInput decode(FriendlyByteBuf buf) {
        return new PacketDragonFireInput(buf.readInt(), buf.readByte());
    }

    // ── Server-side handler ──────────────────────────────────────────────────

    public static void handle(PacketDragonFireInput pkt,
                              Supplier<NetworkEvent.Context> ctxSupplier) {

        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {

            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // Resolve the dragon in the server world
            Entity entity = sender.serverLevel().getEntity(pkt.dragonId);
            if (!(entity instanceof EntityCustomDragon dragon)) return;

            // Security check: only the current controlling rider may fire
            if (dragon.getControllingPassenger() == null) return;
            if (!dragon.getControllingPassenger().getUUID()
                    .equals(sender.getUUID()))          return;

            dragon.handleFireInput(pkt.fireMode);
        });

        ctx.setPacketHandled(true);
    }
}