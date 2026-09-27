# QuickStack

Terraria style inventory management for Minecraft 26.2:

- **Quick stack to nearby chests** moves items from your inventory into nearby
  containers that already hold the same kind of item. Durability, enchantments and names
  do not matter, so a worn diamond pickaxe joins the other diamond pickaxes.
- **Restock from nearby chests** tops up the stacks you carry from nearby containers.
- **Locked items** are never quick stacked, so your tools and food stay with you.
- The **hotbar** only takes part if you want it to.

It comes in two parts that need each other:

| Part | Folder | Platform |
| --- | --- | --- |
| Client mod | [client/](client/) | Fabric (needs Fabric API) |
| Server plugin | [server/](server/) | Paper |

The server does the searching and moving, the client only says which of its slots may
be touched. The mod does nothing on servers without the plugin, and it does not work in
singleplayer.

## Installing

Download both jars from the releases page.

- **Client:** put `quickstack-client-*.jar` into `mods/` together with
  [Fabric API](https://modrinth.com/mod/fabric-api). Mod Menu and Cloth Config are
  optional and add a settings screen.
- **Server:** put `quickstack-server-*.jar` into `plugins/`.

## Using it

Open your inventory or any container. Two buttons sit to the right of your inventory:

| Button | Action |
| --- | --- |
| Chest with green arrow | Quick stack to nearby chests |
| Chest with orange arrow | Restock from nearby chests |

Hold **Left Alt** and click an item in your inventory to lock or unlock it. Locked items
show a small padlock.

The lock belongs to the item. It follows the item when you move it to another slot, swap
it or merge it into another stack, and it ends when the item leaves your inventory.
Locks are remembered per server.

Both actions can also be bound to keys under *Options > Controls > Key Binds > QuickStack*.
They are unbound by default. The key to hold for locking can be changed there as well.

## Client settings

Change them in Mod Menu, or in `config/quickstack.json`.

| Setting | Default | Meaning |
| --- | --- | --- |
| `quickStackHotbar` | `false` | Quick stack items in the hotbar too |
| `restockHotbar` | `true` | Top up stacks in the hotbar when restocking |
| `restockLockedSlots` | `true` | Top up locked items when restocking |
| `showButtons` | `true` | Show the two buttons next to the inventory |
| `showMessages` | `true` | Show how many items were moved |
| `lockCorner` | `TOP_RIGHT` | Corner of the slot that shows the padlock: `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT` or `BOTTOM_RIGHT`. Move it if another mod marks slots in the same corner |
| `locks` | `{}` | Locked items per server, kept up to date by the mod |

## Server settings

`plugins/QuickStack/config.yml`, applied with `/quickstack reload`.

| Setting | Default | Meaning |
| --- | --- | --- |
| `quick-stack` | `true` | Allow quick stacking |
| `restock` | `true` | Allow restocking |
| `match-item-type` | `true` | Quick stack by kind of item. Turn off to require identical items, including damage and enchantments |
| `radius` | `8` | Search distance in blocks, up to 32 |
| `max-containers` | `64` | Most containers used per request, nearest first |
| `cooldown-ms` | `500` | Minimum time between two requests of a player |
| `containers.chests` | `true` | Use chests, trapped chests and copper chests |
| `containers.barrels` | `true` | Use barrels |
| `containers.shulker-boxes` | `true` | Use placed shulker boxes |
| `skip-blocked-chests` | `true` | Skip chests that cannot be opened by hand |
| `check-protection` | `true` | Ask protection plugins before using a container |
| `effects.sound` | `true` | Play a sound for the player |
| `effects.particles` | `true` | Show particles at the containers that were used |

| Permission | Default | Allows |
| --- | --- | --- |
| `quickstack.quickstack` | everyone | Quick stacking |
| `quickstack.restock` | everyone | Restocking |
| `quickstack.reload` | operators | `/quickstack reload` |

Containers that are locked with a key, or that hold loot nobody has generated yet, are
always skipped. Only loaded chunks are searched.

With `check-protection` on, the plugin fires a right click event for each container it is
about to use and skips the container if another plugin denies it. That is how claim and
lock plugins get their say. Plugins that open a menu on right click may react to it too.

## Protocol

Plugin messages in the play phase. Numbers are big endian. The current version is `1`.

| Channel | Direction | Payload |
| --- | --- | --- |
| `quickstack:config` | server to client | `int version`, `boolean quickStack`, `boolean restock`, `int radius` |
| `quickstack:request` | client to server | `byte version`, `byte action`, `long slotMask` |
| `quickstack:result` | server to client | `byte action`, `byte status`, `int items`, `int containers` |

- The client registers `quickstack:config`. The server answers that registration with
  the config, which tells the client that the plugin is there and what this player may do.
  It is sent again after `/quickstack reload`.
- `action` is `0` for quick stack and `1` for restock.
- Bit *n* of `slotMask` allows the server to touch player inventory slot *n* (0-35).
  Locked items and the hotbar setting are applied by leaving bits out.
- `status` is `0` ok, `1` cooldown, `2` denied, `3` disabled, `4` version mismatch.

## Building

Needs JDK 25.

```sh
./gradlew build
```

The jars end up in `client/build/libs/` and `server/build/libs/`. Versions of Minecraft,
Fabric and Paper are set in [gradle.properties](gradle.properties).

## Releasing

The client and the server are versioned and released separately. Raise `client_version`
or `server_version` in [gradle.properties](gradle.properties) and push to `main`.

The [workflow](.github/workflows/build.yml) then looks for the tag of each version,
`client-v1.2.0` or `server-v1.2.0`. If the tag does not exist yet, it creates the tag and
a release with the jar of that part attached. Versions that were released before are
left alone.

Raise the protocol version in both `Protocol` classes whenever the payloads change, so
that mismatched releases tell the player instead of misbehaving.

## Vibe Coding Disclosure

I vibe coded this mod for a small survival server I run lol.
