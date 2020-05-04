package tv.mongotheelder.harvestsprites.setup;

import tv.mongotheelder.harvestsprites.HarvestSprites;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = HarvestSprites.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModSetup {

    public static final ItemGroup ITEM_GROUP = new ItemGroup("harvestsprites") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Registration.SPRITE_LAMP.get());
        }
    };

    public static void init(final FMLCommonSetupEvent event) { }
}
