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
import com.google.common.collect.MultimapBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityModelJsonReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, List<JsonElement>>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String PATH_SUFFIX = ".json";
    private static final int PATH_SUFFIX_LENGTH = ".json".length();
    private static final String DIRECTORY = "models/entity";

    @Override
    protected Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, List<JsonElement>> entityModelJsons = new HashMap<>();

        for (Map.Entry<ResourceLocation, List<Resource>> entry : resourceManager.listResourceStacks(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation fullLocation = entry.getKey();
            String fullPath = fullLocation.getPath();
            ResourceLocation subLocation = new ResourceLocation(fullLocation.getNamespace(), fullPath.substring(DIRECTORY.length() + 1, fullPath.length() - PATH_SUFFIX_LENGTH));

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    entityModelJsons.computeIfAbsent(subLocation, k -> new ArrayList<>()).add(GsonHelper.fromJson(GSON, reader, JsonElement.class));
                } catch (IllegalArgumentException | IOException | JsonParseException e) {
                    LOGGER.error("Couldn't parse data file {} from {}", fullLocation, subLocation, e);
                }
            }
        }

        return entityModelJsons;
    }

    @Override
    protected void apply(Map<ResourceLocation, List<JsonElement>> entityModelJsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Only contains layer definitions we read from JSON
        Map<ModelLayerLocation, LayerDefinition> jsonRoots = new HashMap<>();

        // Get the entity model roots here
        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        // Contains a list of previous layer definitions for a given location, both code and JSON, for models declared multiple times
        // The list is cleared for a given location when the previous has overwrite set to true
        var prevRoots = MultimapBuilder.hashKeys().arrayListValues().<ModelLayerLocation, LayerDefinition>build();

        for (var entry : entityModelJsons.entrySet()) {
            ModelLayerLocation layerLocation = mapPathToModelLayerLoc(entry.getKey());
            if (layerLocation == null)
                continue;

            for (JsonElement jsonElement : entry.getValue()) {
                EntityModelCodecHolder.LAYER_DEFINITION_CODEC.parse(JsonOps.INSTANCE, jsonElement)
                        .resultOrPartial(e -> LOGGER.warn("Error while parsing entity model json with id {} - {}", entry.getKey(), e))
                        .ifPresent(layerDef -> {
                            LayerDefinition prevLayerDef = jsonRoots.get(layerLocation); // Check for previously defined layer in an earlier resource pack
                            if (prevLayerDef == null)
                                prevLayerDef = entityModels.roots.get(layerLocation); // Fallback to code root

                            if (prevLayerDef != null) {
                                if (prevLayerDef.mesh instanceof ParentedMeshDefinition parentedMesh && parentedMesh.isOverwrite())
                                    prevRoots.removeAll(layerLocation);
                                prevRoots.put(layerLocation, prevLayerDef);

                                if (layerDef instanceof InheritingLayerDefinition inheritingLayerDef && inheritingLayerDef.getMaterial() == null) {
                                    inheritingLayerDef.setMaterial(prevLayerDef instanceof InheritingLayerDefinition inheritingPrev
                                            ? inheritingPrev.getMaterial()
                                            : prevLayerDef.material);
                                }
                            }

                            jsonRoots.put(layerLocation, layerDef);
                        });
            }
        }

        // Contains the final merged view of json roots + code roots, with json roots taking precedence
        var roots = new HashMap<>(entityModels.roots);

        // Add all the json defined layer definitions, possibly overwriting ones written in code
        roots.putAll(jsonRoots);

        // Calculate inheritance
        jsonRoots.forEach((location, layer) -> {
            if (layer.mesh instanceof ParentedMeshDefinition parentedMeshDef) {
                parentedMeshDef.calculateInheritance(location, prevRoots, roots);
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
