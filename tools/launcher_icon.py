"""Generates the launcher icon (happy flower, dark green), its themed variant and the animated splash icon
as vector drawables, plus an HTML preview (icons + splash animation frames).

Usage: python tools/launcher_icon.py app/src/main/res <preview.html>
"""
import sys

BG = "#1F7A55"
YELLOW, BLUE, CORAL, PURPLE, LIGHT_GREEN, MINT = "#FFC61A", "#29A8E0", "#E5677A", "#B36AE2", "#8FD9A8", "#B4F1D2"
PETALS = ["#5DBB85", "#5BC0EB", "#F08A9B"]
CENTER = (54, 56)


def f(v):
    return f"{v:g}"


# ---------- primitives ----------
class Path:
    def __init__(self, d, fill=None, stroke=None, width=None, alpha=None, grad=None, evenodd=False):
        self.d, self.fill, self.stroke, self.width, self.alpha, self.grad, self.evenodd = d, fill, stroke, width, alpha, grad, evenodd


class Group:
    def __init__(self, children, tx=0, ty=0, rot=0, px=0, py=0, scale=1, sx=None, sy=None, name=None):
        self.children, self.tx, self.ty, self.rot, self.px, self.py = children, tx, ty, rot, px, py
        self.sx, self.sy, self.name = scale if sx is None else sx, scale if sy is None else sy, name


def T(x, y, *c):
    return Group(list(c), tx=x, ty=y)


def R(a, px, py, *c):
    return Group(list(c), rot=a, px=px, py=py)


def S(k, *c):
    return Group(list(c), scale=k)


def ellipse(cx, cy, rx, ry):
    return f"M{f(cx-rx)},{f(cy)} a{f(rx)},{f(ry)} 0 1,0 {f(2*rx)},0 a{f(rx)},{f(ry)} 0 1,0 {f(-2*rx)},0 Z"


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def rrect(x, y, w, h, r):
    return (f"M{f(x+r)},{f(y)} h{f(w-2*r)} a{f(r)},{f(r)} 0 0,1 {f(r)},{f(r)} v{f(h-2*r)} "
            f"a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(r)} h{f(-(w-2*r))} a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(-r)} "
            f"v{f(-(h-2*r))} a{f(r)},{f(r)} 0 0,1 {f(r)},{f(-r)} Z")


CHECK = "M-2.6,0 L-0.8,1.9 L2.6,-1.9"
SPARKLE = "M0,-3.5 Q0.6,-0.6 3.5,0 Q0.6,0.6 0,3.5 Q-0.6,0.6 -3.5,0 Q-0.6,-0.6 0,-3.5 Z"
STEM = "M54,68 C53,77 55,77 54,86"
LEAF_L = "M54,80 C47,79 40,73 37,64 C44,64 51,70 54,80 Z"
LEAF_R = "M54,80 C61,79 68,73 71,64 C64,64 57,70 54,80 Z"
SMILE = "M-3.2,1.6 Q0,5 3.2,1.6"
BOXES = [(36, 43, YELLOW, 9, -18), (53, 30.5, BLUE, 8, 6), (73, 39, CORAL, 10.5, 14)]
SPARKS = [(41, 29, 0.9, YELLOW), (65.5, 26, 0.65, YELLOW), (81, 52, 0.8, MINT), (28.5, 62, 0.6, YELLOW), (62, 39, 0.45, MINT)]
BITS = [(29.5, 51, PURPLE, 30), (79, 65, YELLOW, -20), (32, 73, BLUE, 50), (75, 77, LIGHT_GREEN, -40), (45, 36, CORAL, -60)]
DOTS = [(47, 24.5, CORAL, 1.2), (82, 44, PURPLE, 1), (27, 45, YELLOW, 1), (68, 84, CORAL, 1)]


def leaf_grad(x0, y0, x1, y1):
    return ("linear", x0, y0, x1, y1, "#D6F7E3", "#6CC794")


