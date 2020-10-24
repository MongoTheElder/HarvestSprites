package tv.mongotheelder.harvestsprites.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.mongotheelder.harvestsprites.Config;

import javax.annotation.Nullable;
import java.util.Random;

public class SpriteLampBlock extends Block {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BooleanProperty CAN_EMIT_LIGHT = BooleanProperty.create("can_emit_light");

    public SpriteLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new SpriteLampTile();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult trace) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof INamedContainerProvider) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (INamedContainerProvider) tileEntity, tileEntity.getPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.POWERED, CAN_EMIT_LIGHT);
    }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {

        // HACK ALERT: Do to the change in the way block light emission works in 1.16+, needed to add a block state to
        // track the config item since the light levels are determined for all block state combinations
        // during registration before the config is available.
        if (Config.ENABLE_LIGHT.get() != stateIn.get(CAN_EMIT_LIGHT)) {
            worldIn.setBlockState(pos, stateIn.with(CAN_EMIT_LIGHT, Config.ENABLE_LIGHT.get()));
        }

        if (stateIn.get(BlockStateProperties.POWERED)) {
            double d0 = (double) pos.getX() + 0.5D;
            double d1 = (double) pos.getY() + 0.55D;
            double d2 = (double) pos.getZ() + 0.5D;
            worldIn.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
    }

    public static int getLightValue(BlockState state, int lightLevel) {
        return state.get(BlockStateProperties.POWERED) && state.get(CAN_EMIT_LIGHT) ? lightLevel : 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(BlockState state) {
        return Config.EMIT_REDSTONE.get();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
        if (!Config.EMIT_REDSTONE.get()) return 0;

        SpriteLampTile te = (SpriteLampTile) blockAccess.getTileEntity(pos);
        if (te == null) return 0;
        return te.getLampRedstoneLevel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
        return getWeakPower(blockState, blockAccess, pos, side) > 0 ? 15 : 0;
    }

    public void notifyNeighbors(World world, BlockPos pos) {
        world.notifyNeighborsOfStateChange(pos, this);
    }

}
