/*
 * Copyright (c) 2022 SizableShrimp
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

public class LayerDefinitionExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Nullable
    public static Path exportSingle(Path exportFolder, ModelLayerLocation layerLoc) throws IOException {
        LayerDefinition layerDef = Minecraft.getInstance().getEntityModels().roots.get(layerLoc);
        if (layerDef == null)
            return null;

        Path outputPath = getOutputPath(exportFolder, layerLoc);
        write(layerDef, outputPath);
        return outputPath;
    }

    public static int export(Path exportFolder) throws IOException {
        return export(exportFolder, ml -> true);
    }

    public static int export(Path exportFolder, Predicate<ModelLayerLocation> filter) throws IOException {
        if (Files.isRegularFile(exportFolder))
            throw new IllegalArgumentException("Export folder must be a directory but was a file instead: " + exportFolder);

        int count = 0;

        for (var entry : Minecraft.getInstance().getEntityModels().roots.entrySet()) {
            ModelLayerLocation layerLoc = entry.getKey();
            if (!filter.test(layerLoc))
                continue;

            write(entry.getValue(), getOutputPath(exportFolder, layerLoc));
            count++;
        }

        return count;
    }

    private static Path getOutputPath(Path exportFolder, ModelLayerLocation layerLoc) {
        return exportFolder.resolve(layerLoc.getModel().getNamespace()).resolve(layerLoc.getLayer()).resolve(layerLoc.getModel().getPath() + ".json");
    }

    private static void write(LayerDefinition layerDef, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        JsonElement layerJson = EntityModelCodecHolder.LAYER_DEFINITION_CODEC.encodeStart(JsonOps.INSTANCE, layerDef).result().orElseThrow();
        Files.writeString(outputPath, GSON.toJson(layerJson), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
