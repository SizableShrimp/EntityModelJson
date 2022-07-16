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
import com.google.common.collect.ListMultimap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class ParentedMeshDefinition extends MeshDefinition {
    private ModelLayerLocation parent;
    private CubeDeformation universalCubeDeformation;
    private boolean overwrite;
    private boolean fixVanillaOffset;
    private boolean calculatedInheritance;

    public ParentedMeshDefinition() {
        this(null, null, null, true, false);
    }

    public ParentedMeshDefinition(@Nullable ModelLayerLocation parent, @Nullable CubeDeformation universalCubeDeformation, boolean overwrite, boolean fixVanillaOffset) {
        this(parent, universalCubeDeformation, null, overwrite, fixVanillaOffset);
    }

    public ParentedMeshDefinition(@Nullable ModelLayerLocation parent, @Nullable CubeDeformation universalCubeDeformation, @Nullable PartDefinition root, boolean overwrite, boolean fixVanillaOffset) {
        this.parent = parent;
        this.universalCubeDeformation = universalCubeDeformation;
        if (root != null)
            this.root = root;
        this.overwrite = overwrite;
        this.fixVanillaOffset = fixVanillaOffset;
    }

    /**
     * The parent's root part definition is used when {@link #calculateInheritance(ModelLayerLocation, ListMultimap, Map) calculating inheritance}
     * to inherit cubes into this root part definition if they do not already exist
     */
    @Nullable
    public ModelLayerLocation getParent() {
        return this.parent;
    }

    public void setParent(@Nullable ModelLayerLocation parent) {
        this.parent = parent;
    }

    /**
     * The universal cube deformation is additively applied to all children of the root part definition
     * when {@link #calculateInheritance(ModelLayerLocation, ListMultimap, Map) calculating inheritance}
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
        return this.overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * True if {@link #calculateInheritance(ModelLayerLocation, ListMultimap, Map)} has already been called for this instance, false otherwise.
     */
    public boolean hasCalculatedInheritance() {
        return this.calculatedInheritance;
    }

    /**
     * @return {@code true} if the root part definition should be shifted down 24 units
     * to account for the vanilla offset of 1.501 blocks in {@link LivingEntityRenderer#render(LivingEntity, float, float, PoseStack, MultiBufferSource, int)}.
     */
    public boolean shouldFixVanillaOffset() {
        return fixVanillaOffset;
    }

    public void setFixVanillaOffset(boolean fixVanillaOffset) {
        this.fixVanillaOffset = fixVanillaOffset;
    }

    /**
     * Uses the {@link #getParent() parent} and {@link #getUniversalCubeDeformation() universal cube deformation} to calculate the modified version of this parented mesh definition.
     * <p>
     * Cubes in the parent are added to this mesh definition's root part definition if they do not already exist.
     * <p>
     * All children of this mesh definitions' root part definition have the universal cube deformation additively applied.
     * <p>
     * The root part pose is shifted down 24 units if {@link #shouldFixVanillaOffset()} is true.
     *
     * @param location The model layer location of this mesh definition, used when merging if {@link #isOverwrite() overwrite} is false
     * @param prevRoots A map of previously defined roots, both in code and earlier resource packs, used when merging if {@link #isOverwrite() overwrite} is false
     * @param roots The merged code + JSON-defined map of model layer locations to layer definition roots; used to find parents
     */
    public void calculateInheritance(ModelLayerLocation location, ListMultimap<ModelLayerLocation, LayerDefinition> prevRoots, Map<ModelLayerLocation, LayerDefinition> roots) {
        if (this.calculatedInheritance)
            return;

        this.calculatedInheritance = true;
        if (!this.overwrite)
            prevRoots.get(location).forEach(layerDef -> inheritChildren(layerDef.mesh.getRoot()));

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

        boolean fixVanillaOffset = shouldFixVanillaOffset();

        if (!fixVanillaOffset && !this.overwrite && this.parent != null) {
            LayerDefinition parentLayerDef = roots.get(this.parent);
            if (parentLayerDef != null && parentLayerDef.mesh instanceof ParentedMeshDefinition parentedMesh) {
                fixVanillaOffset = parentedMesh.shouldFixVanillaOffset();
            }
        }

        if (fixVanillaOffset) {
            PartPose prevPose = getRoot().partPose;
            // Y-axis is inverted so "up" is actually visually down
            getRoot().partPose = PartPose.offsetAndRotation(prevPose.x, prevPose.y + 24F, prevPose.z, prevPose.xRot, prevPose.yRot, prevPose.zRot);
        }
    }

    private PartDefinition getPartDefinition(Map<ModelLayerLocation, LayerDefinition> roots, ModelLayerLocation location) {
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
