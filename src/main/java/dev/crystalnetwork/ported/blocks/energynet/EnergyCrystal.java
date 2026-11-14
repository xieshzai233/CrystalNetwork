package dev.crystalnetwork.ported.blocks.energynet;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.api.hud.IHudBlock;
import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.brandonscore.blocks.TileBCore;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.init.CrystalContent;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EnergyCrystal extends EntityBlockBCore implements IHudBlock {
   private final TechLevel techLevel;
   private final EnergyCrystal.CrystalType crystalType;
   private static VoxelShape CRYSTAL_SHAPE = Shapes.create(new AABB(0.375, 0.125, 0.375, 0.625, 0.875, 0.625));
   private static VoxelShape[] IO_CRYSTAL_SHAPES = new VoxelShape[6];

   public EnergyCrystal(Properties properties, TechLevel techLevel, EnergyCrystal.CrystalType crystalType) {
      super(properties);
      this.techLevel = techLevel;
      this.crystalType = crystalType;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return this.crystalType.createTile(this.techLevel, pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
      BlockEntityType<?> expectedType = switch (this.crystalType) {
         case RELAY -> CrystalContent.TILE_RELAY_CRYSTAL.get();
         case CRYSTAL_IO -> CrystalContent.TILE_IO_CRYSTAL.get();
         case WIRELESS -> CrystalContent.TILE_WIRELESS_CRYSTAL.get();
      };
      return entityType == expectedType ? (e, e2, e3, tile) -> ((TileBCore)tile).tick() : null;
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (this.crystalType == EnergyCrystal.CrystalType.CRYSTAL_IO) {
         BlockEntity tile = world.getBlockEntity(pos);
         Direction facing = tile instanceof TileCrystalDirectIO ? (Direction)((TileCrystalDirectIO)tile).facing.get() : Direction.DOWN;
         return IO_CRYSTAL_SHAPES[facing.get3DDataValue()];
      } else {
         return CRYSTAL_SHAPE;
      }
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public void generateHudText(Level world, BlockPos pos, Player player, List<Component> displayList) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te instanceof TileCrystalBase) {
         displayList.add(Component.translatable(this.asItem().getDescriptionId()).withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GOLD}));
         TileCrystalBase tile = (TileCrystalBase)te;
         tile.addDisplayData(displayList);
      }
   }

   static {
      for (Direction dir : Direction.values()) {
         Cuboid6 c = new Cuboid6(0.35, 0.0, 0.35, 0.65, 0.425, 0.65);
         c.apply(Rotation.sideRotations[dir.get3DDataValue()].at(Vector3.CENTER));
         IO_CRYSTAL_SHAPES[dir.get3DDataValue()] = Shapes.create(c.aabb());
      }
   }

   public static enum CrystalType implements StringRepresentable {
      RELAY(0) {
         @Override
         public BlockEntity createTile(TechLevel techLevel, BlockPos pos, BlockState state) {
            return new TileCrystalRelay(techLevel, pos, state);
         }
      },
      CRYSTAL_IO(1) {
         @Override
         public BlockEntity createTile(TechLevel techLevel, BlockPos pos, BlockState state) {
            return new TileCrystalDirectIO(techLevel, pos, state);
         }
      },
      WIRELESS(2) {
         @Override
         public BlockEntity createTile(TechLevel techLevel, BlockPos pos, BlockState state) {
            return new TileCrystalWirelessIO(techLevel, pos, state);
         }
      };

      private final int index;

      private CrystalType(int index) {
         this.index = index;
      }

      public int getIndex() {
         return this.index;
      }

      private static EnergyCrystal.CrystalType fromIndex(int i) {
         return i == 0 ? RELAY : (i == 1 ? CRYSTAL_IO : WIRELESS);
      }

      public String getSerializedName() {
         return this.name().toLowerCase(Locale.ENGLISH);
      }

      public abstract BlockEntity createTile(TechLevel var1, BlockPos var2, BlockState var3);
   }
}
