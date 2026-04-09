"""
RSPS Hub — Add staff-only voice channels.

    python discord-setup/add_voice.py
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
        staff_role = discord.utils.get(guild.roles, name="RSPS Hub Team")
        staff_cat  = discord.utils.get(guild.categories, name="STAFF")

        overwrites = {
            guild.default_role: discord.PermissionOverwrite(connect=False, view_channel=False),
            staff_role:         discord.PermissionOverwrite(connect=True,  view_channel=True),
        }

        existing = {ch.name for ch in staff_cat.channels}

        for name in ["Staff Chat", "Staff Lounge"]:
            if name in existing:
                print(f"  [skip] {name} already exists")
            else:
                await guild.create_voice_channel(name, category=staff_cat, overwrites=overwrites)
                print(f"  [ok] Created voice: {name}")

        print("Done!")
        await self.close()


client = VoiceBot()
client.run(BOT_TOKEN)
