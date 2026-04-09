"""
RSPS Hub — Post the official announcement embed to #announcements.

    python discord-setup/post_announcement.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


class AnnouncementBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        channel = discord.utils.get(guild.text_channels, name="announcements")

        embed = discord.Embed(
            title="Welcome to RSPS Hub 🚀",
            description=(
                "Hey everyone, I'm **Vinnlarr** — I've been playing RuneScape Private Servers for 18 years "
                "and I got tired of the same headache every RSPS player knows too well.\n\n"
                "JAR files cluttering your desktop. Cache folders eating your memory. "
                "Jumping between sketchy websites just to find a download. Manually hunting down old "
                "server files when your PC starts slowing down.\n\n"
                "So I built **RSPS Hub** — a free unified launcher that handles all of it for you."
            ),
            color=0xff981f
        )

        embed.add_field(
            name="🎮  For Players",
            value=(
                "**Browse & Discover** — Searchable store with tags, filters, and real player reviews. "
                "No more dodgy websites.\n"
                "**One-Click Play** — Install and launch any RSPS instantly. No JAR files, no command line, no hassle.\n"
                "**Stay Clean** — Everything is managed in one place. Clean installs, clean uninstalls, no leftover cache.\n"
                "**Playtime Tracking** — Earn levels per server (1–99 based on hours played). "
                "It's a meta-game on top of RSPS.\n"
                "**Friends List** — See what your mates are playing in real time and jump in with them.\n"
                "**Discord Rich Presence** — Automatically shows what server you're on.\n"
                "**Reviews & Ratings** — Know if a server is worth your time before you download anything."
            ),
            inline=False
        )

        embed.add_field(
            name="🛠️  For Server Developers",
            value=(
                "**Free Listing** — Get your server in front of every RSPS Hub player at no cost.\n"
                "**Dev Portal** — Submit and manage your server directly from inside the launcher. "
                "Set your name, description, tags, banner, icon, accent colour, changelog, and screenshots.\n"
                "**Review Moderation** — Approve or reject player reviews before they go public.\n"
                "**Visibility Control** — Toggle your server live or hidden at any time.\n"
                "**Player Stats** — See playtime and engagement from real players.\n\n"
                "To get listed, open the launcher → Settings → Developer Portal, fill in your server details and submit."
            ),
            inline=False
        )

        embed.add_field(
            name="📅  Release",
            value=(
                "RSPS Hub is launching **Sunday 12th April** — it may be sooner.\n"
                "It is completely free to use. No cost, no catch, built for the community."
            ),
            inline=False
        )

        embed.add_field(
            name="🔗  Links",
            value=(
                "**GitHub:** https://github.com/SlothUIM/RSPS-Hub-Launcher\n"
                "**Discord:** https://discord.gg/grt9C4GJcj"
            ),
            inline=False
        )

        embed.set_footer(text="Built by Vinnlarr  •  RSPS Hub  •  For the community, by the community")

        await channel.send(embed=embed)
        print("[ok] Announcement posted.")
        await self.close()


client = AnnouncementBot()
client.run(BOT_TOKEN)
