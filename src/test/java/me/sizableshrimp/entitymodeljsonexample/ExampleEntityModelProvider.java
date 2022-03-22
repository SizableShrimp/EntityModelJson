package me.sizableshrimp.entitymodeljsonexample;

import me.sizableshrimp.entitymodeljson.datagen.EntityModelProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ExampleEntityModelProvider extends EntityModelProvider {
    public ExampleEntityModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, EntityModelJsonExampleMod.MODID, existingFileHelper);
    }

    @Override
    public void registerEntityModels() {
        // Example builder for modifying a cat's main model

        // Either way works to get a ModelLayerLocation for the cat's main model
        // getBuilder(mcLayerLoc("cat", "main"))
        getBuilder(ModelLayers.CAT)
                .overwrite(false) // Overwrite as false means inherit any children not declared below from the original cat model
                .child("head") // Start head child of root
                .cubes(cubes -> cubes.texOffs(4, 7).addBox(1, 2, 3, 4, 5, 6)) // Demo texture offsets and a box
                .child("nose") // Make nose child of head with no data to overwrite it from the original cat model
                .end() // End nose
                .child("ear") // Start ear child of head
                .cubes(cubes -> cubes.mirror().addBox(7,8,9,10,11,12)) // Demo mirroring and adding another box
                .pose(PartPose.offset(1, 2, 3)) // Offset the ear by 1, 2, 3 relative to the head's origin
                .end() // End ear
                .end(); // End head
    }
}
