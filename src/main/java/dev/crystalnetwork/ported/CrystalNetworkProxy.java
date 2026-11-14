package dev.crystalnetwork.ported;

import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandler;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import net.minecraft.world.level.Level;

public final class CrystalNetworkProxy {
    public static ENetFXHandler<?> createENetFXHandler(IENetEffectTile tile, Level level) {
        // A client process also hosts the logical server in singleplayer.
        // Construction and NBT loading can happen before a level is assigned.
        if (level == null || !level.isClientSide) {
            return tile.createServerFXHandler();
        }
        return Utils.unsafeRunForDist(() -> tile::createClientFXHandler, () -> tile::createServerFXHandler);
    }

    private CrystalNetworkProxy() { }
}
