# Origins

**Origins** lets players choose a species when they join a world. Each origin comes with its own strengths, weaknesses, and playstyle. You can play as a wall-climbing Arachnid, a Nether-immune Blazeborn, an aquatic Merling, or one of several other species. Every origin is built entirely from [Apoli](https://github.com/0vergrown/Apoli) powers and data.

> **This branch:** `NeoForge-1.21.1` - NeoForge, Minecraft 1.21.1. Requires [Apoli](https://github.com/0vergrown/Apoli).

## Branches

Each Minecraft version and mod loader combination has its own branch:

| Branch                                                                         | Loader   | Minecraft |
|--------------------------------------------------------------------------------|----------|-----------|
| [`Fabric-1.20.1`](https://github.com/0vergrown/Origins/tree/Fabric-1.20.1)     | Fabric   | 1.20.1    |
| [`Fabric-1.21.1`](https://github.com/0vergrown/Origins/tree/Fabric-1.21.1)     | Fabric   | 1.21.1    |
| [`NeoForge-1.21.1`](https://github.com/0vergrown/Origins/tree/NeoForge-1.21.1) | NeoForge | 1.21.1    |

## Origins

Origins includes 10 species by default. Each one is defined through Apoli data, so data packs can easily change their theme, add to them, or replace them entirely.

| Origin        | Flavor                                                                             |
|---------------|------------------------------------------------------------------------------------|
| **Human**     | A regular human. Your ordinary Minecraft experience.                               |
| **Merling**   | A natural inhabitant of the ocean, not built for long stretches on land.           |
| **Arachnid**  | Climbs walls and traps foes in webbing, making it a natural hunter.                |
| **Blazeborn** | A descendant of the Blaze, naturally immune to the dangers of the Nether.          |
| **Avian**     | Lost the ability to fly long ago, but can still glide gracefully from place to place. |
| **Phantom**   | Half-human, half-phantom. Can switch between forms.                                |
| **Feline**    | Cat-like grace: always lands on its feet, and creepers steer clear.                |
| **Elytrian**  | At home flying through open skies, but uncomfortable in tight, low spaces.         |
| **Enderian**  | Born of the Ender Dragon. Can teleport, but takes damage from water.               |
| **Shulk**     | Related to the Shulker, with shell-like skin for protection.                       |

## Features

- **Origin layers**: Organize origins into separate layers that players choose independently. For example, you could have a species layer and a class layer. Each layer has its own selection rules, data pack merge and replacement behavior, and conditions for when it can be chosen.
- **Choose-origin screen**: The selection GUI changes its artwork based on the in-game calendar date, with seasonal themes included by default.
- **Badges**: Small icons displayed on power tooltips to indicate things like a bound keybind, a crafting recipe unlock, or additional notes.
- **Orb of Origin**: A craftable or obtainable item that lets players change their origin whenever they want.
- **Built-in randomizer**: Automatically randomize a player's origin on death, after sleeping, or through a command. You can also set a lives counter that determines how many lives a player gets before randomization starts. No separate add-on required.
- **Origin transfer**: Steal, give, or copy origins between players using data packs or commands.
- **Multiplayer support**: Origin choices sync to all clients when a player joins, including players who connect after a session has already started.
- **`/origin` commands**: Get, set, check, and randomize origins and layers, or open the choose-origin GUI directly.

## License

The A/O License. See [`LICENSE`](LICENSE) for the repository copy. The canonical, always-current version is available on the [Handbook](https://0vergrown.github.io/Handbook/license).