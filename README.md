TrueHardcore
============

Multiplayer hardcore plugin that simulates a true one-life hardcore experience.

This is an admin/operator guide. For developer context see `CLAUDE.md`.

---

## What it does

Players opt in to a dedicated hardcore world with `/th play`. They are dropped at a
randomised far-away spawn and play near-vanilla survival across the Overworld, Nether
and End of that world. **Death is permanent for that life:**

- Their world changes are rolled back (via Prism).
- They are banned from re-entering for the configured ban time (default 12h).
- They are ejected to the lobby; their build, items and progress are gone.

Because a death wipes everything a player built, **death and respawn issues are treated
as high severity**. Worlds are left in debug mode permanently so these can be diagnosed.

---

## Deployment overview

Each hardcore world runs on its **own server**, each with **one** hardcore world and its
**own config** (intentionally split so each world can be tuned independently):

- **HC1 / HC2** — main public near-vanilla worlds.
- **HC3** — experimental seasonal world for special events (non-vanilla, rotating plugins).
- **HC4** — not public; used for testing.

A server's hardcore world spans three dimensions (`<world>`, `<world>_nether`,
`<world>_the_end`) which are all treated as the same hardcore game. The lobby is a
separate world (`hclobby`).

---

## Commands

All commands are under `/th` (alias `/truehardcore`).

### Player commands

| Command | Description |
| --- | --- |
| `/th play` | Start a new life, or resume your saved life |
| `/th leave` | Exit hardcore (progress saved). 5s warm-up; cancelled if you move, are in combat, or monsters are nearby |
| `/th info [player]` | Show game info (state, score, deaths, game time, top score) |
| `/th stats [player]` | Show kill statistics |
| `/th list` (or `who`) | List players currently in hardcore worlds |

### Admin commands

| Command | Description |
| --- | --- |
| `/th dump [player]` | Dump a player's full record (or a summary of all) |
| `/th dumpworlds` | Dump world settings and currently held chunks |
| `/th set exit` | Set the lobby/exit position to your current location |
| `/th whitelist add <player>` | Add a player to the TrueHardcore whitelist |
| `/th load <player>` | Reload a player's record from the database |
| `/th forcealive <player>` | Force a player out of "in game" state (recovery for stuck players). Player must be in the lobby, not the HC world |
| `/th reducetime <player> <hours>` | Reduce a dead player's remaining ban wait (player must be online) |
| `/th account <player> [primary\|alt]` | View or set a player's account type |
| `/th tp <player> <player>` | Teleport one player to another |
| `/th save` | Save all buffered player data to the database |
| `/th bcast <msg>` | Broadcast a message to all hardcore players |
| `/th queue` | Show the Prism rollback queue and lock status |
| `/th enable` / `/th disable` | Enable/disable new games (until restart) |
| `/th debug` | Toggle debug logging (until restart) |

> Note: `/th whitelist del` and `list` are not yet implemented.

---

## Permissions

| Permission | Grants |
| --- | --- |
| `truehardcore.*` | Full admin access (default: ops) |
| `truehardcore.use` | Play hardcore (`/th play`) |
| `truehardcore.info` / `.info.other` | View own / others' info |
| `truehardcore.stats` / `.stats.other` | View own / others' stats |
| `truehardcore.list` | Use `/th list` |
| `truehardcore.admin` | All admin commands |
| `truehardcore.endteleport` | Allowed to use the End portal (otherwise blocked) |
| `truehardcore.bypass.teleport` | Bypass the no-teleport rule within a hardcore world |
| `truehardcore.bypass.teleportin` | Bypass the block on teleporting **into** a hardcore world |
| `truehardcore.bypass.teleportout` | Bypass the block on teleporting **out of** a hardcore world |

Ops also bypass the teleport restrictions automatically.

---

## Setup & management

**Prerequisites (managed outside this plugin):**

- Worlds (`<world>`, `_nether`, `_the_end`, `hclobby`) are created and managed by
  **MyWorlds** — TrueHardcore only looks them up, it does not create them.
- A **WorldBorder** should be set on the hardcore world. Spawn-finding and portal
  destinations are both constrained to inside the border; border size varies per world.
