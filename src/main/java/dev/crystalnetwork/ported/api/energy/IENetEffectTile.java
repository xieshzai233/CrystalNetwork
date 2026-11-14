package dev.crystalnetwork.ported.api.energy;

import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandler;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import java.util.LinkedList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IENetEffectTile extends ICrystalLink {
   ENetFXHandler createServerFXHandler();

   @OnlyIn(Dist.CLIENT)
   ENetFXHandler createClientFXHandler();

   @OnlyIn(Dist.CLIENT)
   CrystalFXBase createStaticFX();

   LinkedList<Byte> getFlowRates();

   int getTier();

   int getIDHash();

   default boolean hasStaticFX() {
      return true;
   }
}
