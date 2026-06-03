from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "vedaahaar_technical_process.png"
OUT.parent.mkdir(parents=True, exist_ok=True)

W, H = 1500, 1700
BG = "#FFF9EA"
GREEN = "#0B6B3A"
DARK = "#064B2B"
LIGHT_GREEN = "#DFF2E4"
BLUE = "#E7F1FF"
CREAM = "#FFFDF5"
GOLD = "#F3C35C"
RED = "#FCE7E2"
LINE = "#244D7E"
TEXT = "#092A19"
MUTED = "#425648"


def font(size, bold=False):
    paths = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for path in paths:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


TITLE = font(64, True)
H1 = font(31, True)
H2 = font(24, True)
BODY = font(21)
SMALL = font(18)

img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def center_text(text, y, fnt, fill=GREEN):
    box = draw.textbbox((0, 0), text, font=fnt)
    draw.text(((W - (box[2] - box[0])) / 2, y), text, font=fnt, fill=fill)


def box(x, y, w, h, title, lines, fill=LIGHT_GREEN, outline=GREEN, radius=26):
    draw.rounded_rectangle((x, y, x + w, y + h), radius=radius, fill=fill, outline=outline, width=4)
    draw.text((x + 24, y + 18), title, fill=DARK, font=H1)
    yy = y + 62
    for line in lines:
        draw.text((x + 28, yy), line, fill=MUTED, font=BODY)
        yy += 31


def mini_box(x, y, w, h, title, lines, fill=CREAM):
    draw.rounded_rectangle((x, y, x + w, y + h), radius=20, fill=fill, outline=GREEN, width=3)
    draw.text((x + 18, y + 14), title, fill=DARK, font=H2)
    yy = y + 48
    for line in lines:
        draw.text((x + 20, yy), line, fill=MUTED, font=SMALL)
        yy += 25


def arrow(x1, y1, x2, y2, color=LINE, width=5):
    draw.line((x1, y1, x2, y2), fill=color, width=width)
    if abs(y2 - y1) >= abs(x2 - x1):
        s = 1 if y2 > y1 else -1
        pts = [(x2, y2), (x2 - 13, y2 - s * 22), (x2 + 13, y2 - s * 22)]
    else:
        s = 1 if x2 > x1 else -1
        pts = [(x2, y2), (x2 - s * 22, y2 - 13), (x2 - s * 22, y2 + 13)]
    draw.polygon(pts, fill=color)


center_text("Process For", 40, TITLE)
center_text("Implementation", 112, TITLE)

# User
draw.ellipse((708, 225, 752, 269), fill=TEXT)
draw.rounded_rectangle((695, 270, 765, 335), radius=18, fill=TEXT)
draw.text((800, 260), "USER", font=H1, fill=TEXT)
arrow(810, 248, 760, 248)

# Main vertical pipeline
cx = W // 2
top_x, bw = 430, 640
box(top_x, 360, bw, 135, "Frontend", ["Kotlin, Android Native", "Jetpack Compose, Material Design 3"], fill=LIGHT_GREEN)
arrow(cx, 495, cx, 560)

box(top_x, 560, bw, 135, "API Layer", ["Django REST API", "MapTiler Maps + Geocoding API"], fill="#EAF8E9")
arrow(cx, 695, cx, 760)

box(top_x, 760, bw, 150, "Backend", ["Python, Django, Django REST Framework", "WSGI, Simple JWT, Django Auth"], fill=LIGHT_GREEN)
arrow(cx, 910, cx, 995)

# Core system
draw.rounded_rectangle((110, 995, 1390, 1058), radius=16, fill=GOLD, outline=DARK, width=3)
center_text("Core System", 1010, H1, fill=DARK)

# Mid-layer services
mini_box(
    80,
    1100,
    390,
    250,
    "Database",
    ["PostgreSQL", "Firebase Firestore", "SharedPreferences"],
    fill=CREAM,
)
mini_box(
    555,
    1100,
    390,
    250,
    "Cloud / Auth",
    ["Firebase Authentication", "Firebase Storage", "Firebase Analytics", "Google Services"],
    fill=BLUE,
)
mini_box(
    1030,
    1100,
    390,
    250,
    "Maps System",
    ["MapTiler", "OpenStreetMap", "Leaflet", "Android WebView"],
    fill=CREAM,
)

arrow(cx, 1058, 275, 1100)
arrow(cx, 1058, 750, 1100)
arrow(cx, 1058, 1225, 1100)

# Integration/deployment row
mini_box(
    80,
    1410,
    390,
    190,
    "UI / State",
    ["AndroidX Compose UI", "Navigation Compose", "Lifecycle ViewModel", "Compose State"],
    fill=LIGHT_GREEN,
)
mini_box(
    555,
    1410,
    390,
    190,
    "Build / Testing",
    ["Gradle, AGP, Kotlin Plugin", "ProGuard/R8", "JUnit, Espresso", "Compose UI Test"],
    fill=CREAM,
)
mini_box(
    1030,
    1410,
    390,
    190,
    "Libraries",
    ["Firebase BoM", "psycopg2-binary", "Pillow", "python-dotenv, Leaflet CDN"],
    fill=LIGHT_GREEN,
)

arrow(275, 1350, 275, 1410)
arrow(750, 1350, 750, 1410)
arrow(1225, 1350, 1225, 1410)

draw.rounded_rectangle((375, 1630, 1125, 1685), radius=22, fill=GREEN, outline=DARK, width=3)
center_text("Response Back to User: Dashboard / Diet Plan / Community Care", 1644, BODY, fill="white")

img.save(OUT)
print(str(OUT))
