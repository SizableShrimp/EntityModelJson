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

package me.sizableshrimp.entitymodeljson.animation;

import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.sizableshrimp.entitymodeljson.registry.AnimationTargetRegistry;
import me.sizableshrimp.entitymodeljson.registry.InterpolationRegistry;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;

import java.util.Arrays;

public class EntityAnimationCodecHolder {
    public static final Codec<Keyframe> KEYFRAME_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("timestamp").forGetter(Keyframe::timestamp),
            Vector3f.CODEC.fieldOf("target").forGetter(Keyframe::target),
            InterpolationRegistry.CODEC.optionalFieldOf("interpolation", AnimationChannel.Interpolations.LINEAR).forGetter(Keyframe::interpolation)
    ).apply(instance, Keyframe::new));

    public static final Codec<AnimationChannel> ANIMATION_CHANNEL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AnimationTargetRegistry.CODEC.fieldOf("target").forGetter(AnimationChannel::target),
            KEYFRAME_CODEC.listOf().fieldOf("keyframes").forGetter(ac -> Arrays.asList(ac.keyframes()))
    ).apply(instance, (target, keyframes) -> new AnimationChannel(target, keyframes.toArray(Keyframe[]::new))));

    public static final Codec<AnimationDefinition> ANIMATION_DEFINITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("lengthInSeconds").forGetter(AnimationDefinition::lengthInSeconds),
            Codec.BOOL.optionalFieldOf("looping", false).forGetter(AnimationDefinition::looping),
            Codec.unboundedMap(Codec.STRING, ANIMATION_CHANNEL_CODEC.listOf()).fieldOf("boneAnimations").forGetter(AnimationDefinition::boneAnimations)
    ).apply(instance, AnimationDefinition::new));
}
