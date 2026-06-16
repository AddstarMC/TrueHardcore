# TrueHardcore

Private AddstarMC plugin. Overlays one-life permadeath survival on a dedicated world. Players opt in with `/th play`, get a randomised far spawn, play near-vanilla; **death is permanent** — Prism rolls back their world changes, they're banned for `banTime`, ejected to the lobby. Death = loss of everything built, so death/respawn bugs are high-severity.

## Deployment
- Each HC server is a **separate Paper instance** running this plugin with **one** hardcore world. Players move via proxy. Code only ever registers a single world per instance.
  - **HC1 / HC2** — the main public near-vanilla worlds.
  - **HC3** — experimental seasonal world for special events; very non-vanilla, used to try out fun plugins.
  - **HC4** — not public; purpose undecided, often used for testing new things.
- World naming convention: `hardcoreX` (primary) + `hardcoreX_nether` + `hardcoreX_the_end` + `hclobby` lobby. The `_nether`/`_the_end` suffixes are stripped in `HardcoreWorlds`/`HardcorePlayers` keys, so all 3 dimensions share one player record and ruleset. (`config.yml` ships `hardcore` as a placeholder.)
- **Worlds are created/managed by MyWorlds, not this plugin** — ConfigManager only looks them up. WorldBorder plugin sets per-world borders (size varies per instance); TH integrates with it for spawn-finding and portal-destination limits.
- DB: shared **`truehardcore_global`** across servers for accounts/whitelist; player table is partitioned by the `world` column.

## Workflow
- Build via private Jenkins; deploy/test via our **mcadmin** system. Dev-like env — latest builds run live; sometimes an inactive server is used for testing larger changes, but small changes are rolled straight out.
- Worlds run **permanently in debug mode** (`debugEnabled`) — verbose logging is intentional and the primary diagnostic, especially for death issues. Don't disable it.
- Commit straight to `master`. Small team, summary-line commits, PRs not usually used.

## Conventions
- **HC1/HC2 must stay near-vanilla.** Each server has its own config (intentionally split for full per-world control), so new non-vanilla mechanics are kept out of the vanilla worlds via per-server config rather than in-code world branching. Keep new mechanics config-gated, not hardcoded to a world.
- **Pinned dependency versions are deliberate — never bump without explicit user direction.** Prism is our own **v3 fork** (`network.darkhelmet.prism`); likely moving to official v4 later. Monolith/Pandora are internal libs.

## Known fragile areas
- **Rollback is the biggest pain point.** Prism only records player-triggered changes, so environmental knock-on effects aren't rolled back (e.g. player breaks a tree → logs roll back but leaf-decay doesn't). Known/accepted Prism limitation, occasionally exploited. Check this first for "world not restored" reports.
- Death / respawn / gamemode desync, and players stuck in/out of the world, are the historically unstable paths — verify these carefully on any change touching `doPlayerDeath`, `onPlayerRespawn`, `onPlayerJoin`, portal/teleport gating.
