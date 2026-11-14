package dev.crystalnetwork.ported.blocks.energynet.rendering;

import com.brandon3055.brandonscore.lib.Vec3D;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXLink;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXWireless;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;

public class ENetFXHandlerClientWireless extends ENetFXHandlerClient<TileCrystalWirelessIO> {
    private final CrystalFXConnections<CrystalFXWireless> transfers = new CrystalFXConnections<>();
    private final CrystalFXConnections<CrystalFXLink> links = new CrystalFXConnections<>();

    public ENetFXHandlerClientWireless(TileCrystalWirelessIO tile) {
        super(tile);
    }

    @Override
    public void update() {
        super.update();
        transfers.update(tile.getLevel(), tile.getReceivers(), tile.receiverFlowRates,
                pos -> new CrystalFXWireless((ClientLevel) tile.getLevel(), tile, pos));
        if (ClientEventHandler.playerHoldingWrench) {
            links.update(tile.getLevel(), tile.getReceivers(), List.of(),
                    pos -> new CrystalFXLink((ClientLevel) tile.getLevel(), tile, Vec3D.getCenter(pos)));
        } else {
            links.clear();
        }
    }

    @Override
    public void updateReceived(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
        super.updateReceived(update);
        applyFlowUpdates(tile.receiverFlowRates, update.indexToFlowMap, 128, tile.getReceivers().size());
    }

    @Override
    public void reloadConnections() {
        super.reloadConnections();
        transfers.reloadConnections();
        links.reloadConnections();
    }

    @Override
    public void tileUnload() {
        super.tileUnload();
        transfers.clear();
        links.clear();
    }
}
