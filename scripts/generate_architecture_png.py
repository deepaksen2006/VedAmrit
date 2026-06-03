from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "vedaahaar_complete_architecture.png"
OUT.parent.mkdir(parents=True, exist_ok=True)

W, H = 2600, 2200
BG = "#F7FAF2"
INK = "#183323"
MUTED = "#56645B"
GREEN = "#2E7D32"
DARK_GREEN = "#173F24"
CREAM = "#FFFDF6"
SAGE = "#E7F1E5"
GOLD = "#E4BF65"
BLUE = "#EAF3FF"
RED = "#FCE7E5"
PURPLE = "#F1ECFF"
LINE = "#AFC6AC"


def font(size, bold=False):
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


TITLE = font(54, True)
SUBTITLE = font(28)
H1 = font(28, True)
BODY = font(22)
SMALL = font(18)
TINY = font(16)


img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def wrap_text(text, fnt, max_width):
    words = text.split()
    lines, current = [], ""
    for word in words:
        test = f"{current} {word}".strip()
        if draw.textbbox((0, 0), test, font=fnt)[2] <= max_width:
            current = test
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def card(x, y, w, h, title, body=None, fill=CREAM, border=LINE, title_color=INK):
    draw.rounded_rectangle((x, y, x + w, y + h), radius=24, fill=fill, outline=border, width=3)
    draw.text((x + 26, y + 22), title, fill=title_color, font=H1)
    if body:
        yy = y + 66
        for line in wrap_text(body, BODY, w - 52):
            draw.text((x + 26, yy), line, fill=MUTED, font=BODY)
            yy += 30


def pill(x, y, text, fill, color=INK):
    pad_x, pad_y = 18, 10
    box = draw.textbbox((0, 0), text, font=SMALL)
    tw, th = box[2] - box[0], box[3] - box[1]
    draw.rounded_rectangle((x, y, x + tw + pad_x * 2, y + th + pad_y * 2), radius=18, fill=fill, outline=LINE, width=2)
    draw.text((x + pad_x, y + pad_y - 2), text, fill=color, font=SMALL)
    return tw + pad_x * 2


def arrow(x1, y1, x2, y2, color=DARK_GREEN, width=4):
    draw.line((x1, y1, x2, y2), fill=color, width=width)
    dx, dy = x2 - x1, y2 - y1
    if abs(dx) > abs(dy):
        sign = 1 if dx > 0 else -1
        pts = [(x2, y2), (x2 - sign * 18, y2 - 10), (x2 - sign * 18, y2 + 10)]
    else:
        sign = 1 if dy > 0 else -1
        pts = [(x2, y2), (x2 - 10, y2 - sign * 18), (x2 + 10, y2 - sign * 18)]
    draw.polygon(pts, fill=color)


# Header
draw.text((90, 58), "VedaAahar Complete Technical App Architecture", fill=INK, font=TITLE)
draw.text(
    (92, 126),
    "Android Native healthcare app with Ayurvedic diet intelligence, doctor consultation, Firebase, Django backend, and map-based community care.",
    fill=MUTED,
    font=SUBTITLE,
)

# Main user flow
flow_y = 220
flow = [
    ("Landing", "Welcome page"),
    ("Auth", "Firebase Auth + Django Auth + JWT"),
    ("Consent", "DPDP-style privacy consent"),
    ("Profile", "Patient demographics and health profile"),
    ("15 Categories", "Health, lifestyle, goal, food and location inputs"),
    ("Dosha Test", "Vata, Pitta, Kapha scoring"),
    ("Dashboard", "Home, Wellness, Consult, Shopping, Profile"),
]
x = 90
for i, (t, b) in enumerate(flow):
    card(x, flow_y, 300, 150, t, b, fill=CREAM)
    if i < len(flow) - 1:
        arrow(x + 300, flow_y + 75, x + 350, flow_y + 75)
    x += 350

