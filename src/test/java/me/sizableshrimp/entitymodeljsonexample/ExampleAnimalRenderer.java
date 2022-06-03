/*
 * Copyright (c) 2021 SizableShrimp
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

package me.sizableshrimp.entitymodeljsonexample;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ExampleAnimalRenderer extends MobRenderer<ExampleAnimal, EntityModel<ExampleAnimal>> {
    // The layer, in this case "main", becomes the root folder for the entity model
    // All JSON entity models reside in assets/modid/models/entity.
    // This model location translates to assets/entitymodeljsonexample/models/entity/main/example_animal.json
    public static final ModelLayerLocation EXAMPLE_ANIMAL_MODEL_LOCATION = new ModelLayerLocation(new ResourceLocation(EntityModelJsonExampleMod.MODID, "example_animal"), "main");
    // The actual texture PNG
    public static final ResourceLocation SQUID_LOCATION = new ResourceLocation("textures/entity/armorstand/wood.png");

    public ExampleAnimalRenderer(EntityRendererProvider.Context context) {
        // Bake the model location, this will get a ModelPart with the data defined in the custom JSON file
        super(context, new ExampleAnimalModel(context.bakeLayer(EXAMPLE_ANIMAL_MODEL_LOCATION)), 1);
    }

    @Override
    public ResourceLocation getTextureLocation(ExampleAnimal entity) {
        return SQUID_LOCATION;
    }

    public static class ExampleAnimalModel extends HierarchicalModel<ExampleAnimal> {
        private final ModelPart root;

        public ExampleAnimalModel(ModelPart root) {
            this.root = root;
        }

        @Override
        public void setupAnim(ExampleAnimal entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}

        @Override
        public ModelPart root() {
            return root;
        }
    }
}