def color_parts():
    """The color drawing split into the parts the splash animates, in drawing order."""
    cx, cy = CENTER
    parts = {"stem": [Path(STEM, stroke=LIGHT_GREEN, width=3),
                      Path(LEAF_L, grad=leaf_grad(37, 64, 54, 80)), Path(LEAF_R, grad=leaf_grad(54, 64, 71, 80))],
             "petals": [R(i * 36, cx, cy, Path(ellipse(cx, cy - 9, 3.6, 7), fill=PETALS[i % 3], stroke="#FFFFFF", width=0.8))
                        for i in range(10)],
             "center": [Path(circle(cx, cy, 6.5), grad=("radial", 52.7, 54.05, 10.4, "#FFE08A", "#F2B824")), face()]}
    for i, (x, y, c, s, r) in enumerate(BOXES):
        parts[f"box{i}"] = [T(x, y, R(r, 0, 0, Path(rrect(-s/2, -s/2, s, s, s*0.27), fill=c)),
                              S(s/10, Path(CHECK, stroke="#FFFFFF", width=1.7)))]
    parts["party"] = party()
    return parts


def color_icon():
    return [n for part in color_parts().values() for n in part]


def face():
    cx, cy = CENTER
    brown = "#5A3A00"
    return T(cx, cy, Path(circle(-3, -1.5, 1.2), fill=brown), Path(circle(3, -1.5, 1.2), fill=brown),
             Path(SMILE, stroke=brown, width=1.4),
             Path(circle(-5, 1.4, 1.3), fill=CORAL, alpha=0.5), Path(circle(5, 1.4, 1.3), fill=CORAL, alpha=0.5))


def party(mono=False):
    out = []
    for x, y, k, c in SPARKS:
        out.append(T(x, y, S(k, Path(SPARKLE, fill="#FFFFFF" if mono else c))))
    if mono:
        return out
    for x, y, c, r in BITS:
        out.append(R(r, x, y, Path(rrect(x-1, y-2.2, 2, 4.4, 0.8), fill=c)))
    for x, y, c, r in DOTS:
        out.append(Path(circle(x, y, r), fill=c))
    return out


def mono_icon():
    """Themed icon: same drawing as line art (the launcher only keeps the alpha)."""
    cx, cy, w = *CENTER, "#FFFFFF"
    out = [Path(STEM, stroke=w, width=3), Path(LEAF_L, fill=w), Path(LEAF_R, fill=w)]
    # Solid petals kept clear of the center, so the face can be cut out of it (even-odd holes).
    for i in range(10):
        out.append(R(i * 36, cx, cy, Path(ellipse(cx, cy - 12.2, 3.3, 4.4), fill=w)))
    face_holes = circle(-3, -1.5, 1.3) + " " + circle(3, -1.5, 1.3) + " M-3.4,1.4 Q0,6 3.4,1.4 Q0,3.6 -3.4,1.4 Z"
    out.append(T(cx, cy, Path(circle(0, 0, 6.5) + " " + face_holes, fill=w, evenodd=True)))
    for x, y, _, s, r in BOXES:
        out.append(T(x, y, R(r, 0, 0, Path(rrect(-s/2, -s/2, s, s, s*0.27), stroke=w, width=1.3)), S(s/10, Path(CHECK, stroke=w, width=1.7))))
    out += party(mono=True)
    return out


