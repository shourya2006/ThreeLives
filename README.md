# ThreeLives

Three lives. Every death matters. Give your survival server a limited-lives challenge with visible heart indicators, tradable Life Items, and craftable Revive Books.

![One, two and three remaining lives](https://cdn.modrinth.com/data/uTewbEqq/images/c9a54845f1bb0e0ee53a0a8b0306139c37b7b248_350.webp)

## Features

- **Three lives by default.** Death removes one life, with player life counts saved between server restarts.
- **Hearts beside player names.** The included resource pack displays filled and empty heart icons.
- **Life Items.** Withdraw spare lives with `/withdraw [amount]`, trade the resulting item, and right-click to redeem one life. Players must keep at least one life when withdrawing.
- **Revive Books.** Craft one book with three genuine Life Items in a crafting table, then right-click the resulting Revive Book and select an eliminated player. Revival restores the configured starting lives and consumes one book.
- **Elimination options.** Choose a protected death box, a server ban, or no automatic elimination action. Set the death-box location before playing with that mode. Revive Books can restore offline players eliminated by this plugin's ban mode.
- **Server configuration.** Configure starting/maximum lives, death drops, item withdrawal, revival recipes, messages and optional elimination commands.
- **Administrative controls.** Set, add, remove or reset online players' lives and reload the configuration.

## Requirements and compatibility

- Minecraft Java Edition **1.21.1** or **1.21.11**, using the matching **Paper** server version.
- **Java 21**.
- The server plugin is installed on the server; players do not need a client mod.
- Install the bundled **ThreeLives Resource Pack** matching Minecraft 1.21.1 or 1.21.11 on each client to see the custom heart icons and Life Item appearance. The pack is not automatically downloaded by this plugin.

This release is tested on Paper 1.21.1 build 133 and Paper 1.21.11 build 132. Compatibility with other Minecraft versions, other server platforms and Folia is not claimed.

## Downloads

| Download | Install on | Minecraft |
|---|---|---|
| [ThreeLives-1.0.1.jar](https://cdn.modrinth.com/data/uTewbEqq/versions/koXqTzJH/ThreeLives-1.0.1.jar) | Paper server | 1.21.1 or 1.21.11 |
| [ThreeLives-ResourcePack-1.21.1.zip](https://cdn.modrinth.com/data/uTewbEqq/versions/koXqTzJH/ThreeLives-ResourcePack-1.21.1.zip) | Minecraft client | 1.21.1 |
| [ThreeLives-ResourcePack-1.21.11.zip](https://cdn.modrinth.com/data/uTewbEqq/versions/koXqTzJH/ThreeLives-ResourcePack-1.21.11.zip) | Minecraft client | 1.21.11 |

Use one resource pack matching your exact Minecraft version. The packs provide visual assets; the JAR provides gameplay on the server.

## Installation

1. Stop your Paper server and place `ThreeLives-1.0.1.jar` in its `plugins` folder.
2. Start the server to generate `plugins/ThreeLives/config.yml`.
3. On the release page, open the additional downloads and get `ThreeLives-ResourcePack-1.21.1.zip` or `ThreeLives-ResourcePack-1.21.11.zip` for your exact Minecraft version. Put it in each client's `resourcepacks` folder and enable it. Install only the matching pack.
4. In the default death-box mode, choose a safe location and run `/setdeathbox` as an administrator. This generates a **5 × 5 × 5** box, replacing blocks at that location.
5. Adjust the configuration and use `/threelives reload`, or restart the server.

For public servers, keep normal Minecraft authentication enabled. The offline loopback server used during development is a local test environment.

## Commands

| Command | Purpose | Default access |
|---|---|---|
| `/lives [player]` | Check your own or an online player's lives | Everyone |
| `/withdraw [amount]` | Convert spare lives into Life Items | Everyone |
| `/setlives <player> <amount>` | Set an online player's lives | Operator |
| `/addlives <player> <amount>` | Add lives, capped at the configured maximum | Operator |
| `/removelives <player> <amount>` | Remove lives, applying elimination at zero | Operator |
| `/resetlives <player>` | Restore the configured starting lives | Operator |
| `/setdeathbox` | Generate and select the global death box | Operator |
| `/threelives reload` | Reload settings, data and the revival recipe | Operator |

`/tl` is an alias for `/threelives`. The `threelives.bypass` permission prevents life loss on death and is **not granted by default**, including to operators.

## Playing with revival

The default crafting recipe is shapeless: put **one Book** and **three genuine Life Items in separate crafting slots**. Ordinary Nether Stars do not work. A living player must hold a Revive Book when selecting the revival target. Eliminated players cannot use one themselves.

The revival menu supports multiple pages. A revived player receives the configured starting lives (three by default). Reviving a player does not remove bans imposed by unrelated plugins or administrators.

## Inspiration

Inspired by the heart indicators visible in the opening of [Nufuli's “I Became A Villain To Avenge My Rival”](https://www.youtube.com/watch?v=0ao4KSXmu2k). This is an independent project and is not affiliated with Nufuli or Wisp SMP. The opening alone does not establish the original server's full rules or plugin implementation.

## License

MIT. The plugin and included ThreeLives resource-pack files are distributed under the MIT License.

## Build from source

Install JDK 21 and run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The resulting plugin is in `build/libs/`. Source code for release 1.0.1 is included here; the published JAR remains the exact artifact validated in the test reports. Resource-pack source files are in `resource-packs/1.21.1/` and `resource-packs/1.21.11/`.

## Validation

Release 1.0.1 passed 51 named Mineflayer integration checks: 28 on Paper 1.21.1 and 23 on Paper 1.21.11. They covered life counts, withdrawal/redemption, revival and recipe validation, menu misuse, death-box confinement, persistence, and ban-mode revival. Automated server checks do not establish rendering in every client or compatibility with untested versions.

## Development disclosure

The existing ThreeLives plugin received AI-assisted code fixes and validation work using Codex. The project description and release text were also prepared with AI assistance. Existing resource-pack artwork was reused for the icon; the gallery image is a capture of the actual Minecraft client. The plugin does not use generative AI at runtime.

## Support

Report bugs at https://github.com/shourya2006/ThreeLives/issues. Include your Minecraft and Paper versions, plugin version, relevant configuration, and steps to reproduce. Remove secrets and private player information from logs before posting.
