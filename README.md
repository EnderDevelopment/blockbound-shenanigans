# Minecraft Plugin: Jujutsu Shenanigans Blockbound

Jujutsu Shenanigans: Blockbound is a Paper Minecraft plugin that provides fast-paced arena combat with combo attacks, kit-based abilities, dashes, knockback, particles, cooldowns, ultimate charge, and an area-of-effect ultimate system. It is an original Minecraft arena combat implementation inspired by ability-focused fighting games and does not include the original Roblox map or assets.

## Install

1. Install Java 21.
2. Install a Paper Minecraft server compatible with Paper 1.21.11.
3. Build the plugin with Maven:

```bash
mvn package
```

4. Copy the generated `target/plugin.jar` file into the server's `plugins` directory.
5. Start or restart the Paper server.
6. Confirm that the plugin loads successfully in the server console.

## Use

1. Join the combat system:

```text
/arena join
```

2. Select a kit:

```text
/arena kit ember
/arena kit tide
/arena kit stone
```

3. View the available commands and controls:

```text
/arena help
```

4. Use the combat controls while participating:

- Left-click to perform a four-step combo attack.
- Right-click to activate the ability assigned to the selected hotbar slot.
- Use hotbar slots to select different ability types.
- Sneak to dash forward.
- Swap the items in your hands to activate the ultimate when the charge reaches 100%.
- Use `/arena leave` to leave the combat system.

Available kits are Ember, Tide, and Stone. Each kit changes ability damage or visual particle effects. Abilities have cooldowns, attacks generate ultimate charge, and ultimate attacks damage and knock back nearby living entities.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/1984fc74-d402-4d8c-97c5-aa9c4d028570?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=1984fc74-d402-4d8c-97c5-aa9c4d028570
