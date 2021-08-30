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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A safe variant of {@link ModelPart}.
 * If a child is requested from this model part that does not exist, it will return an empty model part, which is also safe like this one.
 * The same empty model part instance is guaranteed to be returned for each call to {@link #getChild(String)} with the same name.
 */
public class SafeModelPart extends ModelPart {
    protected final Map<String, Empty> cachedEmpties = new HashMap<>();

    public SafeModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
        super(cubes, children);
    }

    @Override
    public ModelPart getChild(String name) {
        ModelPart childPart = this.children.get(name);
        return childPart == null ? getEmptyChild(name) : childPart;
    }

    protected ModelPart getEmptyChild(String name) {
        return cachedEmpties.computeIfAbsent(name, n -> new Empty());
    }

    private static class Empty extends SafeModelPart {
        private Empty() {
            super(List.of(), Map.of());
        }

        @Override
        public ModelPart getChild(String name) {
            return getEmptyChild(name);
        }
    }
}
