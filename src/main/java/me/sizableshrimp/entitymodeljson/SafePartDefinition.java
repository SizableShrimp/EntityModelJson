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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A safe variant of {@link PartDefinition} that returns a {@link SafeModelPart safe model part} when baking.
 */
public class SafePartDefinition extends PartDefinition {
    public SafePartDefinition(List<CubeDefinition> cubes, PartPose partPose) {
        super(cubes, partPose);
    }

    @Override
    public SafeModelPart bake(int texWidth, int texHeight) {
        Object2ObjectArrayMap<String, ModelPart> map = this.children.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().bake(texWidth, texHeight), (firstPart, secondPart) -> firstPart, Object2ObjectArrayMap::new));
        List<ModelPart.Cube> list = this.cubes.stream().map(cube -> cube.bake(texWidth, texHeight)).toList();

        SafeModelPart modelPart = new SafeModelPart(list, map);
        modelPart.loadPose(this.partPose);
        return modelPart;
    }
}
