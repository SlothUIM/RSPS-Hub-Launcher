"""
RSPS Hub — Update the #rules channel message.

    python discord-setup/update_rules.py
"""

import discord

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN

RULES = """**RSPS Hub — Rules**

1. Be respectful to all members.
2. No spam or self-promotion.
3. Keep discussions relevant to the channel topic.
4. No NSFW content.
5. Server developers must own the server they promote.
6. Report bugs in #bug-reports, not in general chat."""


class RulesBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        channel = discord.utils.get(guild.text_channels, name="rules")

        async for msg in channel.history(limit=10):
            if msg.author == self.user:
                await msg.edit(content=RULES)
                print("[ok] Rules updated.")
                await self.close()
                return

        await channel.send(RULES)
        print("[ok] Rules posted.")
        await self.close()


client = RulesBot()
client.run(BOT_TOKEN)
