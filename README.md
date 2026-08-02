# Origins

**Origins** lets every player pick a species when they join the world — each with its own strengths, weaknesses, and playstyle. Choose to be a spider-climbing Arachnid, a Nether-immune Blazeborn, a water-bound Merling, or one of several others, each built entirely from [Apoli](https://github.com/0vergrown/Apoli) powers and data.

> **This branch:** `Fabric-1.20.1` — Fabric, Minecraft 1.20.1. Requires [Apoli](https://github.com/0vergrown/Apoli).

## Branches

This repository holds one branch per Minecraft version / mod loader combination:

| Branch                                                                         | Loader   | Minecraft |
|--------------------------------------------------------------------------------|----------|-----------|
| [`Fabric-1.20.1`](https://github.com/0vergrown/Origins/tree/Fabric-1.20.1)     | Fabric   | 1.20.1    |
| [`Fabric-1.21.1`](https://github.com/0vergrown/Origins/tree/Fabric-1.21.1)     | Fabric   | 1.21.1    |
| [`NeoForge-1.21.1`](https://github.com/0vergrown/Origins/tree/NeoForge-1.21.1) | NeoForge | 1.21.1    |

## Origins

10 origins ship by default, each defined as ordinary Apoli data and easy to retheme, extend, or replace with a data pack:

| Origin        | Flavor                                                                             |
|---------------|------------------------------------------------------------------------------------|
| **Human**     | A regular human. Your ordinary Minecraft experience.                               |
| **Merling**   | A natural inhabitant of the ocean, not built for long stretches on land.           |
| **Arachnid**  | Climbs walls and traps foes in webbing - a perfect hunter.                         |
| **Blazeborn** | A late descendant of the Blaze, naturally immune to the perils of the Nether.      |
| **Avian**     | Lost the ability to fly long ago, but still glides gracefully from place to place. |
| **Phantom**   | Half-human, half-phantom - can switch between forms.                               |
| **Feline**    | Cat-like grace: always lands on its feet, and creepers steer clear.                |
| **Elytrian**  | At home flying in open sky; uncomfortable in tight, low spaces.                    |
| **Enderian**  | Born of the Ender Dragon - can teleport, but takes damage from water.              |
| **Shulk**     | Related to the Shulker, protected by a shell-like skin.                            |

## Features

- **Origin layers**: Group origins into independently-chosen layers (e.g. a "species" layer and a separate "class" layer), each with its own selection rules, merge/replace behavior for data packs, and gating conditions.
- **Choose-origin screen** with full seasonal re-theming: The GUI's art changes with the in-game calendar date out of the box.
- **Badges**: Small icons on a power's tooltip that call out extra behavior (a bound keybind, a crafting recipe unlock, custom notes), so a power's tooltip alone tells the full story.
- **Orb of Origin**: A craftable/obtainable item that lets a player re-roll their origin on demand.
- **Built-in randomizer**: Auto re-roll a player's origin on death, on sleep, or on command, with an optional "lives" counter before the randomizer kicks in. No separate add-on needed.
- **Origin transfer**: Steal, give, or copy an origin between players entirely from a data pack or command.
- Multiplayer-safe: Origin choices sync correctly to every client on join, including mid-session joins.
- `/origin` commands to get, set, check, and randomize origins and layers, and to open the choose-origin GUI on demand.

## License

The A/O License — see [`LICENSE`](LICENSE). The canonical, always-current text lives on the Handbook: <https://0vergrown.github.io/Handbook/license>.
