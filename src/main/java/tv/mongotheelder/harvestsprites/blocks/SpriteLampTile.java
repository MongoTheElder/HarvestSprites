package tv.mongotheelder.harvestsprites.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.mongotheelder.harvestsprites.Config;
import tv.mongotheelder.harvestsprites.setup.Registration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;

public class SpriteLampTile extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private LazyOptional<IItemHandler> handler = LazyOptional.of(this::createHandler);

    private long foodCounter;
    private long foodMaxValue = 1;
    private float harvestLimit;
    private boolean done;
    private int tickCount = 0;
    private boolean needToReplant;
    private float progress;
    private int lampRedstoneLevel;

    public SpriteLampTile() {
        super(Registration.SPRITE_LAMP_TILE.get());
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(getType().getRegistryName().getPath());
    }

    private void updatePoweredState() {
        BlockState blockState = world.getBlockState(pos);
        boolean active = (foodCounter > 0);

        if (active != blockState.get(BlockStateProperties.POWERED)) {
            world.setBlockState(pos, blockState.with(BlockStateProperties.POWERED, active), 3);
        }
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0.0F, progress);
    }

    public void setProgress(long counter, long maxCount) {
        setProgress(maxCount > 0 ? counter*1.0F / maxCount : 0.0F);
    }

    public float getProgress() {
        return progress;
    }

    public void updateProgress(float progress) {
        //PacketHandler.HANDLER.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), new ProgressPacket(pos, progress));
    }

    @Override
    public void tick() {
        if (world.isRemote) {
            return;
        }

        // Only process crops on a harvest cycle
        if (tickCount++ < Config.TICKS_PER_CYCLE.get()) return;
        tickCount = 0;

        updatePoweredState();

        // Prevent crop scanning if there is no way we could harvest them
        done = false;
        if (foodCounter <= 0) {
            handler.ifPresent(h -> {
                ItemStack stack = h.getStackInSlot(0);
                if (stack.isEmpty()) {
                    done = !(Config.FOOD_CONSUMPTION_RATE.get() == 0);
                }
            });
        }
        if (done) return;

        harvestBlocks();
        consumeFood();
        markDirty();
    }

    private void harvestBlocks() {
        ArrayList<BlockPos> harvestableCropPositions = getHarvestableCropPositions();
        ArrayList<BlockPos> hoardList = locateHoards();
        if (Config.RANDOMIZE_HOARDS.get()) {
            Collections.shuffle(hoardList);
        }
        int harvestCost = (!Config.CONSUME_ONLY_ON_HARVEST.get() || !harvestableCropPositions.isEmpty()) && (Config.FOOD_CONSUMPTION_RATE.get() != 0) ? 1 : 0;

        if (foodCounter > 0) {
            foodCounter -= harvestCost;
            // Randomize the list of harvestable blocks
            Collections.shuffle(harvestableCropPositions);
            doHarvest(harvestableCropPositions, hoardList);
            setProgress(foodCounter, foodMaxValue);
            updateProgress(progress);
        }
    }

    public void foodChanged() {
        if (world == null) return;
        SpriteLampBlock block = (SpriteLampBlock) world.getBlockState(pos).getBlock();
        block.notifyNeighbors(world, pos);
    }

    private void consumeFood() {
        if (foodCounter <= 0 && Config.FOOD_CONSUMPTION_RATE.get() > 0) {
            handler.ifPresent(h -> {
                ItemStack stack = h.getStackInSlot(0);
                if (stack.getItem().isFood()) {
                    Food food = stack.getItem().getFood();
                    h.extractItem(0, 1, false);
                    foodMaxValue = food.getHealing() * Config.FOOD_CONSUMPTION_RATE.get();
                    foodCounter = foodMaxValue;
                    harvestLimit = food.getSaturation() * Config.HARVEST_RATE.get();
                    foodChanged();
                }
            });
        }
    }

    public int getLampRedstoneLevel() {
        lampRedstoneLevel = 0;
        handler.ifPresent(h -> {
            ItemStack stack = h.getStackInSlot(0);
            // This makes sure that any food present in the stack has a redstone value of at least 1
            lampRedstoneLevel = stack.getCount() == 0 ? 0 : (int) Math.floor((stack.getCount() - 1) * 15f / stack.getMaxStackSize()) + 1;
        });
        return lampRedstoneLevel;
    }

    private boolean isSeedy(ItemStack itemStack) {
        ResourceLocation tag = new ResourceLocation("forge", "seeds");
        return (itemStack.getItem().getTags().contains(tag));
    }

    private boolean isCropSeed(BlockState blockState, ItemStack itemStack) {
        CropsBlock blk = (CropsBlock) blockState.getBlock();
        Item cropSeed = blk.getItem(world, pos, blockState).getItem();
        return isSeedy(itemStack) || cropSeed == itemStack.getItem();
    }

    private boolean isValidCropBlock(BlockPos pos) {
        return world.isBlockPresent(pos) && world.getBlockState(pos).getBlock() instanceof CropsBlock;
    }

    private boolean isValidHoardBlock(BlockPos pos) {
        return world.isBlockPresent(pos) && world.getBlockState(pos).getBlock() instanceof SpriteHoardBlock;
    }

    private void doHarvest(ArrayList<BlockPos> harvestableBlocks, ArrayList<BlockPos> hoardList) {
        int harvestCount = 0;

        for (BlockPos pos : harvestableBlocks) {
            if ((harvestCount++ > harvestLimit && harvestLimit != 0.0) || !isValidCropBlock(pos)) return;

            BlockState blockState = world.getBlockState(pos);

            CropsBlock blk = (CropsBlock) blockState.getBlock();
            Item cropSeed = blk.getItem(world, pos, blockState).getItem();
            needToReplant = true;

            Block.getDrops(blockState, (ServerWorld) world, pos, (TileEntity) null).forEach(itemStack -> {
                //LOGGER.debug("Harvesting "+itemStack.getCount()+" "+itemStack.getItem().toString()+" at ("+pos.getX()+", "+pos.getZ()+"), Has a seed tag: "+isSeedy(itemStack)+", Seed is the crop: "+(itemStack.getItem() == cropSeed)+", isFood: "+cropSeed.isFood()+" cropSeed: "+cropSeed.toString());
                if (isCropSeed(blockState, itemStack) && needToReplant) {
                    needToReplant = false;
                    // Deduct one seed from the drops to account for replanting
                    if (!Config.SUPPRESS_SEED_DROPS.get() || cropSeed.isFood()) {
                        itemStack.shrink(1);
                    }
                }

                // HACK : Since some crops are their own seeds (i.e. carrots, potatoes), we need to allow them to drop
                // even if SUPPRESS_SEED_DROPS is set
                if ((!Config.SUPPRESS_SEED_DROPS.get() || itemStack.getItem() != cropSeed || cropSeed.isFood()) && !itemStack.isEmpty()) {
                    for (BlockPos p : hoardList) {
                        if (isValidHoardBlock(p)) {
                            SpriteHoardTile spriteHordeTile = (SpriteHoardTile) world.getTileEntity(p);
                            if (spriteHordeTile != null) {
                                itemStack = putStackInInventoryAllSlots(spriteHordeTile, itemStack);
                                if (itemStack.isEmpty()) break;
                            }
                        }
                    }
                    if (!itemStack.isEmpty()) {
                        Block.spawnAsEntity(world, pos, itemStack);
                    }
                }

            });

            // Reset crop age
            world.setBlockState(pos, blk.withAge(0));
        }
    }

    @SuppressWarnings("deprecation")
    private ArrayList<BlockPos> getHarvestableCropPositions() {
        int minX = pos.getX() - Config.HARVEST_RANGE.get();
        int maxX = pos.getX() + Config.HARVEST_RANGE.get();
        int minY = pos.getY() - Config.HARVEST_HEIGHT.get();
        int maxY = pos.getY() + Config.HARVEST_HEIGHT.get();
        int minZ = pos.getZ() - Config.HARVEST_RANGE.get();
        int maxZ = pos.getZ() + Config.HARVEST_RANGE.get();
        ArrayList<BlockPos> blocks = new ArrayList();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos target = new BlockPos(x, y, z);
                    if (world.isBlockLoaded(target)) {
                        BlockState blockState = world.getBlockState(target);
                        if (blockState.getBlock() instanceof CropsBlock) {
                            if (((CropsBlock) blockState.getBlock()).isMaxAge(blockState)) {
                                blocks.add(target);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return blocks;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<BlockPos> locateHoards() {
        int minX = pos.getX() - Config.HOARD_RANGE.get();
        int maxX = pos.getX() + Config.HOARD_RANGE.get();
        int minY = pos.getY() - Config.HOARD_HEIGHT.get();
        int maxY = pos.getY() + Config.HOARD_HEIGHT.get();
        int minZ = pos.getZ() - Config.HOARD_RANGE.get();
        int maxZ = pos.getZ() + Config.HOARD_RANGE.get();
        ArrayList<BlockPos> blocks = new ArrayList();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos target = new BlockPos(x, y, z);
                    if (world.isBlockLoaded(target)) {
                        BlockState blockState = world.getBlockState(target);
                        if (blockState.getBlock() == Registration.SPRITE_HOARD.get()) {
                            blocks.add(target);
                        }
                    }
                }
            }
        }
        return blocks;
    }

    @Override
    public void read(CompoundNBT tag) {
        CompoundNBT invTag = tag.getCompound("inventory");
        handler.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(invTag));

        foodCounter = tag.getLong("food_counter");
        foodMaxValue = tag.getLong("food_max_value");
        harvestLimit = tag.getFloat("harvest_limit");
        super.read(tag);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        handler.ifPresent(h -> {
            CompoundNBT compound = ((INBTSerializable<CompoundNBT>) h).serializeNBT();
            tag.put("inventory", compound);
        });

        tag.putLong("food_counter", foodCounter);
        tag.putLong("food_max_value", foodMaxValue);
        tag.putFloat("harvest_limit", harvestLimit);
        return super.write(tag);
    }

    private IItemHandler createHandler() {
        return new ItemStackHandler(1) {

            @Override
            protected void onContentsChanged(int slot) {
                foodChanged();
                markDirty();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return (stack.getItem().isFood());
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!stack.getItem().isFood()) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new SpriteLampContainer(i, world, pos, playerInventory, playerEntity);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    public static ItemStack putStackInInventoryAllSlots(IInventory destination, ItemStack stack) {
        int i = destination.getSizeInventory();

        for (int j = 0; j < i && !stack.isEmpty(); ++j) {
            stack = insertStack(destination, stack, j);
        }
        return stack;
    }

    private static ItemStack insertStack(IInventory destination, ItemStack stack, int index) {
        ItemStack itemstack = destination.getStackInSlot(index);
        if (canInsertItemInSlot(destination, stack, index)) {
            boolean flag = false;
            if (itemstack.isEmpty()) {
                destination.setInventorySlotContents(index, stack);
                stack = ItemStack.EMPTY;
                flag = true;
            } else if (canCombine(itemstack, stack)) {
                int i = stack.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag) {
                destination.markDirty();
            }
        }
        return stack;
    }

    private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) {
            return false;
        } else if (stack1.getDamage() != stack2.getDamage()) {
            return false;
        } else if (stack1.getCount() > stack1.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.areItemStackTagsEqual(stack1, stack2);
        }
    }

    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index) {
        return inventoryIn.isItemValidForSlot(index, stack);
    }

}
