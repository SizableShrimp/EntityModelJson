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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Mod(EntityModelJsonMod.MODID)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityModelJsonMod {
    public static final String MODID = "entitymodeljson";
    public static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public EntityModelJsonMod() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "anything", (remoteVersion, networkBool) -> networkBool));
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        Minecraft mc = Minecraft.getInstance();
        List<PreparableReloadListener> listeners = ((ReloadableResourceManager) mc.getResourceManager()).listeners;
        int idx = listeners.indexOf(mc.getEntityModels());
        // We need to place this reload listener right after entity model sets,
        // so that the layer definitions will exist by the time entity renderers bake them
        listeners.add(idx + 1, new EntityModelJsonReloadListener());
    }

    public static boolean dumpVanillaLayers(File outputFolder) {
        return dumpLayers("minecraft", outputFolder);
    }

    public static boolean dumpLayers(@Nullable String namespaceFilter, File outputFolder) {
        if (!outputFolder.exists() && !outputFolder.mkdirs())
            return false;

        boolean successful = true;
        for (var entry : Minecraft.getInstance().getEntityModels().roots.entrySet()) {
            ModelLayerLocation layerLocation = entry.getKey();
            LayerDefinition layerDef = entry.getValue();
            if (namespaceFilter != null && !layerLocation.getModel().getNamespace().equals(namespaceFilter))
                continue;

            File layerFolder = new File(outputFolder, layerLocation.getLayer());
            File outputFile = new File(layerFolder, layerLocation.getModel().getPath() + ".json");
            if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
                successful = false;
            JsonElement json = EntityModelCodecHolder.LAYER_DEFINITION_CODEC.encodeStart(JsonOps.INSTANCE, layerDef).get().left().orElse(null);
            if (json != null) {
                try {
                    Files.writeString(outputFile.toPath(), GSON.toJson(json));
                } catch (IOException e) {
                    LOGGER.error("Error while dumping layers with namespace filter {} and output file {}", namespaceFilter, outputFile, e);
                    successful = false;
                }
            }
        }
        return successful;
    }
}
