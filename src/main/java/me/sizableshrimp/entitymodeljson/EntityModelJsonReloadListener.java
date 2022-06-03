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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class EntityModelJsonReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public EntityModelJsonReloadListener() {
        super(GSON, "models/entity");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entityModelJsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ModelLayerLocation, LayerDefinition> jsonRoots = new HashMap<>();

        // Get the entity model roots here
        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        var codeRoots = entityModels.roots;
        var roots = new HashMap<>(codeRoots);

        // Export vanilla model roots
        // VanillaLayerExporter.export(path);

        for (var entry : entityModelJsons.entrySet()) {
            ModelLayerLocation layerLocation = mapPathToModelLayerLoc(entry.getKey());
            if (layerLocation == null)
                continue;
            EntityModelCodecHolder.LAYER_DEFINITION_CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(e -> LOGGER.error("Error while parsing entity model json: {}", e))
                    .ifPresent(layerDef -> {
                        if (layerDef instanceof InheritingLayerDefinition inheritingLayerDef && inheritingLayerDef.getMaterial() == null) {
                            LayerDefinition oldLayerDef = roots.get(layerLocation);
                            if (oldLayerDef != null)
                                inheritingLayerDef.setMaterial(oldLayerDef.material);
                        }
                        jsonRoots.put(layerLocation, layerDef);
                    });
        }

        // Add all the json defined layer definitions, possibly overwriting ones written in code
        roots.putAll(jsonRoots);

        // Calculate inheritance
        jsonRoots.forEach((location, layer) -> {
            if (layer.mesh instanceof ParentedMeshDefinition parentedMeshDef) {
                parentedMeshDef.calculateInheritance(location, codeRoots, roots);
            }
        });

        entityModels.roots = ImmutableMap.copyOf(roots);
    }

    private static ModelLayerLocation mapPathToModelLayerLoc(ResourceLocation path) {
        int idx = path.getPath().indexOf('/');
        if (idx == -1) {
            LOGGER.error("Entity model path of {} was invalid, must contain at least one folder", path);
            return null;
        }

        return new ModelLayerLocation(new ResourceLocation(path.getNamespace(), path.getPath().substring(idx + 1)), path.getPath().substring(0, idx));
    }
}