# ---------- Android vector drawable ----------
def vd_path(p, ind):
    a = [f'android:pathData="{p.d}"']
    if p.fill:
        a.append(f'android:fillColor="{p.fill}"')
    if p.alpha is not None:
        a.append(f'android:fillAlpha="{p.alpha}"')
    if p.evenodd:
        a.append('android:fillType="evenOdd"')
    if p.stroke:
        a += [f'android:strokeColor="{p.stroke}"', f'android:strokeWidth="{f(p.width)}"',
              'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
    sep = "\n" + ind + "    "
    if not p.grad:
        return f"{ind}<path{sep}{sep.join(a)} />\n"
    g = p.grad
    if g[0] == "linear":
        ga = f'android:type="linear" android:startX="{f(g[1])}" android:startY="{f(g[2])}" android:endX="{f(g[3])}" android:endY="{f(g[4])}"'
        c0, c1 = g[5], g[6]
    else:
        ga = f'android:type="radial" android:centerX="{f(g[1])}" android:centerY="{f(g[2])}" android:gradientRadius="{f(g[3])}"'
        c0, c1 = g[4], g[5]
    return (f"{ind}<path{sep}{sep.join(a)}>\n{ind}    <aapt:attr name=\"android:fillColor\">\n"
            f"{ind}        <gradient {ga}>\n{ind}            <item android:offset=\"0\" android:color=\"{c0}\" />\n"
            f"{ind}            <item android:offset=\"1\" android:color=\"{c1}\" />\n{ind}        </gradient>\n"
            f"{ind}    </aapt:attr>\n{ind}</path>\n")


def vd_node(n, ind="    "):
    if isinstance(n, Path):
        return vd_path(n, ind)
    a = []
    if n.tx or n.ty:
        a += [f'android:translateX="{f(n.tx)}"', f'android:translateY="{f(n.ty)}"']
    if n.name:
        a.append(f'android:name="{n.name}"')
    if n.px or n.py:
        a += [f'android:pivotX="{f(n.px)}"', f'android:pivotY="{f(n.py)}"']
    if n.rot:
        a.append(f'android:rotation="{f(n.rot)}"')
    if n.sx != 1:
        a.append(f'android:scaleX="{f(n.sx)}"')
    if n.sy != 1:
        a.append(f'android:scaleY="{f(n.sy)}"')
    inner = "".join(vd_node(c, ind + "    ") for c in n.children)
    return f"{ind}<group {' '.join(a)}>\n{inner}{ind}</group>\n"


def vd(nodes, comment):
    body = "".join(vd_node(n) for n in nodes)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            f"<!-- {comment} Generated by tools/launcher_icon.py: edit the script, not this file. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            f"{body}</vector>\n")


# ---------- SVG preview ----------
GRAD_IDS = []


def svg_node(n):
    if isinstance(n, Path):
        fill = n.fill or "none"
        if n.grad:
            gid = f"g{len(GRAD_IDS)}"
            g = n.grad
            if g[0] == "linear":
                GRAD_IDS.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{g[1]}" y1="{g[2]}" x2="{g[3]}" y2="{g[4]}"><stop offset="0" stop-color="{g[5]}"/><stop offset="1" stop-color="{g[6]}"/></linearGradient>')
            else:
                GRAD_IDS.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{g[1]}" cy="{g[2]}" r="{g[3]}"><stop offset="0" stop-color="{g[4]}"/><stop offset="1" stop-color="{g[5]}"/></radialGradient>')
            fill = f"url(#{gid})"
        s = f'<path d="{n.d}" fill="{fill}"'
        if n.alpha is not None:
            s += f' fill-opacity="{n.alpha}"'
        if n.evenodd:
            s += ' fill-rule="evenodd"'
        if n.stroke:
            s += f' stroke="{n.stroke}" stroke-width="{n.width}" stroke-linecap="round" stroke-linejoin="round"'
        return s + "/>"
    # Same order as a VectorDrawable group: scale, then rotate (both around the pivot), then translate.
    t = f"translate({n.tx + n.px} {n.ty + n.py}) rotate({n.rot}) scale({n.sx} {n.sy}) translate({-n.px} {-n.py})"
    return f'<g transform="{t}">' + "".join(svg_node(c) for c in n.children) + "</g>"


def svg(nodes, bg):
    GRAD_IDS.clear()
    body = "".join(svg_node(n) for n in nodes)
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72"><defs>{"".join(GRAD_IDS)}</defs><rect width="108" height="108" fill="{bg}"/>{body}</svg>'


# ---------- animated splash icon ----------
SPLASH_MS = 1000
# part -> (pivot, {property: (from, to)}, start ms, duration ms, interpolator)
SPLASH = {
    "stem": ((54, 86), {"scaleY": (0, 1)}, 0, 300, "decelerate"),
    "petals": (CENTER, {"scaleX": (0, 1), "scaleY": (0, 1), "rotation": (-90, 0)}, 200, 450, "overshoot"),
    "center": (CENTER, {"scaleX": (0, 1), "scaleY": (0, 1)}, 450, 250, "overshoot"),
    "box0": (BOXES[0][:2], {"scaleX": (0, 1), "scaleY": (0, 1)}, 550, 200, "overshoot"),
    "box1": (BOXES[1][:2], {"scaleX": (0, 1), "scaleY": (0, 1)}, 650, 200, "overshoot"),
    "box2": (BOXES[2][:2], {"scaleX": (0, 1), "scaleY": (0, 1)}, 750, 200, "overshoot"),
    "party": (CENTER, {"scaleX": (0, 1), "scaleY": (0, 1)}, 700, 300, "decelerate"),
}
INTERPOLATORS = {"decelerate": "@android:interpolator/decelerate_cubic", "overshoot": "@android:interpolator/overshoot"}


def ease(kind, t):
    if kind == "decelerate":
        return 1 - (1 - t) ** 3
    t -= 1
    return t * t * (3 * t + 2) + 1  # OvershootInterpolator, tension 2


def splash_nodes(t_ms=None):
    """Animated parts wrapped in named groups; t_ms=None gives the initial (hidden) state the drawable starts in."""
    out = []
    for name, children in color_parts().items():
        (px, py), props, start, dur, kind = SPLASH[name]
        k = 0 if t_ms is None or t_ms < start else ease(kind, min((t_ms - start) / dur, 1))
        v = {p: a + (b - a) * k for p, (a, b) in props.items()}
        out.append(Group(children, px=px, py=py, rot=v.get("rotation", 0), sx=v.get("scaleX", 1), sy=v.get("scaleY", 1), name=name))
    return out


def avd():
    targets = []
    for name, (_, props, start, dur, kind) in SPLASH.items():
        anims = "".join(
            f'                <objectAnimator android:propertyName="{p}" android:valueFrom="{a}" android:valueTo="{b}"\n'
            f'                    android:startOffset="{start}" android:duration="{dur}" android:valueType="floatType"\n'
            f'                    android:interpolator="{INTERPOLATORS[kind]}" />\n' for p, (a, b) in props.items())
        targets.append(f'    <target android:name="{name}">\n        <aapt:attr name="android:animation">\n'
                       f'            <set>\n{anims}            </set>\n        </aapt:attr>\n    </target>\n')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            "<!-- Splash icon: the stem grows, the petals spin open, the face appears, the goals get checked, confetti bursts."
            " Generated by tools/launcher_icon.py: edit the script, not this file. -->\n"
            '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:drawable="@drawable/splash_icon_vector">\n'
            f"{''.join(targets)}</animated-vector>\n")


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)


