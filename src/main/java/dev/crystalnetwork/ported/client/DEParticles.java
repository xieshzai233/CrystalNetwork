package dev.crystalnetwork.ported.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public final class DEParticles {
    public static Particle addParticleDirect(Level level, Particle particle) {
        if (level.isClientSide && Minecraft.getInstance().particleEngine != null) {
            Minecraft.getInstance().particleEngine.add(particle);
            return particle;
        }
        return null;
    }
    private DEParticles() { }
}
