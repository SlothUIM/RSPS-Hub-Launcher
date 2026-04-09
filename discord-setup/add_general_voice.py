"""
RSPS Hub — Add a general voice channel open to all.

    python discord-setup/add_general_voice.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


class VoiceBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        community_cat = discord.utils.get(guild.categories, name="COMMUNITY")

        existing = {ch.name for ch in community_cat.channels}

        if "General Voice" in existing:
            print("  [skip] General Voice already exists")
        else:
            await guild.create_voice_channel("General Voice", category=community_cat)
            print("  [ok] Created voice: General Voice")

        print("Done!")
        await self.close()


client = VoiceBot()
client.run(BOT_TOKEN)
