package tv.mongotheelder.harvestsprites.setup;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.Sound;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tv.mongotheelder.harvestsprites.HarvestSprites;
import tv.mongotheelder.harvestsprites.blocks.*;

import static tv.mongotheelder.harvestsprites.HarvestSprites.MODID;

public class Registration {
    private static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<TileEntityType<?>> TILES = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, MODID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = new DeferredRegister<>(ForgeRegistries.CONTAINERS, MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = new DeferredRegister<>(ForgeRegistries.SOUND_EVENTS, MODID);

    public static void init() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static final RegistryObject<SpriteLampBlock> SPRITE_LAMP = BLOCKS.register("sprite_lamp", () -> new SpriteLampBlock(Block.Properties.create(Material.IRON).hardnessAndResistance(0.3f).sound(SoundType.LANTERN).notSolid().lightValue(7)));
    public static final RegistryObject<SpriteHoardBlock> SPRITE_HOARD = BLOCKS.register("sprite_hoard", () -> new SpriteHoardBlock(Block.Properties.create(Material.WOOD).hardnessAndResistance(0.3f).sound(SoundType.WOOD).notSolid()));
    public static final RegistryObject<Item> SPRITE_LAMP_ITEM = ITEMS.register("sprite_lamp", () -> new BlockItem(SPRITE_LAMP.get(), new Item.Properties().group(ModSetup.ITEM_GROUP)));
    public static final RegistryObject<Item> SPRITE_HOARD_ITEM = ITEMS.register("sprite_hoard", () -> new BlockItem(SPRITE_HOARD.get(), new Item.Properties().group(ModSetup.ITEM_GROUP)));

    public static final RegistryObject<TileEntityType<SpriteLampTile>> SPRITE_LAMP_TILE = TILES.register("sprite_lamp", () -> TileEntityType.Builder.create(SpriteLampTile::new, SPRITE_LAMP.get()).build(null));
    public static final RegistryObject<TileEntityType<SpriteHoardTile>> SPRITE_HOARD_TILE = TILES.register("sprite_hoard", () -> TileEntityType.Builder.create(SpriteHoardTile::new, SPRITE_HOARD.get()).build(null));

    public static final RegistryObject<ContainerType<SpriteLampContainer>> SPRITE_LAMP_CONTAINER = CONTAINERS.register("sprite_lamp", () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        return new SpriteLampContainer(windowId, HarvestSprites.proxy.getClientWorld(), pos, inv, HarvestSprites.proxy.getClientPlayer());
    }));
    public static final RegistryObject<SoundEvent> SPRITE_HOARD_OPEN = SOUNDS.register("sprite_hoard", () -> new SoundEvent(new ResourceLocation(MODID, "hoard_open")));
}
