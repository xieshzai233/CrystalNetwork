package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.client.DEParticles;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Keeps live effects across tile syncs, including links with no flow data yet. */
final class CrystalFXConnections<P extends CrystalFXBase<?>> {
    private final Map<BlockPos, P> effects = new LinkedHashMap<>();
    private List<BlockPos> lastTargets;

    void update(Level level, List<BlockPos> targets, List<Byte> flows, Function<BlockPos, P> factory) {
        if (!targets.equals(lastTargets)) {
            Iterator<Map.Entry<BlockPos, P>> iterator = effects.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, P> entry = iterator.next();
                if (!targets.contains(entry.getKey())) {
                    entry.getValue().remove();
                    iterator.remove();
                }
            }
            lastTargets = List.copyOf(targets);
        }

        for (int i = 0; i < targets.size(); i++) {
            BlockPos target = targets.get(i);
            P effect = effects.get(target);
            boolean created = effect == null || !effect.isAlive();
            if (created) {
                effect = factory.apply(target);
                if (effect == null) {
                    effects.remove(target);
                    continue;
                }
                effects.put(target, effect);
            }
            // A connection remains visible with the binder even while idle or awaiting sync.
            effect.updateFX(i < flows.size() ? (flows.get(i) & 255) / 255.0F : 0.0F);
            if (created) {
                DEParticles.addParticleDirect(level, effect);
            }
        }
    }

    void reloadConnections() {
        lastTargets = null;
    }

    void clear() {
        effects.values().forEach(CrystalFXBase::remove);
        effects.clear();
        lastTargets = null;
    }
}
