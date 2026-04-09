"""
RSPS Hub — Move Admin role to the top of the hierarchy so its colour takes priority.

    python discord-setup/fix_admin_role.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


class FixRoleBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        admin_role = discord.utils.get(guild.roles, name="Admin")

        if admin_role is None:
            print("ERROR: Admin role not found.")
            await self.close()
            return

        # Move Admin to the highest possible position (just below the bot's own role)
        bot_member = guild.get_member(self.user.id)
        bot_top = bot_member.top_role.position

        target_position = max(1, bot_top - 1)
        await admin_role.edit(position=target_position, colour=discord.Colour(0xff981f))
        print(f"[ok] Admin role moved to position {target_position} (orange, above all other roles).")
        await self.close()


client = FixRoleBot()
client.run(BOT_TOKEN)
