/*
 * Copyright (c) 2021 FoundationGames
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

(function () {

    const CODEC = new Codec("javaentitymodeljson_entity", {
        name: "Java Entity Model JSON",
        load_filter: {
            extensions: ["json"],
            type: "json",
            condition: function (json) {
                return "mesh" in json;
            }
        },
        extension: "json",
        remember: true,
        parse(model, path) {
            // console.log(`Parsing Model: ${JSON.stringify(model)}`)

            let mesh = model.mesh;

            if ("material" in model) {
                let material = model.material;
                Project.texture_width = material.xTexSize;
                Project.texture_height = material.yTexSize;
            } else {
                Project.texture_width = 64;
                Project.texture_height = 32;
            }

            if ("root" in mesh) {
                addBone(undefined, [0, 0, 0], "root", mesh.root);
            }
        },
        compile() {
            let compiled = {material: {}, mesh: {}};

            compiled.material.xTexSize = Project.texture_width;
            compiled.material.yTexSize = Project.texture_height;

            if (Outliner.root.length === 1) {
                compiled.mesh.root = compileBone(Outliner.root[0], [0, 0, 0])
            } else {
                let root = {};
                compiled.mesh.root = root;
                for (node of Outliner.root) {
                    if (node instanceof Group) {
                        if (!("children" in root))
                            root.children = {};
                        root.children[node.name] = compileBone(node, [0, 0, 0]);
                    } else if (node instanceof Cube) {
                        if (!("cubes" in root))
                            root.cubes = {};
                        root.cubes[node.name] = compileCube(node, [0, 0, 0]);
                    }
                }
            }

            // console.log(`Exporting Model: ${JSON.stringify(compiled)}`)

            return JSON.stringify(compiled, null, 2);
        },
        export_action: new Action("export_javaentitymodeljson", {
            name: "Export Java Entity Model JSON",
            icon: "icon-format_java",
            category: "file",
            click: () => CODEC.export()
        })
    });

    const FORMAT = new ModelFormat({
        id: "javaentitymodeljson",
        icon: "icon-format_java",
        name: "Java Entity Model JSON",
        description: "Entity model for Minecraft Java Edition through the Entity Model JSON mod.",
        show_on_start_screen: true,
        box_uv: true,
        single_texture: true,
        bone_rig: true,
        centered_grid: true,
        rotate_cubes: true,
        display_mode: true,
        animation_mode: true,
        // animation_files: true,
        codec: CODEC,
        onActivation() {
            MenuBar.addAction(CODEC.export_action, "file.export")
        },
        onDeactivation() {
            CODEC.export_action.delete();
        }
    });
    CODEC.format = FORMAT;

    Plugin.register("javaentitymodeljson_models", {
        title: "Java Entity Model JSON Support",
        author: "SizableShrimp",
        description: "Create models and animations to be used with https://github.com/SizableShrimp/EntityModelJson",
        icon: "icon-format_java",
        version: "1.1.0",
        variant: "both",
        about: 'This plugin adds support for exporting models and keyframe animations to JSON files for use with the Entity Model JSON mod. ' +
            '(Molang and step interpolation are not supported for animations.)',
        tags: ["Minecraft: Java Edition"],
        onload() {
            // TODO support importing animation definitions
            let animationMenu = Animation.prototype.menu;
            if (animationMenu != null && animationMenu.structure instanceof Array) {
                let prevIdx = animationMenu.structure.findIndex(e => e.id === 'export_animation_definition_json');
                if (prevIdx !== -1)
                    animationMenu.structure.splice(prevIdx, 1);
                let saveIdx = animationMenu.structure.findIndex(e => e.id === "save");
                animationMenu.structure.splice(saveIdx + 1, 0, {
                    id: 'export_animation_definition_json',
                    name: 'Export Animation Definition',
                    description: 'Exports this animation to a JSON animation definition for use with Entity Model JSON',
                    icon: 'fa-file-export',
                    click(animation) {
                        let contents = generateAnimationDefinition(animation);
                        Blockbench.export({
                            resource_id: 'animation_definition_json',
                            type: 'Animation Definition',
                            extensions: ['json'],
                            savetype: 'json',
                            name: animation.name.replaceAll('.', '_').replace('animation_', ''),
                            content: contents
                        });
                    }
                });
            }
        },
        onunload() {
            let animationMenu = Animation.prototype.menu;
            if (animationMenu != null && animationMenu.structure instanceof Array) {
                let removeIdx = animationMenu.structure.findIndex(e => e.id === 'export_animation_definition_json');
                animationMenu.structure.splice(removeIdx, 1);
            }
        }
    });

    // function hasArgs(easing) {
    //     return easing.includes("Back") ||
    //         easing.includes("Elastic") ||
    //         easing.includes("Bounce") ||
    //         easing === EASING_OPTIONS.step;
    // }

    function compileKeyFrame(keyFrame, targetFactory) {
        let target = keyFrame.data_points[0];
        target = {x: parseFloat(target.x), y: parseFloat(target.y), z: parseFloat(target.z)};
        let compiled = {timestamp: keyFrame.time, target: targetFactory(target)};

        // let setEasing = false;
        // if (keyFrame.easing != null && Plugins.installed.some(e => e.id === 'animation_utils')) {
        //     let easingType = keyFrame.easing;
        //     if (hasArgs(easingType)) {
        //         if (Array.isArray(keyFrame.easingArgs) && keyFrame.easingArgs.length > 0) {
        //             // MISSING
        //             setEasing = true;
        //         }
        //     } else {
        //         let compiledInterp = {};
        //         if (easingType.includes('In')) compiledInterp.easeIn = true;
        //         if (easingType.includes('Out')) compiledInterp.easeOut = true;
        //
        //         if (Object.keys(compiledInterp).length > 0) {
        //             compiledInterp.type = easingType.replaceAll('In', '').replaceAll('Out', '');
        //         } else {
        //             compiledInterp = easingType;
        //         }
        //
        //         compiled.interpolation = compiledInterp;
        //         setEasing = true;
        //     }
        // }

        if (/*!setEasing && */keyFrame.interpolation != null) {
            compiled.interpolation = keyFrame.interpolation;
        }

        return compiled;
    }

    function pushKeyFrames(channels, channelTarget, keyFrames, targetFactory) {
        if (!keyFrames.length) return

        let channel = {target: channelTarget, keyframes: []};
        channels.push(channel);

        for (const keyFrame of keyFrames) {
            channel.keyframes.push(compileKeyFrame(keyFrame, targetFactory));
        }
    }

    function generateAnimationDefinition(animation) {
        let compiled = {lengthInSeconds: animation.length, boneAnimations: {}};

        if (animation.loop === "loop") {
            compiled.looping = true;
        }

        for (const id in animation.animators) {
            const boneAnimator = animation.animators[id];
            if (!(boneAnimator instanceof BoneAnimator)) continue;

            let channels = [];

            pushKeyFrames(channels, 'position', boneAnimator.position, function (target) {
                return [target.x, -target.y, target.z];
            });
            pushKeyFrames(channels, 'rotation', boneAnimator.rotation, function (target) {
                return [Math.degToRad(target.x), Math.degToRad(target.y), Math.degToRad(target.z)];
            });
            pushKeyFrames(channels, 'scale', boneAnimator.scale, function (target) {
                return [target.x - 1.0, target.y - 1.0, target.z - 1.0];
            });

            if (channels.length) {
                compiled.boneAnimations[boneAnimator._name] = channels;
            }
        }

        return JSON.stringify(compiled, null, 2);
    }

    function flipCoords(vec) {
        return [-vec[0], -vec[1], vec[2]];
    }

    function flipY(vec) {
        return [vec[0], -vec[1], vec[2]];
    }

    function isZero(vec) {
        for (c of vec) {
            if (c != 0) {
                return false;
            }
        }
        return true;
    }

    function addBone(parent, pOrigin, key, bone) {
        let groupOptions = {name: key, children: []};

        let origin = pOrigin;
        if ("partPose" in bone) {
            let partPose = bone.partPose;
            origin = [origin[0] + (partPose.x || 0), origin[1] + (partPose.y || 0), origin[2] + (partPose.z || 0)];
            groupOptions.rotation = [
                "xRot" in partPose ? -Math.radToDeg(partPose.xRot) : 0,
                "yRot" in partPose ? -Math.radToDeg(partPose.yRot) : 0,
                "zRot" in partPose ? Math.radToDeg(partPose.zRot) : 0
            ];
        }
        groupOptions.origin = flipCoords(origin);

        let group = new Group(groupOptions);

        if (parent !== undefined) {
            group.addTo(parent);
        }

        group = group.init();

        if ("cubes" in bone) {
            for (cube of bone.cubes) {
                let cubeOptions = {name: "cube"};

                if ("comment" in cube) {
                    cubeOptions.name = cube.comment;
                }
                if ("grow" in cube) {
                    cubeOptions.inflate = cube.grow instanceof Array ? cube.grow[0] : cube.grow;
                }
                if ("mirror" in cube) {
                    cubeOptions.mirror_uv = cube.mirror;
                }

                let pos = cube.origin;
                pos = [pos[0] + origin[0], pos[1] + origin[1], pos[2] + origin[2]]
                let size = cube.dimensions;
                cubeOptions.to = flipCoords([pos[0], pos[1], pos[2] + size[2]]);
                cubeOptions.from = flipCoords([pos[0] + size[0], pos[1] + size[1], pos[2]]);

                let texCoord = cube.texCoord;
                cubeOptions.uv_offset = [texCoord.u, texCoord.v];

                new Cube(cubeOptions).addTo(group).init();
            }
        }

        for (childKey in bone.children) {
            addBone(group, origin, childKey, bone.children[childKey]);
        }
    }

    function compileCube(cube, bOrigin) {
        let compiled = {texCoord: {}};

        if (cube.name !== "cube") {
            compiled.comment = cube.name;
        }

        let to = cube.to;
        let from = cube.from;

        let size = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
        let pos = flipCoords([from[0] + size[0], from[1] + size[1], from[2]]);

        compiled.origin = [pos[0] - bOrigin[0], pos[1] - bOrigin[1], pos[2] - bOrigin[2]];
        compiled.dimensions = size;
        compiled.texCoord.u = cube.uv_offset[0];
        compiled.texCoord.v = cube.uv_offset[1];

        if (cube.mirror_uv) {
            compiled.mirror = true;
        }
        if (cube.inflate > 0) {
            compiled.grow = cube.inflate;
        }

        return compiled;
    }

    function compileBone(bone, pOrigin) {
        let compiled = {};

        let bOrigin = flipCoords(bone.origin); // Bone origin
        let origin = [bOrigin[0] - pOrigin[0], bOrigin[1] - pOrigin[1], bOrigin[2] - pOrigin[2]]; // Origin to write to json

        if (!isZero(origin)) {
            if (!("partPose" in compiled)) compiled.partPose = {};
            let partPose = compiled.partPose;
            partPose.x = origin[0];
            partPose.y = origin[1];
            partPose.z = origin[2];
        }

        let brot = bone.rotation;
        let rotation = [Math.degToRad(-brot[0]), Math.degToRad(-brot[1]), Math.degToRad(brot[2])];
        if (!isZero(rotation)) {
            if (!("partPose" in compiled)) compiled.partPose = {};
            let partPose = compiled.partPose;
            partPose.xRot = rotation[0];
            partPose.yRot = rotation[1];
            partPose.zRot = rotation[2];
        }

        let cubes = [];
        let children = {};

        for (node of bone.children) {
            if (node instanceof Group) {
                if (node.parent !== bone) {
                    continue;
                }

                children[node.name] = compileBone(node, bOrigin);
            } else if (node instanceof Cube) {
                if (node.parent !== bone) {
                    continue;
                }

                cubes.push(compileCube(node, bOrigin));
            }
        }
        if (Object.keys(children).length > 0) {
            compiled.children = children;
        }
        if (cubes.length > 0) {
            compiled.cubes = cubes;
        }

        return compiled;
    }

})();