# Diet pipeline
card(90, 470, 2420, 540, "Personalized Ayurvedic Diet Intelligence Pipeline", fill="#FFF9E8", border=GOLD)
steps = [
    ("1. Data Collection", "15 categories + patient profile"),
    ("2. Database Storage", "PostgreSQL + Firestore + SharedPreferences"),
    ("3. Response Encoding", "Answers converted into machine-readable values"),
    ("4. Ayurvedic Rule Engine", "Rules and mappings for patient understanding"),
    ("5. Health Analysis", "Prakriti, Vikriti, Agni, Ama"),
    ("6. Dosha Scoring", "Vata, Pitta, Kapha imbalance"),
    ("7. Food Intelligence DB", "Nutrition, rasa, digestibility, dosha suitability"),
    ("8. Recommendation Engine", "Food scoring by dosha, agni, ama, goals"),
    ("9. ML Enhancement", "Optional decision-tree refinement"),
    ("10. Diet Generator", "Meals, include/avoid, lifestyle, hydration"),
    ("11. Final Output", "Diet plan + patient report"),
]
sx, sy = 130, 570
for i, (t, b) in enumerate(steps):
    row = 0 if i < 6 else 1
    col = i if i < 6 else i - 6
    bw = 360 if row == 0 else 430
    bh = 150
    gap = 35
    xx = 130 + col * (bw + gap)
    yy = sy + row * 210
    fill = [SAGE, BLUE, "#EEF7EE", "#FFF2DC", PURPLE, RED][i % 6]
    card(xx, yy, bw, bh, t, b, fill=fill)
    if row == 0 and i < 5:
        arrow(xx + bw, yy + 75, xx + bw + gap - 8, yy + 75)
    if i == 5:
        arrow(xx + bw / 2, yy + bh, xx + bw / 2, yy + 205)
    if row == 1 and i < 10:
        arrow(xx + bw, yy + 75, xx + bw + gap - 8, yy + 75)

# Dashboard feature modules
card(90, 1080, 2420, 390, "Dashboard Feature Modules", fill=CREAM)
modules = [
    ("Home", "Personalized Diet\nLifestyle Calculator\nRetake Dosha Test\nIngredient Book"),
    ("Wellness", "Yoga & Meditation\nIngredient Book\nHealth Reminder"),
    ("Consult", "Doctor Consultation\nCommunity Care\nNearby Healthcare"),
    ("Shopping", "Ayurvedic Store\nHerbal Products"),
    ("Profile", "Patient Details\nHealth History\nSaved State"),
]
mx = 130
for title, body in modules:
    card(mx, 1180, 440, 230, title, body, fill=SAGE if title != "Consult" else BLUE)
    mx += 475

# Layers
layer_y = 1540
layers = [
    ("Frontend / Mobile", "Android Native, Kotlin, Jetpack Compose, Material Design 3, AndroidX Compose UI, Navigation Compose"),
    ("State & Local Storage", "AndroidX Lifecycle ViewModel, Compose State, SharedPreferences"),
    ("Backend API", "Python, Django, Django REST Framework, WSGI, Simple JWT, Django Auth"),
    ("Databases", "PostgreSQL, Firebase Firestore, SharedPreferences"),
    ("Firebase Cloud", "Firebase Authentication, Firebase Storage, Firebase Analytics, Google Services"),
    ("Maps", "MapTiler, MapTiler Geocoding API, OpenStreetMap, Leaflet, Android WebView, HTML, CSS, JavaScript"),
    ("Build / Test", "Gradle, Gradle Wrapper, Android Gradle Plugin, Kotlin Gradle Plugin, ProGuard/R8, JUnit, Espresso, Compose UI Test"),
]
for i, (t, b) in enumerate(layers):
    xx = 90 + (i % 4) * 625
    yy = layer_y + (i // 4) * 260
    card(xx, yy, 570, 210, t, b, fill=[CREAM, SAGE, BLUE, PURPLE][i % 4])

# Cross-layer arrows and footer
arrow(1300, 1470, 1300, 1535, color=GREEN)
draw.rounded_rectangle((90, 2070, 2510, 2140), radius=24, fill=DARK_GREEN)
draw.text(
    (130, 2090),
    "End-to-end flow: User -> Compose UI -> ViewModel/Local State -> Firebase + Django REST API -> PostgreSQL/Firestore -> Diet Report, Consultation, Community Care",
    fill="white",
    font=BODY,
)

img.save(OUT)
print(str(OUT))
