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

package me.sizableshrimp.entitymodeljson.registry;

import com.mojang.serialization.Codec;
import me.sizableshrimp.entitymodeljson.EntityModelJsonMod;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class InterpolationRegistry {
    public static final ResourceKey<Registry<AnimationChannel.Interpolation>> KEY = ResourceKey.createRegistryKey(new ResourceLocation(EntityModelJsonMod.MODID, "interpolation"));
    // We use the minecraft namespace to signify builtin entries
    private static final DeferredRegister<AnimationChannel.Interpolation> DEFERRED_REGISTER = DeferredRegister.create(KEY, "minecraft");
    private static final Supplier<IForgeRegistry<AnimationChannel.Interpolation>> REGISTRY_SUPPLIER = DEFERRED_REGISTER.makeRegistry(() -> new RegistryBuilder<AnimationChannel.Interpolation>()
            .disableSaving()
            .disableSync());
    public static final Codec<AnimationChannel.Interpolation> CODEC = ExtraCodecs.lazyInitializedCodec(() -> getRegistry().getCodec());

    public static final RegistryObject<AnimationChannel.Interpolation> LINEAR = DEFERRED_REGISTER.register("linear", () -> AnimationChannel.Interpolations.LINEAR);
    public static final RegistryObject<AnimationChannel.Interpolation> CATMULLROM = DEFERRED_REGISTER.register("catmullrom", () -> AnimationChannel.Interpolations.CATMULLROM);

    public static void subscribe(IEventBus eventBus) {
        DEFERRED_REGISTER.register(eventBus);
    }

    public static IForgeRegistry<AnimationChannel.Interpolation> getRegistry() {
        return REGISTRY_SUPPLIER.get();
    }
}
