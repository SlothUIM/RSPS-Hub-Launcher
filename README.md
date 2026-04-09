# RSPS Hub

A free, unified launcher for RuneScape Private Servers — like Steam but for RSPS.

Built by [Vinnlarr](https://github.com/Vinnlarr) & [SlothUIM](https://github.com/SlothUIM).

![License](https://img.shields.io/badge/license-Proprietary-red)
![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17-orange)
![Status](https://img.shields.io/badge/status-early%20access-orange)

---

## What is RSPS Hub?

RSPS players know the pain — JAR files cluttering your desktop, cache folders eating memory, jumping between sketchy websites just to find a download link. RSPS Hub fixes all of that.

One launcher. Every server. No clutter.

---

## Features

- **Browse & Discover** — Searchable store with tags, filters, and real player reviews
- **One-Click Install & Play** — No JAR files on your desktop, no manual cache management
- **Playtime Tracking** — Level up per server (1–99 based on hours played)
- **Friends List** — See what your mates are playing in real time
- **Discord Rich Presence** — Automatically shows what server you're on
- **Reviews & Ratings** — Know if a server is worth your time before downloading
- **Developer Portal** — Server owners can list and manage their server directly in the launcher
- **Review Moderation** — Developers can approve or reject player reviews

---

## For Server Developers

Getting your server listed is free and takes minutes:

1. Download and open RSPS Hub
2. Go to **Settings → Developer Portal**
3. Fill in your server details — name, description, tags, banner, icon, JAR URL, Discord, website
4. Submit — your server will be reviewed and listed

---

## Tech Stack

- **Java 17**
- **JavaFX 17** — UI framework
- **Gson** — JSON parsing
- **Gradle** — build system

---

## Building from Source

```bash
git clone https://github.com/SlothUIM/RSPS-Hub-Launcher.git
cd RSPS-Hub-Launcher
./gradlew build
./gradlew run
```

Requires Java 17+ and JavaFX 17.

---

## Security

When you hit Play, the launcher runs the game JAR as a completely separate process and steps away. We never see what happens inside the game — login credentials, account data, and game traffic go directly between your client and the server. Nothing passes through us.

The full source code is open for anyone to audit.

---

## Community

- **Discord:** https://discord.gg/grt9C4GJcj
- **Reddit:** r/2007scape

---

## License

Copyright (c) 2026 Vinnlarr & SlothUIM. All Rights Reserved.

This software is **proprietary**. You may view and run it for personal use only. Copying, forking, redistribution, or commercial use is strictly prohibited. See [LICENSE](LICENSE) for full terms.
