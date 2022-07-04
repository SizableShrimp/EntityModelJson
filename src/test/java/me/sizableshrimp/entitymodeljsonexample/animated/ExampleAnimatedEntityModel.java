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

package me.sizableshrimp.entitymodeljsonexample.animated;

import me.sizableshrimp.entitymodeljson.animation.EntityAnimations;
import me.sizableshrimp.entitymodeljsonexample.EntityModelJsonExampleMod;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.definitions.WardenAnimation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.warden.Warden;

public class ExampleAnimatedEntityModel extends HierarchicalModel<Warden> {
    // All JSON entity animation definitions reside in assets/modid/animations/entity.
    // This resource location with path "warden/attack" translates to assets/entitymodeljsonexample/animations/entity/warden/attack.json
    private static final ResourceLocation ATTACK_LOCATION = new ResourceLocation(EntityModelJsonExampleMod.MODID, "warden/attack");
    private final ModelPart root;
    private final AnimationDefinition attackAnimation;

    public ExampleAnimatedEntityModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        // Query the animation definition from the loaded animations
        this.attackAnimation = EntityAnimations.getDefinitionOrThrow(ATTACK_LOCATION);
    }

    @Override
    public void setupAnim(Warden warden, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Keep playing the attack animation to demo it
        if (warden.attackAnimationState.getAccumulatedTime() >= this.attackAnimation.lengthInSeconds() * 1000L) {
            warden.attackAnimationState.start(warden.tickCount);
        }

        // Animate the attack animation; this should always be run and will just do nothing if the animation is not playing
        this.animate(warden.attackAnimationState, this.attackAnimation, ageInTicks);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
