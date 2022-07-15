/*
 * Copyright (c) 2022 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.entitymodeljsonexample.datagen;

import me.sizableshrimp.entitymodeljson.datagen.EntityModelProvider;
import me.sizableshrimp.entitymodeljsonexample.EntityModelJsonExampleMod;
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
