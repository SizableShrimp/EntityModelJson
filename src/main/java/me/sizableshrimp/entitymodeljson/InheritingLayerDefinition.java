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

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MaterialDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A type of layer definition that can optionally inherit a material definition
 */
public class InheritingLayerDefinition extends LayerDefinition {
    public static final MaterialDefinition DEFAULT_MATERIAL = new MaterialDefinition(64, 32);
    private MaterialDefinition internalMaterial;

    public InheritingLayerDefinition(MeshDefinition mesh, Optional<MaterialDefinition> materialOptional) {
        this(mesh, materialOptional.orElse(null));
    }

    public InheritingLayerDefinition(MeshDefinition mesh, @Nullable MaterialDefinition material) {
        super(mesh, material == null ? DEFAULT_MATERIAL : material);
        this.internalMaterial = material;
    }

    @Override
    public ModelPart bakeRoot() {
        MaterialDefinition material = this.internalMaterial == null ? DEFAULT_MATERIAL : this.internalMaterial;
        return this.mesh.getRoot().bake(material.xTexSize, material.yTexSize);
    }

    @Nullable
    public MaterialDefinition getMaterial() {
        return internalMaterial;
    }

    public void setMaterial(@Nullable MaterialDefinition internalMaterial) {
        this.internalMaterial = internalMaterial;
    }
}