- A reachable **MySQL** database. Account/whitelist data lives in the shared
  `truehardcore_global` database; the player table is keyed by world so multiple servers
  can coexist.
- **Prism** (required for rollback) plus optional integrations: Vault (economy rewards),
  LWC (lock cleanup on death), PremiumVanish, PlayerParticles, ProtocolLib (hardcore
  hearts display), PlaceholderAPI.

**Key config (`config.yml`) settings to be aware of:**

- `world` — the hardcore world name for this server.
- `lobbyWorld` / `exitPos` — where players are sent on death/leave.
- `banTime` — wait time after death before a player can start a new life.
- `spawnDistance` — how far out new spawns are searched for.
- `spawnProtection` — invincibility window on a new life.
- `whitelisted` — whether the world requires the TrueHardcore whitelist.
- `rollbackdelay` — grace period after death before rollback runs (lets others loot first).
- `chunkHoldOnDeath` — how long the death-site chunk stays loaded for looting.
- `difficulty` — world difficulty (default HARD).
- `deathcommand` / `dragonkillcommand` / `dragonrespawncommand` / `newlifecommand` —
  console commands run on those events (support placeholders and `<player>`, `<world>`,
  `<score>`, `<cause>`).

---

## Troubleshooting

- **Worlds run in debug mode permanently** — detailed logs (especially around death,
  respawn and teleports) are written to the plugin's debug log. Leave debug on.
- **Player stuck in/out of the world:** ask them to return to the lobby, then use
  `/th forcealive <player>`. As a last resort, a relog reconciles their state on join.
- **World not fully restored after a death:** Prism only rolls back **player-triggered**
  changes. Environmental knock-on effects (e.g. leaf decay after a player fells a tree)
  are **not** rolled back — this is a known Prism limitation, not a bug. Use `/th queue`
  to confirm the rollback was queued/processed.
- **Player can't start a new life:** check their account type (`/th account`), whitelist
  status, and remaining ban time (`/th info` / `/th dump`). Use `/th reducetime` to
  shorten a ban if appropriate.
- **Alt-account false positive:** detection is IP-based. Use `/th account <player> primary`
  to clear an incorrect "alt" flag.

---

## Roadmap / wishlist

Ideas we'd like TrueHardcore to own itself, so they don't have to be maintained across
external plugins and kept in sync across the three dimensions. Not yet implemented.

1. **In-game command blocking.** Currently we rely on WorldGuard region "deny-commands"
   to block commands that would feel like cheating. TH already knows when a player is
   *in game* in a TH world (vs the lobby), so it could block a configurable command
   list only while in game — removing the need to sync WorldGuard regions across the
   Overworld/Nether/End and manage the include/exclude list there.

2. **Built-in permission/action restrictions.** We maintain a complex LuckPerms structure
   to keep things fair and vanilla-like (mostly blocking elevated staff/admin privileges,
   plus some player actions). TH could automatically enforce a core set of these while a
   player is in game. Requires clearly identifying and documenting exactly which
   actions/permissions it takes over, so it doesn't silently conflict with LuckPerms.

3. **Take over portal/dimension travel.** Nether/End/exit portal routing is fragile and
   sometimes misfires (sends players to `hclobby`, or fails entirely). It currently
   depends on careful MyWorlds configuration to cover every case. TH already gates and
   validates portal use, so it could fully own dimension travel for TH worlds to
   guarantee correct, consistent routing without relying on MyWorlds portal config.

   - **Fix the End exit portal return (currently broken).** TH already *tries* to send
     End-exit-portal users back to their per-life spawn instead of the Overworld world
     spawn (0,0) — but in practice players still land at 0,0. The TH logic appears to be
     losing a fight with MyWorlds: either MyWorlds overrides the respawn destination last
     (event priority/load-order), or it intercepts the travel as a portal event so TH's
     respawn handler is never the deciding factor. (The existing "portal teleport cause
     is UNKNOWN" workaround in that path is a sign it's already unreliable.) The fix
     belongs with TH fully owning this routing rather than competing with MyWorlds.
     0,0 is wrong regardless — each life uses a pseudo-random safe spawn — so the target
     should be the player's original per-life spawn, or a fresh safe RTP.
