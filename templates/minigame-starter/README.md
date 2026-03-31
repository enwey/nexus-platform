# Mini Game Starter (Nexus Platform)

This starter is the recommended structure for developers building web mini-games for Nexus Platform.

## Goals

- Keep `index.html` at zip root (required by platform upload validator).
- Isolate platform APIs in `src/platform/bridge.js`.
- Keep gameplay code independent from platform-specific APIs.
- Support desktop browser local debug and Android runtime.

## Folder Layout

```text
minigame-starter/
├── index.html
├── manifest.json
├── src/
│   ├── main.js
│   ├── game/
│   │   └── Game.js
│   ├── platform/
│   │   ├── bridge.js
│   │   └── api.js
│   └── ui/
│       └── hud.js
├── assets/
│   └── .gitkeep
├── config/
│   ├── dev.json
│   └── prod.json
└── scripts/
    └── package.ps1
```

## Quick Start

1. Open this folder in local static server.
2. Edit game logic in `src/game/Game.js`.
3. Call backend through `src/platform/api.js`.
4. Build zip with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package.ps1
```

Output:

- `release/minigame-starter.zip`

Upload this zip in developer portal.

## Integration Rules

1. Keep `index.html` in zip root.
2. Do not rely on Node.js runtime in production package.
3. Use relative paths only.
4. Put platform-specific logic only in `src/platform/*`.
5. Keep entry file name stable: `index.html`.
