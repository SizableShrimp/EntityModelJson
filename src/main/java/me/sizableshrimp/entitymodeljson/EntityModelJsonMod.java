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

package me.sizableshrimp.entitymodeljson;

import com.mojang.logging.LogUtils;
import me.sizableshrimp.entitymodeljson.animation.EntityAnimationJsonReloadListener;
import me.sizableshrimp.entitymodeljson.registry.AnimationTargetRegistry;
import me.sizableshrimp.entitymodeljson.registry.InterpolationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.util.List;

@Mod(EntityModelJsonMod.MODID)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityModelJsonMod {
    public static final String MODID = "entitymodeljson";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EntityModelJsonMod() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "anything", (remoteVersion, networkBool) -> networkBool));

        if (FMLLoader.getDist() == Dist.CLIENT) {
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

            InterpolationRegistry.subscribe(modEventBus);
            AnimationTargetRegistry.subscribe(modEventBus);
        }
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        Minecraft mc = Minecraft.getInstance();
        List<PreparableReloadListener> listeners = ((ReloadableResourceManager) mc.getResourceManager()).listeners;

        int entityModelsIdx = listeners.indexOf(mc.getEntityModels());
        // We need to place this reload listener right after entity model sets,
        // so that the layer definitions will exist by the time entity renderers bake them
        listeners.add(entityModelsIdx + 1, new EntityModelJsonReloadListener());

        int entityRenderDispatcherIdx = listeners.indexOf(mc.getEntityRenderDispatcher());
        // We need to place this reload listener right before the entity render dispatcher,
        // so that the animation definitions will exist by the time that entity renderers are created
        listeners.add(Math.max(0, entityRenderDispatcherIdx - 1), new EntityAnimationJsonReloadListener());
    }
}
