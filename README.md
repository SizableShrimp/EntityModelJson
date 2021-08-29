# Entity Model JSON
This is a Minecraft mod for Forge 1.17 which allows declaring entity models in JSON.
It can be used to create powerful entity models both in mods and modpacks.

For mods, entity models can be declared in JSON and shipped as assets in the final jar, with a required dependency on this mod.
For modpack makers, vanilla and modded entity models can be overriden in JSON using this system.

## Resources
For resources on how to declare and use JSON entity models, please see the following:
* [All vanilla entity models exported as JSON](vanilla_layers) - This resource allows you to see how all vanilla entity models look in a JSON format.
They can be used as a starting skeleton, or directly copied to override vanilla entity models with a custom variant.
* [An example Forge mod using JSON entity models](https://github.com/SizableShrimp/EntityModelJsonExample)
* [The complete JSON spec](docs/SPEC.json) - This JSON file documents all the properties you can possibly use.

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
Then, add the dependency to your dependencies block:
```groovy
dependencies {
    implementation fg.deobf("me.sizableshrimp:entitymodeljson:${minecraft_version}-1.0.0")
}
```
For a complete example, see [here](https://github.com/SizableShrimp/EntityModelJsonExample/blob/1.17.x/build.gradle).