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

(function() {

    const CODEC = new Codec("javaentitymodeljson_entity", {
        name: "Java Entity Model JSON",
        load_filter: {
            extensions: ["json"],
            type: "json",
            condition: function(json) {
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
        codec: CODEC,
        onActivation() {
            MenuBar.addAction(CODEC.export_action, "file.export")
        },
        onDeactivation() {
            CODEC.export_action.delete();
        }
    })

    Plugin.register("javaentitymodeljson_models", {
        title: "Java Entity Model JSON Support",
        author: "SizableShrimp",
        description: "Create models to be used with https://github.com/SizableShrimp/EntityModelJson",
        icon: "icon-format_java",
        version: "1.0",
        variant: "both"
    })

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
            origin = [origin[0] + partPose.x, origin[1] + partPose.y, origin[2] + partPose.z];
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
                if ("dilation" in cube) {
                    cubeOptions.inflate = cube.dilation instanceof Array ? cube.dilation[0] : cube.dilation;
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