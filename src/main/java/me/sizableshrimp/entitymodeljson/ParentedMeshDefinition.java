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

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class ParentedMeshDefinition extends MeshDefinition {
    private ModelLayerLocation parent;
    private CubeDeformation universalCubeDeformation;
    private boolean overwrite;
    private boolean calculatedInheritance;

    public ParentedMeshDefinition() {
        this(null, null, null, true);
    }

    public ParentedMeshDefinition(@Nullable ModelLayerLocation parent, @Nullable CubeDeformation universalCubeDeformation, boolean overwrite) {
        this(parent, universalCubeDeformation, null, overwrite);
    }

    public ParentedMeshDefinition(@Nullable ModelLayerLocation parent, @Nullable CubeDeformation universalCubeDeformation, @Nullable PartDefinition root, boolean overwrite) {
        this.parent = parent;
        this.universalCubeDeformation = universalCubeDeformation;
        if (root != null)
            this.root = root;
        this.overwrite = overwrite;
    }

    /**
     * The parent's root part definition is used when {@link #calculateInheritance(ModelLayerLocation, Map, Map) calculating inheritance}
     * to inherit cubes into this root part definition if they do not already exist
     */
    @Nullable
    public ModelLayerLocation getParent() {
        return parent;
    }

    public void setParent(@Nullable ModelLayerLocation parent) {
        this.parent = parent;
    }

    /**
     * The universal cube deformation is additively applied to all children of the root part definition
     * when {@link #calculateInheritance(ModelLayerLocation, Map, Map) calculating inheritance}
     */
    @Nullable
    public CubeDeformation getUniversalCubeDeformation() {
        return universalCubeDeformation;
    }

    public void setUniversalCubeDeformation(@Nullable CubeDeformation universalCubeDeformation) {
        this.universalCubeDeformation = universalCubeDeformation;
    }

    /**
     * If true, overwrites any existing mesh definitions of the same model layer location with this one.
     * If false, merges this one and any existing mesh definitions of the same model layer location.
     * Defaults to true.
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * True if {@link #calculateInheritance(ModelLayerLocation, Map, Map)} has already been called for this instance, false otherwise.
     */
    public boolean hasCalculatedInheritance() {
        return calculatedInheritance;
    }

    /**
     * Uses the {@link #getParent() parent} and {@link #getUniversalCubeDeformation() universal cube deformation} to calculate the modified version of this parented mesh definition.
     * <p>
     * Cubes in the parent are added to this mesh definition's root part definition if they do not already exist.
     * <p>
     * All children of this mesh definitions' root part definition have the universal cube deformation additively applied.
     *
     * @param location The model layer location of this mesh definition, used when merging if {@link #isOverwrite() overwrite} is false
     * @param codeRoots A possibly-null map of code-defined roots, used when merging if {@link #isOverwrite() overwrite} is false
     * @param roots The map of model layer locations to layer definition roots, used to find parents
     */
    public void calculateInheritance(ModelLayerLocation location, @Nullable Map<ModelLayerLocation, LayerDefinition> codeRoots, Map<ModelLayerLocation, LayerDefinition> roots) {
        this.calculatedInheritance = true;
        if (!this.isOverwrite())
            inheritChildren(getPartDefinition(codeRoots, location));

        if (parent != null)
            inheritChildren(getPartDefinition(roots, parent));

        if (universalCubeDeformation != null) {
            for (PartDefinition child : getRoot().children.values()) {
                var cubes = new ArrayList<CubeDefinition>();
                for (CubeDefinition cube : child.cubes) {
                    CubeDeformation cubeDef = cube.grow.extend(universalCubeDeformation.growX, universalCubeDeformation.growY, universalCubeDeformation.growZ);
                    CubeDefinition newCube = EntityModelCodecHolder.createCubeDefinition(Optional.ofNullable(cube.comment),
                            cube.origin, cube.dimensions, cubeDef, cube.mirror, cube.texCoord, cube.texScale.u(), cube.texScale.v());
                    cubes.add(newCube);
                }
                child.cubes = cubes;
            }
        }
    }

    private PartDefinition getPartDefinition(Map<ModelLayerLocation, LayerDefinition> roots, ModelLayerLocation location) {
        if (roots == null)
            return null;
        LayerDefinition layer = roots.get(location);
        return layer == null ? null : layer.mesh.getRoot();
    }

    private void inheritChildren(PartDefinition partDef) {
        if (partDef == null)
            return;
        for (var entry : partDef.children.entrySet()) {
            getRoot().children.putIfAbsent(entry.getKey(), copyPartDefinition(entry.getValue()));
        }
    }

    private static PartDefinition copyPartDefinition(PartDefinition partDef) {
        PartDefinition copy = new PartDefinition(ImmutableList.copyOf(partDef.cubes), partDef.partPose);

        for (var entry : partDef.children.entrySet()) {
            copy.children.put(entry.getKey(), copyPartDefinition(entry.getValue()));
        }

        return copy;
    }
}
