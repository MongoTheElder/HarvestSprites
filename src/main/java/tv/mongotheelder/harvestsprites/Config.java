package tv.mongotheelder.harvestsprites;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber
public class Config {
    public static final String CATEGORY_ITEMS = "harvest_sprites";
    public static final String SPRITE_LAMP_ITEMS = "sprite_lamp";
    public static final String SPRITE_HOARD_ITEMS = "sprite_hoard";
    public static final String ENVIRONMENT_ITEMS = "environment";

    public static ForgeConfigSpec COMMON_CONFIG;

    public static ForgeConfigSpec.LongValue FOOD_CONSUMPTION_RATE;
    public static ForgeConfigSpec.IntValue HARVEST_RATE;
    public static ForgeConfigSpec.BooleanValue CONSUME_ONLY_ON_HARVEST;
    public static ForgeConfigSpec.BooleanValue SUPPRESS_SEED_DROPS;
    public static ForgeConfigSpec.IntValue TICKS_PER_CYCLE;
    public static ForgeConfigSpec.IntValue HARVEST_RANGE;
    public static ForgeConfigSpec.IntValue HARVEST_HEIGHT;
    public static ForgeConfigSpec.IntValue HOARD_RANGE;
    public static ForgeConfigSpec.IntValue HOARD_HEIGHT;
    public static ForgeConfigSpec.BooleanValue ENABLE_LIGHT;
    public static ForgeConfigSpec.BooleanValue RANDOMIZE_HOARDS;
    public static ForgeConfigSpec.BooleanValue EMIT_REDSTONE;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

        setupItems(COMMON_BUILDER);
        COMMON_CONFIG = COMMON_BUILDER.build();

    }
    private static void setupItems(ForgeConfigSpec.Builder COMMON_BUILDER) {
        COMMON_BUILDER.comment("Harvest Sprites settings").push(CATEGORY_ITEMS);

        COMMON_BUILDER.comment("Sprite Lamp settings").push(SPRITE_LAMP_ITEMS);
        FOOD_CONSUMPTION_RATE = COMMON_BUILDER.comment("Rate sprites consume food (in harvest cycles per hunger point). Zero disables food consumption. NOTE: Food MUST be present for sprites to work even when set to zero")
                .defineInRange("food_consumption_rate", 10L, 0L, 1000000000L);
        HARVEST_RATE = COMMON_BUILDER.comment("Number of crops harvested per saturation point each harvest cycle. Setting this to zero will harvest all available crops each cycle regardless of food type")
                .defineInRange("harvest_rate", 5, 0, 81);
        CONSUME_ONLY_ON_HARVEST = COMMON_BUILDER.comment("Food is consumed only when there are harvestable crops")
                .define("consume_only_on_harvest", true);
        SUPPRESS_SEED_DROPS = COMMON_BUILDER.comment("Don't drop crop seeds (secondary seeds may still drop)")
                .define("suppress_seed_drops", false);
        HARVEST_RANGE = COMMON_BUILDER.comment("Harvest range surrounding lamp. 0 will only harvest the block under/over the lamp")
                .defineInRange("harvest_range", 4, 0, 8);
        HARVEST_HEIGHT = COMMON_BUILDER.comment("Harvest range above/below lamp")
                .defineInRange("harvest_height", 3, 1, 4);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Sprite Hoard settings").push(SPRITE_HOARD_ITEMS);
        HOARD_RANGE = COMMON_BUILDER.comment("Hoard search range surrounding lamp. 0 will only search the block under/over the lamp")
                .defineInRange("hoard_range", 5, 0, 10);
        HOARD_HEIGHT = COMMON_BUILDER.comment("Hoard search range above/below lamp")
                .defineInRange("hoard_height", 3, 1, 6);
        RANDOMIZE_HOARDS = COMMON_BUILDER.comment("Choose a random Sprite Hoard each time a crop is harvested")
                .define("randomize_hoards", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Environment settings").push(ENVIRONMENT_ITEMS);
        ENABLE_LIGHT = COMMON_BUILDER.comment("Sprite Lamp emits light when active")
                .define("enable_light", true);
        EMIT_REDSTONE = COMMON_BUILDER.comment("Sprite Lamp emits a redstone signal proportional to the amount of food in the inventory")
                .define("emit_redstone", true);
        TICKS_PER_CYCLE = COMMON_BUILDER.comment("Number of game ticks between harvest cycles. Raising this value increases the time between harvest cycles")
                .defineInRange("ticks_per_cycle", 4, 1, 800);

        COMMON_BUILDER.pop().pop();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }
}
