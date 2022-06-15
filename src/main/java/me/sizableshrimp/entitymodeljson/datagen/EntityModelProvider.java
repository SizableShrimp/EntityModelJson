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

package me.sizableshrimp.entitymodeljson.datagen;

import me.sizableshrimp.entitymodeljson.EntityModelCodecHolder;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A data provider for JSON entity models
 */
public abstract class EntityModelProvider implements DataProvider {
    protected static final ExistingFileHelper.ResourceType MODEL = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".json", "models");
    public static final String ENTITY_FOLDER = "entity";

    protected final DataGenerator generator;
    protected final String modid;
    protected final ExistingFileHelper existingFileHelper;
    private final Map<ResourceLocation, EntityModelBuilder> generatedModels = new HashMap<>();

    protected EntityModelProvider(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
        this.generator = generator;
        this.modid = modid;
        this.existingFileHelper = existingFileHelper;
    }

    /**
     * Registers all the entity models for this provider.
     * An entity model can be registered by retrieving an {@link EntityModelBuilder} using one of the helper methods, like {@link #getBuilder(ModelLayerLocation)}.
     */
    public abstract void registerEntityModels();

    @Override
    public void run(CachedOutput cache) {
        clear();
        registerEntityModels();
        generateAll(cache);
    }

    /**
     * Gets a builder instance by converting this path into a {@link ModelLayerLocation} and calling {@link #getBuilder(ModelLayerLocation)}.
     * The returned builder is stored in this provider and can be used to build an entity model which is saved as JSON.
     *
     * @param path The string path to be converted into a {@link ModelLayerLocation}.
     * If a namespace is omitted, it will default to the mod id stored in this provider.
     * @return an entity model builder stored in this provider
     * @throws IllegalArgumentException if the path cannot be converted into a model layer location
     * @see #getBuilder(ModelLayerLocation)
     */
    public EntityModelBuilder getBuilder(String path) {
        ModelLayerLocation layerLoc = readLayerLocOrThrow(path, modid);
        ResourceLocation outputLoc = extendWithFolder(layerLoc);
        return getBuilder(layerLoc, outputLoc);
    }

    /**
     * Gets a builder instance.
     * The returned builder is stored in this provider and can be used to build an entity model which is saved as JSON.
     *
     * @param layerLoc The model layer location used to retrieve a builder
     * @return an entity model builder stored in this provider
     * @throws IllegalArgumentException if the path cannot be converted into a model layer location
     * @see #getBuilder(String)
     */
    public EntityModelBuilder getBuilder(ModelLayerLocation layerLoc) {
        ResourceLocation outputLoc = extendWithFolder(layerLoc);
        return getBuilder(layerLoc, outputLoc);
    }

    private EntityModelBuilder getBuilder(ModelLayerLocation layerLoc, ResourceLocation outputLoc) {
        this.existingFileHelper.trackGenerated(outputLoc, MODEL);
        return generatedModels.computeIfAbsent(outputLoc, o -> new EntityModelBuilder(layerLoc, outputLoc, modid));
    }

    /**
     * Creates a {@link ResourceLocation resource location} using the mod id stored in this provider as the namespace.
     *
     * @param path The path part of the resource location
     * @return a new {@link ResourceLocation}
     */
    public ResourceLocation modLoc(String path) {
        return new ResourceLocation(modid, path);
    }

    /**
     * Creates a {@link ModelLayerLocation model layer location} using the mod id stored in this provider as the namespace.
     *
     * @param path The path part of the resource location
     * @param layer The model's layer
     * @return a new {@link ModelLayerLocation}
     */
    public ModelLayerLocation modLayerLoc(String path, String layer) {
        return new ModelLayerLocation(modLoc(path), layer);
    }

    /**
     * Creates a {@link ResourceLocation resource location} using "minecraft" as the namespace.
     *
     * @param path The path part of the resource location
     * @return a new {@link ResourceLocation}
     */
    public ResourceLocation mcLoc(String path) {
        return new ResourceLocation("minecraft", path);
    }

    /**
     * Creates a {@link ModelLayerLocation model layer location} using "minecraft" as the namespace.
     *
     * @param path The path part of the resource location
     * @param layer The model's layer
     * @return a new {@link ModelLayerLocation}
     */
    public ModelLayerLocation mcLayerLoc(String path, String layer) {
        return new ModelLayerLocation(mcLoc(path), layer);
    }

    /**
     * Gets an existing entity model file instance for the entity model path specified.
     *
     * @param path The string path, which is converted into a {@link ModelLayerLocation} and checked for existence, then returned.
     * If a namespace is omitted, it will default to the mod id stored in this provider.
     * @return An existing entity model file instance
     * @throws IllegalStateException if the provided path does not exist
     * @throws IllegalArgumentException if the path cannot be converted into a model layer location
     */
    public ModelFile.ExistingModelFile getExistingFile(String path) {
        ResourceLocation outputLoc = extendWithFolder(readLayerLocOrThrow(path, modid));
        return getExistingFile(outputLoc);
    }

    /**
     * Gets an existing entity model file instance for the entity model path specified.
     *
     * @param layerLoc The model layer location, which is checked for existence, then returned
     * @return An existing entity model file instance
     * @throws IllegalStateException if the provided layer location does not exist
     */
    public ModelFile.ExistingModelFile getExistingFile(ModelLayerLocation layerLoc) {
        return getExistingFile(extendWithFolder(layerLoc));
    }

    private ModelFile.ExistingModelFile getExistingFile(ResourceLocation outputLoc) {
        ModelFile.ExistingModelFile ret = new ModelFile.ExistingModelFile(outputLoc, existingFileHelper);
        ret.assertExistence();
        return ret;
    }

    /**
     * Helper which creates a builder with the path specified,
     * then sets the parent with an existing file (must be json!) from {@link #getExistingFile(String)}
     *
     * @param path The string path used to retrieve the builder
     * @param parent The parent entity model
     * @return an entity model builder stored in this provider
     * @throws IllegalArgumentException if the path or parent model path cannot be converted into a model layer location
     * @throws IllegalStateException if the provided parent model does not exist
     * @see #getBuilder(String)
     * @see #getExistingFile(String)
     */
    public EntityModelBuilder withExistingParent(String path, String parent) {
        return getBuilder(path).parent(getExistingFile(parent));
    }

    /**
     * Helper which creates a builder with the model layer location specified,
     * then sets the parent with an existing file (must be json!) from {@link #getExistingFile(ModelLayerLocation)}
     *
     * @param layerLoc The model layer location used to retrieve the builder
     * @param parent The parent entity model
     * @return an entity model builder stored in this provider
     * @throws IllegalStateException if the provided parent model does not exist
     * @see #getBuilder(ModelLayerLocation)
     * @see #getExistingFile(ModelLayerLocation)
     */
    public EntityModelBuilder withExistingParent(ModelLayerLocation layerLoc, ModelLayerLocation parent) {
        return getBuilder(layerLoc).parent(getExistingFile(parent));
    }

    /**
     * Clears the currently generated models stored in this provider
     */
    protected void clear() {
        this.generatedModels.clear();
    }

    /**
     * Generates and saves the JSON forms of all generated models currently stored in this provider
     *
     * @param cache The hash cache used for up-to-date checking
     */
    protected void generateAll(CachedOutput cache) {
        for (EntityModelBuilder model : generatedModels.values()) {
            Path target = getPath(model);
            try {
                DataProvider.saveStable(cache, model.toJson(), target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ResourceLocation extendWithFolder(ResourceLocation rl) {
        return rl.getPath().startsWith(ENTITY_FOLDER + "/") ? rl : new ResourceLocation(rl.getNamespace(), ENTITY_FOLDER + "/" + rl.getPath());
    }

    private ResourceLocation extendWithFolder(ModelLayerLocation layerLoc) {
        return new ResourceLocation(layerLoc.getModel().getNamespace(), ENTITY_FOLDER + "/" + layerLoc.getLayer() + "/" + layerLoc.getModel().getPath());
    }

    protected static ModelLayerLocation readLayerLocOrThrow(String path, @Nullable String defaultNamespace) {
        if (defaultNamespace != null && path.indexOf(':') == -1)
            path = defaultNamespace + ':' + path;
        return EntityModelCodecHolder.readModelLayerLocation(path).getOrThrow(false, err -> {
            throw new IllegalArgumentException(err);
        });
    }

    private Path getPath(EntityModelBuilder model) {
        ResourceLocation loc = model.getLocation();
        return generator.getOutputFolder().resolve("assets/" + loc.getNamespace() + "/models/" + loc.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Entity Models: " + modid;
    }
}