if __name__ == "__main__":
    res, preview = sys.argv[1], sys.argv[2]
    write(f"{res}/drawable/ic_launcher_foreground.xml",
          vd(color_icon(), "Launcher icon: a happy flower (habit-colored petals) celebrating its checked goals."))
    write(f"{res}/drawable/ic_launcher_monochrome.xml", vd(mono_icon(), "Themed (monochrome) launcher icon: solid happy flower."))
    write(f"{res}/drawable/splash_icon_vector.xml", vd(splash_nodes(), "Splash icon, initial state: animated by splash_icon.xml."))
    write(f"{res}/drawable/splash_icon.xml", avd())
    color, mono = svg(color_icon(), BG), svg(mono_icon(), "#3A4A42")

    def cell(c, size, radius):
        return f'<div style="width:{size}px;height:{size}px;border-radius:{radius};overflow:hidden;display:inline-block;margin:6px">{c}</div>'

    icons = "".join(cell(c, size, r) for c in (color, mono) for size, r in ((220, "50%"), (120, "30%"), (48, "50%")))
    frames = "".join(cell(svg(splash_nodes(t), BG), 120, "50%") + f"<small>{t}</small>" for t in range(0, SPLASH_MS + 1, 100))
    write(preview, f'<!DOCTYPE html><html><body style="background:#F1F6F3;margin:16px;font-family:sans-serif">{icons}<div>{frames}</div></body></html>')
