"""Draws the FreshMart main-algorithm flow chart (two pages joined by off-page connector A)."""
import sys
from PIL import Image, ImageDraw, ImageFont

out_dir = sys.argv[1]
FONT = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 30)
FONT_B = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 30)
SMALL = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 28)
INK = "#1F2A36"
FILL = {"term": "#DDEFE3", "process": "#EAF1FB", "decision": "#FFF4D6", "io": "#F3E9FA", "pre": "#E8F4F4",
        "conn": "#FFFFFF", "offpage": "#FFFFFF"}


class Chart:
    def __init__(self, w, h, title):
        self.img = Image.new("RGB", (w, h), "white")
        self.d = ImageDraw.Draw(self.img)
        self.n = {}
        self.io = set()
        self.d.text((40, 24), title, font=FONT_B, fill="black")

    def wrap(self, text, width):
        words, lines, cur = text.split(), [], ""
        for w in words:
            t = (cur + " " + w).strip()
            if self.d.textlength(t, font=FONT) <= width:
                cur = t
            else:
                lines.append(cur)
                cur = w
        lines.append(cur)
        return lines

    def node(self, key, kind, cx, cy, text, w=560, h=None):
        d = self.d
        inner = w - (180 if kind == "decision" else 70)
        lines = self.wrap(text, inner)
        th = len(lines) * 38
        if h is None:
            h = th + (110 if kind == "decision" else 50)
            if kind == "decision":
                h = max(h, 200)
        x1, y1, x2, y2 = cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2
        f = FILL[kind]
        if kind == "term":
            d.rounded_rectangle([x1, y1, x2, y2], radius=h / 2, fill=f, outline=INK, width=4)
        elif kind == "process":
            d.rectangle([x1, y1, x2, y2], fill=f, outline=INK, width=4)
        elif kind == "pre":
            d.rectangle([x1, y1, x2, y2], fill=f, outline=INK, width=4)
            d.line([x1 + 26, y1, x1 + 26, y2], fill=INK, width=4)
            d.line([x2 - 26, y1, x2 - 26, y2], fill=INK, width=4)
        elif kind == "io":
            s = 40
            d.polygon([(x1 + s, y1), (x2, y1), (x2 - s, y2), (x1, y2)], fill=f, outline=INK, width=4)
        elif kind == "decision":
            d.polygon([(cx, y1), (x2, cy), (cx, y2), (x1, cy)], fill=f, outline=INK, width=4)
        elif kind == "conn":
            d.ellipse([x1, y1, x2, y2], fill=f, outline=INK, width=4)
        elif kind == "offpage":
            d.polygon([(x1, y1), (x2, y1), (x2, y2 - h * 0.35), (cx, y2), (x1, y2 - h * 0.35)], fill=f, outline=INK,
                      width=4)
        ty = cy - th / 2 - (8 if kind == "offpage" else 0)
        for line in lines:
            d.text((cx, ty + 19), line, font=FONT_B if kind in ("term", "conn", "offpage") else FONT, fill="black",
                   anchor="mm")
            ty += 38
        self.n[key] = (x1, y1, x2, y2)
        if kind == "io":
            self.io.add(key)

    def p(self, key, side):
        x1, y1, x2, y2 = self.n[key]
        if key in self.io and side in "lr":
            x1, x2 = x1 + 20, x2 - 20
        return {"t": ((x1 + x2) / 2, y1), "b": ((x1 + x2) / 2, y2), "l": (x1, (y1 + y2) / 2),
                "r": (x2, (y1 + y2) / 2)}[side]

    def flow(self, points, label=None, label_pos=None):
        d = self.d
        d.line(points, fill=INK, width=4, joint="curve")
        (ax, ay), (bx, by) = points[-2], points[-1]
        if ax == bx:
            s = 1 if by > ay else -1
            d.polygon([(bx, by), (bx - 13, by - 26 * s), (bx + 13, by - 26 * s)], fill=INK)
        else:
            s = 1 if bx > ax else -1
            d.polygon([(bx, by), (bx - 26 * s, by - 13), (bx - 26 * s, by + 13)], fill=INK)
        if label:
            x, y = label_pos or (points[0][0] + 14, points[0][1] + 20)
            d.text((x, y), label, font=SMALL, fill="#8A1C1C")

    def down(self, a, b, label=None):
        pa, pb = self.p(a, "b"), self.p(b, "t")
        self.flow([pa, (pa[0], pb[1])], label, (pa[0] + 14, pa[1] + 6) if label else None)

    def right(self, a, b, label="No"):
        pa, pb = self.p(a, "r"), self.p(b, "l")
        self.flow([pa, (pb[0], pa[1])], label, (pa[0] + 16, pa[1] - 42))

    def save(self, name):
        self.img.save(f"{out_dir}/{name}.png")
        print(name, self.img.size)


