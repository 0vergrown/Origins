# Origins 1.6.0

Adds **origin storage** — a per-player key/value store that remembers an origin (or any
piece of text) under a name you choose, so you can put it back later.

Available on **Fabric 1.20.1**, **Fabric 1.21.1** and **NeoForge 1.21.1** with identical
behavior.

> Requires Apoli 1.7.0 or newer.

## Added

### Origin storage

Every player carries a small store of named entries. Entries survive death and relog, and
are copied to the fresh player on respawn. Two kinds of entry exist:

- **origins** — an origin id plus the layer it came from;
- **values** — plain text (a name, a flag, a marker).

The typical use is "take someone's origin, hand it back later": store it under a key,
switch them to something else, then apply the stored key when the effect ends.

#### Entity actions

| Action                        | What it does                                  |
|-------------------------------|-----------------------------------------------|
| `origins:store_origin`        | Stores the entity's own origin under `key`.   |
| `origins:apply_stored_origin` | Sets the entity's origin from a stored `key`. |
| `origins:store_value`         | Stores a text `value` under `key`.            |

#### Bi-entity action

| Action                 | What it does                                                                                           |
|------------------------|--------------------------------------------------------------------------------------------------------|
| `origins:store_origin` | Stores the **target's** origin into the **actor's** store. Wrap in `apoli:invert` to go the other way. |

#### Entity conditions

| Condition               | What it checks                                                                                           |
|-------------------------|----------------------------------------------------------------------------------------------------------|
| `origins:stored_origin` | An origin is stored under `key` (optionally matching `origin` / `layer`). Omit `key` to match any entry. |
| `origins:stored_value`  | A value is stored under `key` (optionally equal to `value`).                                             |

#### Example — remember your own origin, then take a temporary one

```json
{
  "type":"apoli:and",
  "actions":[
    {
      "type":"origins:store_origin",
      "key":"before_curse"
    },
    {
      "type":"apoli:execute_command",
      "command":"origin set @s origins:origin origins:phantom"
    }
  ]
}
```

Undo it later with:

```json
{
  "type":"origins:apply_stored_origin",
  "key":"before_curse",
  "clear":true
}
```

#### Example — copy the entity you hit into your own store

```json
{
  "type":"apoli:and",
  "actions":[
    {
      "type":"origins:store_origin",
      "key":"stolen"
    },
    {
      "type":"apoli:target_action",
      "action":{
        "type":"apoli:play_sound",
        "sound":"entity.evoker.cast_spell"
      }
    }
  ]
}
```

Used as a `bientity_action`, `origins:store_origin` reads the **target's** origin and writes it into the **actor's** store.

#### Commands

```
/origin storage list <target>
/origin storage get <target> <key>
/origin storage store origin <target> <key> <source> [<layer>]
/origin storage store value <target> <key> <value…>
/origin storage apply <target> <key> [<layer>]
/origin storage clear <target> [<key>]
/origin storage run <target> <command…>
```

`store value` and `run` expand `[key]` placeholders against the target's store, and `[key.name]` expands to the stored origin's display name. A command whose placeholders cannot all be resolved is not run. Permission node `origins.command.origin.storage` (level 2).

## Notes

- Storage is server-side. Conditions that read it are evaluated on the server, so it is meant for gameplay logic rather than client-only rendering decisions.
- `apply_stored_origin` goes through the normal origin-change path — powers are diffed, not removed and re-added, and gated layers are revalidated afterwards.
- No existing type ids, fields or defaults changed. Data packs written for 1.5.x load unchanged.
