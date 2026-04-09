"""
RSPS Hub — Remove default Discord channels (voice + text) and reorder categories.

    python discord-setup/cleanup_server.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN

# Categories in the order you want them
CATEGORY_ORDER = ["INFORMATION", "COMMUNITY", "SUPPORT", "STAFF"]


class CleanupBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        print(f"Logged in as {self.user}. Cleaning up...\n")
        guild = self.get_guild(GUILD_ID)
        if guild is None:
            print("ERROR: Guild not found.")
            await self.close()
            return

        await self.cleanup(guild)
        print("\nDone!")
        await self.close()

    async def cleanup(self, guild: discord.Guild):
        our_channels = {ch.name for cat in guild.categories for ch in cat.channels}

        # ── 1. Delete all voice channels ────────────────────────────────────
        print("Removing voice channels...")
        for vc in guild.voice_channels:
            print(f"  [del] Voice: {vc.name}")
            await vc.delete(reason="RSPS Hub cleanup")

        # ── 2. Delete uncategorised text/stage channels ──────────────────────
        print("Removing uncategorised channels...")
        for ch in guild.channels:
            if ch.category is None and not isinstance(ch, discord.CategoryChannel):
                print(f"  [del] Uncategorised: #{ch.name}")
                await ch.delete(reason="RSPS Hub cleanup")

        # ── 3. Reorder categories ────────────────────────────────────────────
        print("Reordering categories...")
        cat_map = {c.name.upper(): c for c in guild.categories}
        ordered = []
        for i, name in enumerate(CATEGORY_ORDER):
            if name in cat_map:
                ordered.append(cat_map[name])

        for i, cat in enumerate(ordered):
            await cat.edit(position=i)
            print(f"  [ok] {i+1}. {cat.name}")


client = CleanupBot()
client.run(BOT_TOKEN)
