package tv.mongotheelder.harvestsprites.blocks;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class SpriteHoardBlock extends BarrelBlock {

    public SpriteHoardBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new SpriteHoardTile();
    }

    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof SpriteHoardTile) {
            ((SpriteHoardTile)tileentity).hoardTick();
        }
    }

    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof SpriteHoardTile) {
                player.openContainer((SpriteHoardTile)tileentity);
                player.addStat(Stats.OPEN_BARREL);
            }
        }
        return ActionResultType.SUCCESS;
    }

}
