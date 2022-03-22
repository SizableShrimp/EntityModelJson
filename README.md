# Entity Model JSON
This is a Minecraft mod for Forge which allows declaring entity models in JSON.
It can be used to create powerful entity models both in mods and modpacks.

For mods, entity models can be declared in JSON and shipped as assets in the final jar, with a required dependency on this mod.
For modpack makers, vanilla and modded entity models can be overriden in JSON using this system.

## Resources
For resources on how to declare and use JSON entity models, please see the following:
* [All vanilla entity models exported as JSON](vanilla_layers) - This resource allows you to see how all vanilla entity models look in a JSON format.
They can be used as a starting skeleton, or directly copied to override vanilla entity models with a custom variant.
* [The complete JSON spec](docs/SPEC.json5) - This JSON file documents all the properties you can possibly use.
* [The complete JSON schema](docs/SCHEMA.json) - This has the complete JSON Schema of entity models. You can use this for hints in your editor of choice when making a JSON entity model.
* [An example Forge mod using JSON entity models](src/test) - This example mod adds a dummy entity to the game that has an armor stand model defined in JSON, but uses the squid texture.
It also uses a data generator to modify the cat's main model. 
Notable places to check out are:
  * [Example entity renderer](src/test/java/me/sizableshrimp/entitymodeljsonexample/ExampleAnimalRenderer.java) - Demonstrates the model with its `ModelLayerLocation` and how to determine what file it points to
  * [Example entity model datagen provider](src/test/java/me/sizableshrimp/entitymodeljsonexample/ExampleEntityModelProvider.java) - Demonstrates how to generate JSON entity models through data generation
  * [Example entity model datagen output file](src/test_generated/resources/assets/minecraft/models/entity/main/cat.json) - Output file from example entity model datagen provider
  * [Entity animal entity model JSON file](src/test/resources/assets/entitymodeljsonexample/models/entity/main/example_animal.json) - Contains the armor stand entity model defined in JSON
  * [Entity Model Json dependency in mods.toml](src/test/resources/META-INF/mods.toml#L51-L57) - Demonstrates how to declare a required clientside dependency on the Entity Model JSON mod

## Declaring a mod dependency
To add this mod as a dependency, first add this to your repositories block:
```groovy
repositories {
    // Entity Model JSON maven
    maven {
        url = 'https://sizableshrimp.me/maven'
    }
}
```
Then, add the dependency to your dependencies block. For the latest version, see [here](https://github.com/SizableShrimp/EntityModelJson/releases).
```groovy
dependencies {
    implementation fg.deobf("me.sizableshrimp:entitymodeljson:${minecraft_version}-${entitymodeljson_version}")
}
```