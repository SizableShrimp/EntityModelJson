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

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PartDefinitionBuilder<T> {
    private final T parentBuilder;
    private final String name;
    private final Map<String, PartDefinitionBuilder<PartDefinitionBuilder<T>>> children = new HashMap<>();
    private CubeListBuilder cubeBuilder = new CubeListBuilder();
    private PartPose partPose;

    public PartDefinitionBuilder(T parentBuilder, String name) {
        this.parentBuilder = parentBuilder;
        this.name = name;
    }


    /**
     * Returns the name of this child builder defined in the parent builder
     */
    public String getName() {
        return name;
    }

    /**
     * Provides the cube list builder stored in this part definition builder to the consumer for configuration.
     *
     * @param consumer The consumer which is used to configure the cube list builder in this part definition builder
     * @return this builder
     */
    public PartDefinitionBuilder<T> cubes(Consumer<CubeListBuilder> consumer) {
        consumer.accept(this.cubeBuilder);
        return this;
    }

    /**
     * Adds all the cubes in the cube list builder to this part definition's stored cube definition builder
     *
     * @param cubeBuilder The cubes to add to this part definition's stored cube definition builder
     * @return this builder
     */
    public PartDefinitionBuilder<T> cubes(CubeListBuilder cubeBuilder) {
        cubeBuilder.getCubes().addAll(cubeBuilder.getCubes());
        return this;
    }

    /**
     * Provides a child part definition builder of this parent part definition.
     *
     * @param name The name of the child part definition
     * @return the child part definition builder
     */
    public PartDefinitionBuilder<PartDefinitionBuilder<T>> child(String name) {
        return children.computeIfAbsent(name, n -> new PartDefinitionBuilder<>(this, n));
    }

    /**
     * Sets the offset and rotation for this part definition.
     *
     * @param partPose The offset and rotation. If set to null, will default to {@link PartPose#ZERO}.
     * @return this builder
     */
    public PartDefinitionBuilder<T> pose(@Nullable PartPose partPose) {
        this.partPose = partPose;
        return this;
    }

    /**
     * Ends configuration of this child part definition builder and returns the instance of the parent builder for chaining
     */
    public T end() {
        return parentBuilder;
    }

    /**
     * Builds this builder
     *
     * @param parent The parent part definition to add this child part definition builder to
     * @return A built part definition
     */
    public PartDefinition build(PartDefinition parent) {
        PartDefinition thisPart = parent.addOrReplaceChild(name, cubeBuilder, partPose == null ? PartPose.ZERO : partPose);
        children.values().forEach(child -> child.build(thisPart));
        return thisPart;
    }
}
