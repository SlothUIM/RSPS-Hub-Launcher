"""Save the RSPS Hub logo as a PNG file for uploading to Discord developer portal."""

from PIL import Image, ImageDraw, ImageFont
import io

def make_logo() -> bytes:
    size   = 512
    bg     = (26, 29, 36)
    white  = (255, 255, 255)
    orange = (255, 152, 31)

    img  = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([0, 0, size, size], radius=80, fill=bg)

    try:
        font_rsps = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 108)
        font_hub  = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 80)
    except Exception:
        font_rsps = ImageFont.load_default()
        font_hub  = font_rsps

    bbox = draw.textbbox((0, 0), "RSPS", font=font_rsps)
    draw.text(((size - (bbox[2] - bbox[0])) / 2, 148), "RSPS", font=font_rsps, fill=white)

    bbox2 = draw.textbbox((0, 0), "HUB", font=font_hub)
    draw.text(((size - (bbox2[2] - bbox2[0])) / 2, 272), "HUB", font=font_hub, fill=orange)

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()

with open("discord-setup/rsps-hub-logo.png", "wb") as f:
    f.write(make_logo())

print("Saved: discord-setup/rsps-hub-logo.png")
