"""Check channel permissions are correctly set."""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


class CheckBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        everyone = guild.default_role

        checks = {
            "announcements": {"read": True,  "write": False},
            "changelog":     {"read": True,  "write": False},
            "rules":         {"read": True,  "write": False},
            "staff-chat":    {"read": False, "write": False},
            "mod-log":       {"read": False, "write": False},
        }

        print(f"{'Channel':<20} {'@everyone read':<18} {'@everyone write':<18} {'OK?'}")
        print("-" * 65)

        for ch_name, expected in checks.items():
            ch = discord.utils.get(guild.text_channels, name=ch_name)
            if ch is None:
                print(f"{ch_name:<20} CHANNEL NOT FOUND")
                continue

            ow = ch.overwrites_for(everyone)
            can_read  = ow.read_messages
            can_write = ow.send_messages

            read_ok  = (can_read  == expected["read"])  or (can_read  is None and expected["read"])
            write_ok = (can_write == expected["write"]) or (can_write is None and not expected["write"])
            ok = "OK" if (read_ok and write_ok) else "NEEDS FIX"

            print(f"{ch_name:<20} {str(can_read):<18} {str(can_write):<18} {ok}")

        await self.close()


client = CheckBot()
client.run(BOT_TOKEN)
