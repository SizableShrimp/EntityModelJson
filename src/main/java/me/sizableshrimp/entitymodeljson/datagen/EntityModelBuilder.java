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

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.sizableshrimp.entitymodeljson.EntityModelCodecHolder;
import me.sizableshrimp.entitymodeljson.InheritingLayerDefinition;
import me.sizableshrimp.entitymodeljson.ParentedMeshDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MaterialDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ModelFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EntityModelBuilder extends ModelFile {
    private final ModelLayerLocation layerLoc;
    private final String modid;
    private final ParentedMeshDefinition mesh = new ParentedMeshDefinition();
    private final Map<String, PartDefinitionBuilder<EntityModelBuilder>> children = new HashMap<>();
    private MaterialDefinition material;

    public EntityModelBuilder(ModelLayerLocation layerLoc, ResourceLocation location, @Nullable String modid) {
        super(location);
        this.layerLoc = layerLoc;
        this.modid = modid;
    }

    @Override
    protected boolean exists() {
        return true;
    }

    /**
     * Returns the model layer location that this builder saves to.
     */
    public ModelLayerLocation getLayerLocation() {
        return layerLoc;
    }

    /**
     * Sets the parent entity model for this entity model.
     * This method does NO assertion on the existence of the parent.
     *
     * @param parent The parent entity model path.
     * If a namespace is omitted, it will default to the mod id stored in this provider.
     * @return this builder
     * @throws IllegalArgumentException if the parent path cannot be converted into a model layer location
     * @see #parent(ModelLayerLocation)
     * @see #parent(ModelFile)
     */
    public EntityModelBuilder parent(@Nonnull String parent) {
        return parent(readLayerLocOrThrow(parent, true));
    }

    /**
     * Sets the parent entity model for this entity model.
     * This method does NO assertion on the existence of the parent.
     *
     * @param parent The parent entity model layer location
     * @return this builder
     * @see #parent(String)
     * @see #parent(ModelFile)
     */
    public EntityModelBuilder parent(@Nullable ModelLayerLocation parent) {
        mesh.setParent(parent);
        return this;
    }

    /**
     * Sets the parent entity model for this entity model.
     * This method will assert that the provided parent entity model file exists.
     *
     * @param parent The parent entity model file
     * @return this builder
     * @throws IllegalArgumentException if the parent model file's location cannot be converted into a valid {@link ModelLayerLocation}
     * @see #parent(String)
     * @see #parent(ModelLayerLocation)
     */
    public EntityModelBuilder parent(@Nonnull ModelFile parent) {
        if (parent instanceof EntityModelBuilder builder) {
            mesh.setParent(builder.getLayerLocation());
        } else {
            mesh.setParent(convertOutputLocation(parent.getLocation()));
        }
        return this;
    }

    /**
     * Sets the universal cube deformation for this entity model.
     * The specified deformation will be applied to all direct children of the root part definition.
     *
     * @param universalCubeDeformation The universally applied cube deformation
     * @return this builder
     */
    public EntityModelBuilder universalCubeDeformation(CubeDeformation universalCubeDeformation) {
        mesh.setUniversalCubeDeformation(universalCubeDeformation);
        return this;
    }

    /**
     * Sets whether a model defined in code with the same model layer location will be entirely overwritten by this model.
     * This value does nothing if there is no model to overwrite.
     * If set to false and another model with the same name exists, that model will be merged with this one.
     * Merging happens by joining the children of both mesh definitions together.
     * Children defined in this model override any other. Defaults to true.
     *
     * @param overwrite Whether to overwrite a model defined in code with the same model layer location
     * @return this builder
     */
    public EntityModelBuilder overwrite(boolean overwrite) {
        mesh.setOverwrite(overwrite);
        return this;
    }

    /**
     * Provides a part definition builder for a child of the root part definition.
     *
     * @param name The name of the child part definition
     * @return the part definition builder
     */
    public PartDefinitionBuilder<EntityModelBuilder> child(String name) {
        return children.computeIfAbsent(name, n -> new PartDefinitionBuilder<>(this, n));
    }

    /**
     * Sets the material definition for this entity model.
     * If null, will attempt to inherit the material definition from a code-defined entity model using the same model layer location as this one.
     * If such entity model does not exist, then it will default to {@link InheritingLayerDefinition#DEFAULT_MATERIAL}.
     *
     * @param material The material definition
     * @return this builder
     */
    public EntityModelBuilder material(@Nullable MaterialDefinition material) {
        this.material = material;
        return this;
    }

    /**
     * Creates and sets the material definition for this entity model.
     *
     * @param xTexSize The width of the target texture
     * @param yTexSize The height of the target texture
     * @return this builder
     */
    public EntityModelBuilder material(int xTexSize, int yTexSize) {
        this.material = new MaterialDefinition(xTexSize, yTexSize);
        return this;
    }

    /**
     * Converts this entity model builder to JSON for serialization.
     */
    public JsonObject toJson() {
        children.values().forEach(child -> child.build(mesh.getRoot()));
        LayerDefinition layerDef = new LayerDefinition(mesh, material);
        return (JsonObject) EntityModelCodecHolder.LAYER_DEFINITION_CODEC.encodeStart(JsonOps.INSTANCE, layerDef).getOrThrow(false, err -> {
            throw new IllegalArgumentException("Could not encode entity model builder: " + err);
        });
    }

    protected ModelLayerLocation readLayerLocOrThrow(String path, boolean defaultToModNamespace) {
        return EntityModelProvider.readLayerLocOrThrow(path, defaultToModNamespace ? modid : null);
    }

    protected ModelLayerLocation convertOutputLocation(ResourceLocation outputLoc) {
        String entityFolder = EntityModelProvider.ENTITY_FOLDER + "/";
        String path = outputLoc.getPath().startsWith(entityFolder) ? outputLoc.getPath().substring(entityFolder.length()) : outputLoc.getPath();
        if (path.isEmpty())
            throwOutputLocException(outputLoc, "path was empty");
        int idx = path.indexOf('/');
        if (idx == -1)
            throwOutputLocException(outputLoc, "path did not contain a root model layer");
        String layer = path.substring(0, idx);
        String layerPath = path.substring(idx + 1);
        return new ModelLayerLocation(new ResourceLocation(outputLoc.getNamespace(), layerPath), layer);
    }

    private void throwOutputLocException(ResourceLocation outputLoc, String error) {
        throw new IllegalArgumentException("Output location of " + outputLoc + " could not be made into a model layer location: " + error);
    }
}
