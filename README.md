# qCraft Reimagined

A reimagination of qCraft for modern Minecraft.

## About

[qCraft](https://www.curseforge.com/minecraft/mc-mods/qcraft) is a mod that introduced many concepts of Quantum Physics into Minecraft. It was beloved by the community for its novel ideas and interesting capabilities; unfortunately, its development lay dormant for many years. **qCraft Reimagined** aims to bring the mod back into the spotlight.

## Features

- Blocks that change state depending on the face observed
- Practical redstone randomness
- Machines that will perform quantum observations automatically
- Computers that can swap entire regions of the world

...and much more! See the [wiki](https://github.com/acikek/qcraft/wiki) for in-depth feature explanations and tutorials.

## Forge 1.20.1

Install the same qCraft JAR on your Forge client and dedicated server. The build uses Java 17. Run `./gradlew build` to produce the mod in `build/libs`. Run `./gradlew runGameTestServer` for the headless gameplay tests.

With Patchouli installed on both client and server, craft the **qCraft Field Guide** with one Book and one Quantum Dust in any crafting grid. Finding Quantum Dust unlocks the recipe. You can also take the guide from the qCraft creative tab. Its book ID is `qcraft:guide` and its item is `patchouli:guide_book` tagged with `{"patchouli:book":"qcraft:guide"}`. The guide walks through qBlocks, entanglement, redstone automation, and building your first pair of Quantum Computer stations.

When moving a Reimagined world from Fabric 1.19, test with a copy of the entire world directory to retain qBlock faces, entanglement frequencies, and computer inventories. Forge may log that the old Fabric Mods data pack is absent while loading the qCraft data pack.

The Quantum Computer swaps the blocks within matching pylon frames. Both regions must be loaded and must not overlap. The initiating and receiving computers stay in place. The swap rejects any qBlocks and other block entities inside either region, including containers. The initiating computer uses one coolant charge on a successful swap.

## Credits

- This mod wouldn't have been possible without fantastic art by VirtuaLilith. I may have programmed the internal logic, but she really brought the mod to life!
- Trudle helped me with Portuguese localization; I'm not exactly fluent yet.
- Mind-boggling math problems were solved by the Quilt community, most notably supersaiyansubtlety... they saved me many headaches.
- The Field Guide's cover and interface textures adapt the Satiscraftory Patchouli book artwork.

## License

MIT © 2022 Skye Prince