MAIN, RIGHT, LEFT = 900, 1610, 300

# ============================================================ Part 1
c = Chart(1960, 3330, "FreshMart main algorithm, part 1 of 2 - basket entry and order validation (Builder)")
c.node("start", "term", MAIN, 150, "START: customer chooses 'Place a delivery order'", w=640)
c.node("list", "io", MAIN, 300, "Output: product list with stock status")
c.node("pid", "io", MAIN, 460, "Input: product ID (empty line = finish basket)")
c.node("blank", "decision", MAIN, 670, "Input empty?", w=480)
c.node("exists", "decision", MAIN, 920, "Product ID exists?", w=480)
c.node("instock", "decision", MAIN, 1170, "Stock status = In Stock?", w=520)
c.node("dup", "decision", MAIN, 1420, "Already in basket?", w=480)
c.node("qty", "io", MAIN, 1630, "Input: quantity 1 to units in stock (re-asked until valid)")
c.node("add", "process", MAIN, 1790, "Add product ID and quantity to basket")
c.node("m1", "io", LEFT, 920, "Output: 'Product not found'", w=420)
c.node("m2", "io", LEFT, 1170, "Output: 'only In Stock products can be ordered'", w=420)
c.node("m3", "io", LEFT, 1420, "Output: 'already in the basket'", w=420)
c.node("empty", "decision", MAIN, 2000, "Basket empty?", w=480)
c.node("e0", "io", RIGHT, 2000, "Output: 'basket is empty; no order was placed'", w=480)
c.node("t0", "term", RIGHT, 2190, "Return to customer menu", w=480)
c.node("date", "io", MAIN, 2200, "Input: delivery date YYYY-MM-DD and time slot 1-3 (format re-asked)")
c.node("build", "pre", MAIN, 2400, "OrderBuilder: forCustomer, addProduct (each line), deliverOn, inTimeSlot; then validate()",
       w=620)
c.node("future", "decision", MAIN, 2650, "Delivery date after today (Asia/Colombo)?", w=560)
c.node("e1", "io", RIGHT, 2650, "Output error: same-day or past date not allowed", w=480)
c.node("minimum", "decision", MAIN, 2930, "Product total >= highest minimum order value of its categories?", w=600)
c.node("e2", "io", RIGHT, 2930, "Output error: minimum order value LKR x", w=480)
c.node("t1", "term", RIGHT, 3150, "Return to customer menu (no order created)", w=480)
c.node("A", "offpage", MAIN, 3190, "A", w=110, h=110)

c.down("start", "list")
c.down("list", "pid")
c.down("pid", "blank")
c.down("blank", "exists", "No")
c.down("exists", "instock", "Yes")
c.down("instock", "dup", "Yes")
c.down("dup", "qty", "No")
c.down("qty", "add")
for dec, msg, lab in [("exists", "m1", "No"), ("instock", "m2", "No"), ("dup", "m3", "Yes")]:
    pa, pb = c.p(dec, "l"), c.p(msg, "r")
    c.flow([pa, (pb[0], pa[1])], lab, (pb[0] + 16, pa[1] - 42))
# loop-back channel on the far left
ch = 50
for msg in ["m1", "m2", "m3"]:
    pa = c.p(msg, "l")
    c.d.line([pa, (ch, pa[1])], fill=INK, width=4)
pa = c.p("add", "l")
c.d.line([pa, (ch, pa[1])], fill=INK, width=4)
c.d.line([(ch, pa[1]), (ch, 460)], fill=INK, width=4)
c.flow([(ch, 460), c.p("pid", "l")])
c.d.text((ch + 12, 1700), "next product", font=SMALL, fill="#555")
# exit of the loop: blank -> basket empty?
pa = c.p("blank", "r")
c.flow([pa, (1270, pa[1]), (1270, 1880), (MAIN, 1880), (MAIN, c.n["empty"][1])], "Yes", (pa[0] + 16, pa[1] - 42))
c.right("empty", "e0", "Yes")
c.down("e0", "t0")
c.down("empty", "date", "No")
c.down("date", "build")
c.down("build", "future")
c.right("future", "e1", "No")
c.down("future", "minimum", "Yes")
c.right("minimum", "e2", "No")
c.down("e1", "e2") if False else None
pa, pb = c.p("e1", "r"), c.p("t1", "r")
c.flow([pa, (1915, pa[1]), (1915, pb[1]), pb])
c.down("e2", "t1")
c.down("minimum", "A", "Yes")
c.save("flowchart-1")

