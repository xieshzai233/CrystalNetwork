package dev.crystalnetwork.ported.client.render.effect;

import com.brandon3055.brandonscore.lib.Vec3D;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class CrystalFXBase<T extends BlockEntity & IENetEffectTile> extends Particle {
   protected final T tile;
   protected int ticksTillDeath = 0;
   protected float fxState;
   public boolean renderEnabled = true;
   private int ttl = 10;

   public CrystalFXBase(ClientLevel worldIn, T tile) {
      super(worldIn, (double)tile.getBlockPos().getX() + 0.5, (double)tile.getBlockPos().getY() + 0.5, (double)tile.getBlockPos().getZ() + 0.5);
      this.tile = tile;
   }

   public void updateFX(float fxState) {
      this.fxState = fxState;
      this.ticksTillDeath = 4;
      if (this.ttl-- <= 0) {
         this.remove();
      }
   }

   public void tick() {
      this.ttl = 10;
   }

   protected void setPosition(Vec3D pos) {
      this.setPos(pos.x, pos.y, pos.z);
   }

   protected Vector3f[] getRenderVectors(Camera renderInfo, float viewX, float viewY, float viewZ, float scale) {
      Quaternionf quaternion = renderInfo.rotation();
      Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
      vector3f1.rotate(quaternion);
      Vector3f[] renderVector = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F)
      };

      for (int i = 0; i < 4; i++) {
         Vector3f vector3f = renderVector[i];
         vector3f.rotate(quaternion);
         vector3f.mul(scale);
         vector3f.add(viewX, viewY, viewZ);
      }

      return renderVector;
   }
}
