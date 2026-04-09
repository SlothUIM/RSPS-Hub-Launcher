"""
RSPS Hub — Main persistent bot.
Handles auto-role assignment and any future automation.

Keep this running in the background at all times.

    python discord-setup/bot.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


class RSPSHubBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        intents.members = True
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        print(f"[RSPS Hub Bot] Online — {guild.name} ({guild.member_count} members)")

    async def on_member_join(self, member: discord.Member):
        if member.guild.id != GUILD_ID:
            return

        player_role = discord.utils.get(member.guild.roles, name="Player")
        if player_role:
            await member.add_roles(player_role, reason="Auto-role on join")
            print(f"[auto-role] Assigned Player to {member.name}")
        else:
            print(f"[auto-role] WARNING: Player role not found for {member.name}")


client = RSPSHubBot()
client.run(BOT_TOKEN)