# ============================================================ Part 2
c = Chart(1960, 3330, "FreshMart main algorithm, part 2 of 2 - layered bill, confirmation, saving and invoice")
c.node("A", "offpage", MAIN, 150, "A", w=110, h=110)
c.node("s1", "process", MAIN, 330, "Step 1 ProductTotalBill: product total = sum of (unit price x quantity)", w=640)
c.node("thr", "decision", MAIN, 560, "Product total > LKR 10,000?", w=520)
c.node("disc", "process", RIGHT, 560, "DiscountDecorator: each line discount = line total x its category discount rate (Strategy)",
       w=560)
c.node("nodisc", "process", LEFT, 560, "Discount = 0", w=300)
c.node("s3", "process", MAIN, 800, "Step 3 DeliveryFeeDecorator: add the highest delivery fee of the order's categories",
       w=640)
c.node("s4", "process", MAIN, 990, "Step 4 TaxDecorator: tax = sum of (line total - line discount) x category tax rate",
       w=640)
c.node("s5", "process", MAIN, 1170, "Step 5: final payable = product total - discount + delivery fee + tax", w=640)
c.node("prev", "io", MAIN, 1340, "Output: ORDER PREVIEW (lines and the five amounts)", w=640)
c.node("conf", "io", MAIN, 1490, "Input: 'Place this order? (Y/N)'", w=560)
c.node("yes", "decision", MAIN, 1680, "Answer = Y?", w=420)
c.node("n1", "io", RIGHT, 1680, "Output: 'Order not placed; stock is unchanged'", w=480)
c.node("tn", "term", RIGHT, 1870, "Return to customer menu", w=480)
c.node("bld", "process", MAIN, 1890, "build(ORDnnnn): validate again; initial state = PlacedState", w=640)
c.node("stock", "process", MAIN, 2060, "Copy the store; reduce each product's stock by its ordered quantity", w=640)
c.node("saved", "decision", MAIN, 2280, "Copy written to the data file?", w=520)
c.node("n2", "io", RIGHT, 2280, "Output: storage error; live data unchanged", w=480)
c.node("ts", "term", RIGHT, 2470, "Return to customer menu", w=480)
c.node("live", "process", MAIN, 2490, "Saved copy becomes the live store", w=560)
c.node("obs", "pre", MAIN, 2650, "notifyOrderObservers: customer inbox and activity log (Observer)", w=640)
c.node("done", "io", MAIN, 2820, "Output: 'Order placed. Your order ID is ORDnnnn'", w=640)
c.node("inv", "io", MAIN, 2990, "Output on request: invoice with customer, product lines and five amounts", w=640)
c.node("end", "term", MAIN, 3170, "END", w=300)

c.down("A", "s1")
c.down("s1", "thr")
pa, pb = c.p("thr", "r"), c.p("disc", "l")
c.flow([pa, (pb[0], pa[1])], "Yes", (pa[0] + 16, pa[1] - 42))
pa, pb = c.p("thr", "l"), c.p("nodisc", "r")
c.flow([pa, (pb[0], pa[1])], "No", (pb[0] + 16, pa[1] - 42))
for k in ["disc", "nodisc"]:
    pa = c.p(k, "b")
    c.d.line([pa, (pa[0], 700)], fill=INK, width=4)
c.d.line([(LEFT, 700), (RIGHT, 700)], fill=INK, width=4)
c.flow([(MAIN, 700), c.p("s3", "t")])
c.d.text((MAIN + 12, 650), "Step 2 done", font=SMALL, fill="#555")
for a, b in [("s3", "s4"), ("s4", "s5"), ("s5", "prev"), ("prev", "conf"), ("conf", "yes")]:
    c.down(a, b)
c.right("yes", "n1", "No")
c.down("n1", "tn")
c.down("yes", "bld", "Yes")
c.down("bld", "stock")
c.down("stock", "saved")
c.right("saved", "n2", "No")
c.down("n2", "ts")
c.down("saved", "live", "Yes")
for a, b in [("live", "obs"), ("obs", "done"), ("done", "inv"), ("inv", "end")]:
    c.down(a, b)
c.save("flowchart-2")
