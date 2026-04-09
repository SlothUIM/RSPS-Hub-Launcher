"""
RSPS Hub — Add Admin and Support roles with correct permissions.

    python discord-setup/add_roles.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN

# (name, colour_hex, hoist, permissions)
NEW_ROLES = [
    ("Admin",   0xff981f, True, discord.Permissions(administrator=True)),
    ("Support", 0x4a9eff, True, discord.Permissions(
        manage_messages=True,
        kick_members=True,
        ban_members=True,
        manage_channels=False,
        read_messages=True,
        send_messages=True,
        view_audit_log=True,
    )),
]


class RoleBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        existing = {r.name for r in guild.roles}

        for name, colour, hoist, perms in NEW_ROLES:
            if name in existing:
                print(f"  [skip] {name} already exists")
            else:
                await guild.create_role(
                    name=name,
                    colour=discord.Colour(colour),
                    hoist=hoist,
                    permissions=perms,
                    mentionable=True,
                    reason="RSPS Hub setup"
                )
                print(f"  [ok] Created role: {name}")

        print("Done!")
        await self.close()


client = RoleBot()
client.run(BOT_TOKEN)
