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

package me.sizableshrimp.entitymodeljson.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.sizableshrimp.entitymodeljson.LayerDefinitionExporter;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class DumpCommand {
    private static final SimpleCommandExceptionType ERROR_MISSING_LAYER = new SimpleCommandExceptionType(Component.translatable("argument.model_layer_location.missing_layer"));
    private static final SimpleCommandExceptionType ERROR_MISSING_MODEL = new SimpleCommandExceptionType(Component.translatable("argument.model_layer_location.missing_model"));
    private static final SimpleCommandExceptionType ERROR_INVALID_RL = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));

    public static void register(LiteralArgumentBuilder<CommandSourceStack> rootBuilder) {
        rootBuilder.then(Commands.literal("dump")
                .then(Commands.literal("all")
                        .executes(ctx -> {
                            try {
                                Path exportFolder = getExportFolder();
                                int dumpCount = LayerDefinitionExporter.export(exportFolder);
                                Component fileComponent = createFileComponent(exportFolder);

                                ctx.getSource().sendSuccess(Component.translatable("command.entitymodeljson.dump.all.success", dumpCount, fileComponent), false);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("one")
                        .then(Commands.argument("layerLocation", DumpCommand::readLayerLocation)
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Minecraft.getInstance().getEntityModels().roots.keySet().stream().map(Object::toString), builder))
                                .executes(ctx -> {
                                    ModelLayerLocation layerLocation = ctx.getArgument("layerLocation", ModelLayerLocation.class);

                                    try {
                                        Path exportFile = LayerDefinitionExporter.exportSingle(getExportFolder(), layerLocation);
                                        if (exportFile == null) {
                                            ctx.getSource().sendFailure(Component.translatable("command.entitymodeljson.dump.one.missing", layerLocation));
                                        } else {
                                            ctx.getSource().sendSuccess(Component.translatable("command.entitymodeljson.dump.one.success", layerLocation, createFileComponent(exportFile)), false);
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    @NotNull
    private static ModelLayerLocation readLayerLocation(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        while (reader.canRead() && (reader.peek() == '#' || ResourceLocation.isAllowedInResourceLocation(reader.peek()))) {
            reader.skip();
        }
        String layerLocStr = reader.getString().substring(start, reader.getCursor());

        int idx = layerLocStr.indexOf('#');
        String layer = idx == -1 ? "" : layerLocStr.substring(idx + 1);
        if (idx == -1 || layer.isEmpty()) {
            reader.setCursor(start);
            throw ERROR_MISSING_LAYER.createWithContext(reader);
        }

        String model = layerLocStr.substring(0, idx);
        if (model.isEmpty()) {
            reader.setCursor(start);
            throw ERROR_MISSING_MODEL.createWithContext(reader);
        }

        try {
            return new ModelLayerLocation(new ResourceLocation(model), layer);
        } catch (ResourceLocationException e) {
            reader.setCursor(start);
            throw ERROR_INVALID_RL.createWithContext(reader);
        }
    }

    @NotNull
    private static Component createFileComponent(Path path) {
        return Component.literal(path.getFileName().toString())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path.toAbsolutePath().toString())));
    }

    private static Path getExportFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("layerexports");
    }
}
