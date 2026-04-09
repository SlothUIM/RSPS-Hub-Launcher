"""
RSPS Hub — Generate logo PNG with Pillow and set as server icon.

    python discord-setup/upload_logo.py
"""

import discord
import io
from PIL import Image, ImageDraw, ImageFont

from config import BOT_TOKEN, GUILD_ID  # noqa
BOT_TOKEN = BOT_TOKEN


def make_logo() -> bytes:
    size = 512
    radius = 80
    bg     = (26, 29, 36)       # #1a1d24
    white  = (255, 255, 255)
    orange = (255, 152, 31)     # #ff981f

    img  = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Rounded square background
    draw.rounded_rectangle([0, 0, size, size], radius=radius, fill=bg)

    # Try to load a bold font, fall back to default
    try:
        font_rsps = ImageFont.truetype("arialbd.ttf", 108)
        font_hub  = ImageFont.truetype("arialbd.ttf", 80)
    except Exception:
        try:
            font_rsps = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 108)
            font_hub  = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 80)
        except Exception:
            font_rsps = ImageFont.load_default()
            font_hub  = font_rsps

    # "RSPS" centred at ~40% height
    bbox = draw.textbbox((0, 0), "RSPS", font=font_rsps)
    w = bbox[2] - bbox[0]
    draw.text(((size - w) / 2, 148), "RSPS", font=font_rsps, fill=white)

    # "HUB" centred below in orange
    bbox2 = draw.textbbox((0, 0), "HUB", font=font_hub)
    w2 = bbox2[2] - bbox2[0]
    draw.text(((size - w2) / 2, 272), "HUB", font=font_hub, fill=orange)

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


class LogoBot(discord.Client):
    def __init__(self):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

    async def on_ready(self):
        guild = self.get_guild(GUILD_ID)
        png = make_logo()
        await guild.edit(icon=png)
        print("[ok] Server icon updated.")
        await self.close()


client = LogoBot()
client.run(BOT_TOKEN)
