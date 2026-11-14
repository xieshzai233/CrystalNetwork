package dev.crystalnetwork.ported.api.energy;

import com.brandon3055.brandonscore.lib.Vec3D;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public interface ICrystalLink {
   @Nonnull
   List<BlockPos> getLinks();

   boolean binderUsed(Player var1, BlockPos var2, Direction var3);

   boolean createLink(ICrystalLink var1);

   void breakLink(BlockPos var1);

   int balanceMode();

   int maxLinks();

   int maxLinkRange();

   long getEnergyStored();

   long getMaxEnergyStored();

   void modifyEnergyStored(long var1);

   Vec3D getBeamLinkPos(BlockPos var1);

   boolean renderBeamTermination();
}
