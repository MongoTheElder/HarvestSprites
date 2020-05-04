package tv.mongotheelder.harvestsprites.setup;


import net.minecraft.client.gui.ScreenManager;
import tv.mongotheelder.harvestsprites.HarvestSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tv.mongotheelder.harvestsprites.blocks.SpriteLampScreen;

@Mod.EventBusSubscriber(modid = HarvestSprites.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(Registration.SPRITE_LAMP_CONTAINER.get(), SpriteLampScreen::new);
        RenderTypeLookup.setRenderLayer(Registration.SPRITE_LAMP.get(), RenderType.getTranslucent());
    }
}
