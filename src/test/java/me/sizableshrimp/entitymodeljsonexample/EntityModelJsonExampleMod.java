package me.sizableshrimp.entitymodeljsonexample;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod(EntityModelJsonExampleMod.MODID)
public class EntityModelJsonExampleMod {
    public static final String MODID = "entitymodeljsonexample";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
    public static final RegistryObject<EntityType<ExampleAnimal>> EXAMPLE_ANIMAL = registerEntity("animal", () -> EntityType.Builder.of(ExampleAnimal::new, MobCategory.AMBIENT));

    public EntityModelJsonExampleMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.register(this);
        ENTITIES.register(modBus);
    }

    @SubscribeEvent
    public void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EXAMPLE_ANIMAL.get(), ExampleAnimalRenderer::new);
    }

    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EXAMPLE_ANIMAL.get(), ExampleAnimal.createAttributes().build());
    }

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        if (event.includeClient()) {
            generator.addProvider(new ExampleEntityModelProvider(generator, existingFileHelper));
        }
    }

    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> supplier) {
        return ENTITIES.register(name, () -> supplier.get().build(name));
    }
}
