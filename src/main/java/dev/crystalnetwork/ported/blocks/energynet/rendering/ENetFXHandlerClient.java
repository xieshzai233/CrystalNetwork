package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.client.DEParticles;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBeam;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ENetFXHandlerClient<T extends BlockEntity & IENetEffectTile> extends ENetFXHandler<T> {
    protected CrystalFXBase<?> staticFX;
    private final CrystalFXConnections<CrystalFXBeam<T>> beams = new CrystalFXConnections<>();

    public ENetFXHandlerClient(T tile) {
        super(tile);
    }

    @Override
    public void update() {
        if (tile.hasStaticFX()) {
            if (staticFX == null || !staticFX.isAlive()) {
                staticFX = tile.createStaticFX();
                staticFX.updateFX(0.5F);
                DEParticles.addParticleDirect(tile.getLevel(), staticFX);
            } else {
                staticFX.updateFX(0.5F);
            }
        }

        beams.update(tile.getLevel(), tile.getLinks(), tile.getFlowRates(), pos -> {
            BlockEntity target = tile.getLevel().getBlockEntity(pos);
            return target instanceof ICrystalLink link ? new CrystalFXBeam<>(tile.getLevel(), tile, link) : null;
        });
    }

    @Override
    public void updateReceived(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
        tile.modifyEnergyStored(update.crystalCapacity - tile.getEnergyStored());
        applyFlowUpdates(tile.getFlowRates(), update.indexToFlowMap, 0, tile.getLinks().size());
    }

    protected static void applyFlowUpdates(List<Byte> flows, Map<Byte, Byte> changes, int offset, int size) {
        // Packet maps contain deltas and may arrive in any insertion order.
        for (int i = 0; i < size; i++) {
            byte flow = changes.getOrDefault((byte) (i + offset), i < flows.size() ? flows.get(i) : (byte) 0);
            if (i < flows.size()) {
                flows.set(i, flow);
            } else {
                flows.add(flow);
            }
        }
        while (flows.size() > size) {
            flows.remove(flows.size() - 1);
        }
    }

    @Override
    public void reloadConnections() {
        beams.reloadConnections();
    }

    @Override
    public void tileUnload() {
        if (staticFX != null) {
            staticFX.remove();
            staticFX = null;
        }
        beams.clear();
    }
}
