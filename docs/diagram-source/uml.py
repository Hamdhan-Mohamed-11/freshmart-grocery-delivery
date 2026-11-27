"""Small UML class-diagram renderer (boxes, standard relationship ends, multiplicities)."""
import math
from PIL import Image, ImageDraw, ImageFont

F = "C:/Windows/Fonts/"
S = 1.0  # global scale applied by caller through font sizes


def font(name, size):
    return ImageFont.truetype(F + name, size)


class Diagram:
    def __init__(self, width, height, base=22):
        self.img = Image.new("RGB", (width, height), "white")
        self.d = ImageDraw.Draw(self.img)
        self.base = base
        self.f_name = font("segoeuib.ttf", int(base * 1.15))
        self.f_name_i = font("segoeuiz.ttf", int(base * 1.15))
        self.f_stereo = font("segoeui.ttf", int(base * 0.9))
        self.f_member = font("consola.ttf", int(base * 0.86))
        self.f_member_u = font("consola.ttf", int(base * 0.86))
        self.f_member_i = font("consolai.ttf", int(base * 0.86))
        self.f_label = font("segoeui.ttf", int(base * 0.9))
        self.f_label_b = font("segoeuib.ttf", int(base * 0.95))
        self.boxes = {}

    # ------------------------------------------------------------ boxes
    def box(self, key, x, y, name, stereotype=None, fields=(), methods=(), width=None, fill="#F7F9FC",
            abstract=False, pattern=None):
        d, b = self.d, self.base
        line_h = int(b * 1.12)
        name_w = d.textlength(name, font=self.f_name)
        texts = [m if isinstance(m, str) else m[0] for m in list(fields or []) + list(methods)]
        member_w = max([d.textlength(t, font=self.f_member) for t in texts] + [0])
        stereo = " ".join(s for s in [stereotype and f"«{stereotype}»", pattern and f"«{pattern}»"] if s)
        stereo_w = d.textlength(stereo, font=self.f_stereo) if stereo else 0
        w = width or int(max(name_w, member_w, stereo_w) + b * 1.4)
        header_h = int(b * 1.7) + (int(b * 1.05) if stereo else 0)
        fields_h = max(1, len(fields)) * line_h + int(b * 0.5) if fields is not None else 0
        methods_h = max(1, len(methods)) * line_h + int(b * 0.5)
        h = header_h + fields_h + methods_h
        d.rectangle([x, y, x + w, y + h], fill=fill, outline="#2F3E4E", width=2)
        d.rectangle([x, y, x + w, y + header_h], fill=self._shade(fill), outline="#2F3E4E", width=2)
        ty = y + int(b * 0.35)
        if stereo:
            d.text((x + w / 2, ty), stereo, font=self.f_stereo, fill="#334", anchor="mt")
            ty += int(b * 1.05)
        d.text((x + w / 2, ty), name, font=self.f_name_i if abstract else self.f_name, fill="black", anchor="mt")
        yy = y + header_h
        if fields is not None:
            fy = yy + int(b * 0.25)
            for f in fields:
                self._member(x + int(b * 0.5), fy, f)
                fy += line_h
            yy += fields_h
            d.line([x, yy, x + w, yy], fill="#2F3E4E", width=2)
        my = yy + int(b * 0.25)
        for m in methods:
            self._member(x + int(b * 0.5), my, m)
            my += line_h
        self.boxes[key] = (x, y, x + w, y + h)
        return self.boxes[key]

    def _member(self, x, y, m):
        text, style = (m, "") if isinstance(m, str) else m
        f = self.f_member_i if "i" in style else self.f_member
        self.d.text((x, y), text, font=f, fill="black")
        if "u" in style:  # static members are underlined
            w = self.d.textlength(text, font=f)
            self.d.line([x, y + self.base * 0.95, x + w, y + self.base * 0.95], fill="black", width=1)

    @staticmethod
    def _shade(hex_colour):
        r, g, b = (int(hex_colour[i:i + 2], 16) for i in (1, 3, 5))
        return "#%02X%02X%02X" % (int(r * 0.9), int(g * 0.9), int(b * 0.9))

    def group(self, x1, y1, x2, y2, title, colour):
        self.d.rounded_rectangle([x1, y1, x2, y2], radius=18, outline=colour, width=3)
        self.d.text((x1 + 16, y1 + 6), title, font=self.f_label_b, fill=colour)

    # ------------------------------------------------------------ anchors
    def at(self, key, side, frac=0.5):
        x1, y1, x2, y2 = self.boxes[key]
        if side == "t":
            return (x1 + (x2 - x1) * frac, y1)
        if side == "b":
            return (x1 + (x2 - x1) * frac, y2)
        if side == "l":
            return (x1, y1 + (y2 - y1) * frac)
        return (x2, y1 + (y2 - y1) * frac)

    # ------------------------------------------------------------ edges
    def edge(self, points, kind="assoc", src_mult=None, dst_mult=None, label=None, label_at=None,
             colour="#1F2A36"):
        """points run from source to target. kind decides line style and the ends:
        inherit (solid, hollow triangle at target), realize (dashed, hollow triangle at target),
        assoc (solid, open arrow at target), compose (solid, filled diamond at SOURCE = whole),
        aggregate (hollow diamond at source), depend (dashed, open arrow at target), nest (circle-plus at source)."""
        dashed = kind in ("realize", "depend")
        for a, b in zip(points, points[1:]):
            self._line(a, b, dashed, colour)
        end, before = points[-1], points[-2]
        start, after = points[0], points[1]
        if kind in ("inherit", "realize"):
            self._triangle(before, end, colour)
        if kind in ("assoc", "depend", "compose", "aggregate"):
            self._arrow(before, end, colour)
        if kind == "compose":
            self._diamond(after, start, colour, filled=True)
        if kind == "aggregate":
            self._diamond(after, start, colour, filled=False)
        if kind == "nest":
            self._circle_plus(after, start, colour)
        if src_mult:
            self._mult(start, after, src_mult)
        if dst_mult:
            self._mult(end, before, dst_mult)
        if label:
            lx, ly = label_at if label_at else ((points[0][0] + points[1][0]) / 2, (points[0][1] + points[1][1]) / 2)
            w = self.d.textlength(label, font=self.f_label)
            self.d.rectangle([lx - w / 2 - 4, ly - self.base * 0.6, lx + w / 2 + 4, ly + self.base * 0.6], fill="white")
            self.d.text((lx, ly), label, font=self.f_label, fill="#333", anchor="mm")

    def _line(self, a, b, dashed, colour):
        if not dashed:
            self.d.line([a, b], fill=colour, width=3)
            return
        length = math.dist(a, b)
        if length == 0:
            return
        dx, dy = (b[0] - a[0]) / length, (b[1] - a[1]) / length
        pos = 0
        while pos < length:
            e = min(pos + 14, length)
            self.d.line([(a[0] + dx * pos, a[1] + dy * pos), (a[0] + dx * e, a[1] + dy * e)], fill=colour, width=3)
            pos += 24

    @staticmethod
    def _unit(a, b):
        length = math.dist(a, b) or 1
        return (b[0] - a[0]) / length, (b[1] - a[1]) / length

    def _triangle(self, before, tip, colour):
        ux, uy = self._unit(before, tip)
        size = 26
        bx, by = tip[0] - ux * size, tip[1] - uy * size
        px, py = -uy * size * 0.6, ux * size * 0.6
        self.d.polygon([tip, (bx + px, by + py), (bx - px, by - py)], fill="white", outline=colour, width=3)

    def _arrow(self, before, tip, colour):
        ux, uy = self._unit(before, tip)
        size = 20
        bx, by = tip[0] - ux * size, tip[1] - uy * size
        px, py = -uy * size * 0.55, ux * size * 0.55
        self.d.line([(bx + px, by + py), tip, (bx - px, by - py)], fill=colour, width=3)

    def _diamond(self, after, tip, colour, filled):
        ux, uy = self._unit(after, tip)  # points from the line towards the box
        size = 30
        mx, my = tip[0] - ux * size / 2, tip[1] - uy * size / 2
        back = (tip[0] - ux * size, tip[1] - uy * size)
        px, py = -uy * size * 0.35, ux * size * 0.35
        self.d.polygon([tip, (mx + px, my + py), back, (mx - px, my - py)], fill=colour if filled else "white",
                       outline=colour, width=3)

    def _circle_plus(self, after, tip, colour):
        ux, uy = self._unit(after, tip)
        r = 12
        cx, cy = tip[0] - ux * r, tip[1] - uy * r
        self.d.ellipse([cx - r, cy - r, cx + r, cy + r], fill="white", outline=colour, width=3)
        self.d.line([cx - r, cy, cx + r, cy], fill=colour, width=2)
        self.d.line([cx, cy - r, cx, cy + r], fill=colour, width=2)

    def _mult(self, end, towards, text):
        ux, uy = self._unit(end, towards)
        # place the text a little along the line and offset to its side
        px, py = -uy, ux
        x = end[0] + ux * 40 + px * 22
        y = end[1] + uy * 40 + py * 22
        self.d.text((x, y), text, font=self.f_label, fill="#7A1F1F", anchor="mm")

    def note(self, x, y, lines, width=None):
        b = self.base
        w = width or int(max(self.d.textlength(l, font=self.f_label) for l in lines) + b * 1.4)
        h = int(len(lines) * b * 1.2 + b * 0.9)
        fold = 18
        self.d.polygon([(x, y), (x + w - fold, y), (x + w, y + fold), (x + w, y + h), (x, y + h)], fill="#FFF8D6",
                       outline="#8A7A2A", width=2)
        for i, l in enumerate(lines):
            self.d.text((x + b * 0.6, y + b * 0.45 + i * b * 1.2), l, font=self.f_label, fill="#333")
        return (x, y, x + w, y + h)
