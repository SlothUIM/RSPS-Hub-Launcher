"""
Generate src/app_icon.png (256x256) for RSPS Hub Launcher.
Matches the brand logo: dark rounded square, "RSPS" white, "HUB" blue (#4895ef).
Requires: pip install Pillow
"""

from PIL import Image, ImageDraw, ImageFont
import os

OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "src", "app_icon.png")

W = H = 256
BG    = (22, 25, 35)      # #161923 — dark background
BLUE  = (72, 149, 239)    # #4895ef — accent (replaces orange)
WHITE = (230, 235, 245)   # near-white for "RSPS"

# ── Canvas ────────────────────────────────────────────────────────────────────
img  = Image.new("RGBA", (W, H), (0, 0, 0, 0))
mask = Image.new("L",    (W, H), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, W-1, H-1], radius=52, fill=255)

bg  = Image.new("RGBA", (W, H), (*BG, 255))
drw = ImageDraw.Draw(bg)

# ── Fonts ─────────────────────────────────────────────────────────────────────
font_lg = font_sm = None
for fp in [
    "C:/Windows/Fonts/arialbd.ttf",
    "C:/Windows/Fonts/verdanab.ttf",
    "C:/Windows/Fonts/calibrib.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]:
    if os.path.exists(fp):
        font_lg = ImageFont.truetype(fp, 90)   # RSPS
        font_sm = ImageFont.truetype(fp, 82)   # HUB
        break
if font_lg is None:
    font_lg = font_sm = ImageFont.load_default()

# ── Layout: stack "RSPS" and "HUB" vertically, centred ───────────────────────
GAP = 6   # pixels between the two words

bb_r = drw.textbbox((0, 0), "RSPS", font=font_lg)
rw, rh = bb_r[2] - bb_r[0], bb_r[3] - bb_r[1]

bb_h = drw.textbbox((0, 0), "HUB", font=font_sm)
hw, hh = bb_h[2] - bb_h[0], bb_h[3] - bb_h[1]

total_h = rh + GAP + hh
top_y   = (H - total_h) // 2

rsps_x = (W - rw) // 2 - bb_r[0]
rsps_y = top_y - bb_r[1]

hub_x  = (W - hw) // 2 - bb_h[0]
hub_y  = top_y + rh + GAP - bb_h[1]

# ── Draw text ─────────────────────────────────────────────────────────────────
drw.text((rsps_x, rsps_y), "RSPS", font=font_lg, fill=(*WHITE, 255))
drw.text((hub_x,  hub_y),  "HUB",  font=font_sm, fill=(*BLUE,  255))

# ── Apply rounded mask & save ─────────────────────────────────────────────────
bg.putalpha(mask)
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
bg.save(OUTPUT_PATH, "PNG")
print(f"Saved: {OUTPUT_PATH}  ({W}x{H})")
