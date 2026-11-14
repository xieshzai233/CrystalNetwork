package dev.crystalnetwork.ported.blocks.energynet.tileentity;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.lib.Vec3D;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXRing;
import dev.crystalnetwork.ported.init.CrystalContent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class TileCrystalRelay extends TileCrystalBase {
   public TileCrystalRelay(BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_RELAY_CRYSTAL.get(), pos, state);
   }

   public TileCrystalRelay(TechLevel techLevel, BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_RELAY_CRYSTAL.get(), techLevel, pos, state);
   }

   public static void register(RegisterCapabilitiesEvent event) {
      energyCapability(event, CrystalContent.TILE_RELAY_CRYSTAL);
   }

   @Override
   public EnergyCrystal.CrystalType getCrystalType() {
      return EnergyCrystal.CrystalType.RELAY;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public CrystalFXBase createStaticFX() {
      return new CrystalFXRing((ClientLevel)this.level, this);
   }

   @Override
   public Vec3D getBeamLinkPos(BlockPos linkTo) {
      Vec3D thisVec = Vec3D.getCenter(this.worldPosition);
      Vec3D targVec = Vec3D.getCenter(linkTo);
      double dist = thisVec.distXZ(targVec);
      double offM = 0.4;
      if (dist == 0.0) {
         return this.worldPosition.getY() > linkTo.getY() ? thisVec.subtract(0.0, 0.4, 0.0) : thisVec.subtract(0.0, -0.4, 0.0);
      } else {
         double xDist = thisVec.x - targVec.x;
         double zDist = thisVec.z - targVec.z;
         double xOff = xDist / dist;
         double zOff = zDist / dist;
         return thisVec.subtract(xOff * offM, 0.0, zOff * offM);
      }
   }

   @Override
   public boolean renderBeamTermination() {
      return true;
   }
}
