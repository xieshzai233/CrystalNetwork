package dev.crystalnetwork.ported;

import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandler;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;

public final class CrystalNetworkProxy {
    public static ENetFXHandler<?> createENetFXHandler(IENetEffectTile tile) {
        return Utils.unsafeRunForDist(() -> tile::createClientFXHandler, () -> tile::createServerFXHandler);
    }

    private CrystalNetworkProxy() { }
}
