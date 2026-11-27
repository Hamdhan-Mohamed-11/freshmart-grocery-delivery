# Relationships of the FreshMart class diagram (executed inside class_diagram.py; D and boxes exist).
B = D.boxes


def x_mid(k):
    return (B[k][0] + B[k][2]) / 2


def y_mid(k):
    return (B[k][1] + B[k][3]) / 2


# ---- accounts (inheritance)
D.edge([(245, B["admin"][1]), (245, B["account"][3])], "inherit")
D.edge([(695, B["customer"][1]), (695, B["account"][3])], "inherit")

# ---- Main composes the two observers and uses the service
D.edge([(900, 200), (1000, 200)], "assoc", src_mult="1", dst_mult="1")
D.edge([(900, 470), (930, 470), (930, 1150), (900, 1150)], "compose", dst_mult="1")
D.edge([(900, 505), (962, 505), (962, 1490), (900, 1490)], "compose", dst_mult="1")
D.edge([(470, B["notifier"][1]), (470, B["observer"][3])], "realize")
D.edge([(40, 1510), (18, 1510), (18, 868), (40, 868)], "realize")
D.edge([(1000, 650), (900, 650)], "depend", label="uses", label_at=(950, 628))

# ---- Service relationships
D.edge([(1000, 880), (900, 880)], "aggregate", dst_mult="0..*", label="notifies", label_at=(950, 902))
D.edge([(1450, B["service"][3]), (1450, B["store"][1])], "compose", dst_mult="1", label="commits copies",
       label_at=(1450, 1300))
D.edge([(1900, 820), (2000, 820)], "depend", label="«create»", label_at=(1950, 798))
D.edge([(1760, 130), (1760, 110), (4000, 110), (4000, 130)], "compose", dst_mult="1",
       label="owns", label_at=(3000, 110))
D.edge([(1820, 130), (1820, 94), (4900, 94), (4900, 130)], "assoc", dst_mult="1",
       label="injected pricing strategies", label_at=(4300, 94))

# ---- Store aggregates records
D.edge([(1000, 1880), (900, 1880)], "aggregate", src_mult="1", dst_mult="0..*")
D.edge([(1900, 1900), (2000, 1900)], "aggregate", src_mult="1", dst_mult="0..*")
D.edge([(1900, 1690), (1950, 1690), (1950, 350), (2000, 350)], "aggregate", src_mult="1", dst_mult="0..*")

# ---- Builder
D.edge([(2390, B["order"][3]), (2390, B["builder"][1])], "nest", label="nested", label_at=(2390, 600))
D.edge([(2650, B["builder"][1]), (2650, B["order"][3])], "depend", label="«create»", label_at=(2650, 600))
D.edge([(2000, 1100), (1975, 1100), (1975, 2640), (695, 2640), (695, B["customer"][3])], "assoc",
       src_mult="", dst_mult="0..1", label="for customer", label_at=(1300, 2640))

# ---- Order composition and associations
D.edge([(2780, 440), (2840, 440), (2840, 1450), (2780, 1450)], "compose", dst_mult="1..*")
D.edge([(2780, 250), (2900, 250)], "assoc", dst_mult="1", label="state", label_at=(2840, 228))
D.edge([(2780, 410), (2872, 410), (2872, 2800), (2780, 2800)], "assoc", dst_mult="1")
D.edge([(2390, B["line"][3]), (2390, B["product"][1])], "depend", label="copied from", label_at=(2390, 1740))
D.edge([(2185, B["product"][3]), (2185, B["category"][1])], "assoc", dst_mult="1")
D.edge([(2595, B["product"][3]), (2595, B["stock"][1])], "assoc", dst_mult="1")

# ---- State hierarchy (one trunk)
trunk_x = 2990
D.edge([(trunk_x, y_mid("cancelled")), (trunk_x, B["state"][3])], "inherit")
for k in ["placed", "preparing", "ofd", "delivered", "cancelled"]:
    D.d.line([(trunk_x, y_mid(k)), (B[k][0], y_mid(k))], fill="#1F2A36", width=3)

# ---- Decorator
D.edge([(4000, B["calc"][3]), (4000, B["bill"][1])], "depend", label="«create» layers", label_at=(4000, 377))
D.edge([(3900, B["ptb"][1]), (3900, B["bill"][3])], "realize")
D.edge([(4320, 1420), (4362, 1420), (4362, 690), (4320, 690)], "realize")
D.edge([(3680, 1520), (3620, 1520), (3620, 600), (3680, 600)], "aggregate", dst_mult="1", label="wraps",
       label_at=(3620, 1060))
D.edge([(4000, B["discount"][1]), (4000, B["decorator"][3])], "inherit")
D.edge([(4392, y_mid("tax")), (4392, 1600), (4320, 1600)], "inherit")
for k in ["delivery", "tax"]:
    D.d.line([(B[k][2], y_mid(k)), (4392, y_mid(k))], fill="#1F2A36", width=3)
D.edge([(4320, 1460), (4430, 1460), (4430, 300), (4520, 300)], "assoc", dst_mult="1")
D.edge([(4320, 200), (4520, 200)], "assoc", dst_mult="1")
D.edge([(4320, 262), (4470, 262), (4470, 3050), (4320, 3050)], "depend", label="«create»", label_at=(4470, 2850))

# ---- Strategy
D.edge([(4900, B["catalogue"][3]), (4900, B["strategy"][1])], "aggregate", dst_mult="1..*",
       label="one per Category (EnumMap)", label_at=(4900, 450))
D.edge([(4900, B["rate"][1]), (4900, B["strategy"][3])], "realize")
D.edge([(4590, y_mid("house")), (4590, B["rate"][3])], "inherit")
for k in ["veg", "fruit", "dairy", "house"]:
    D.d.line([(4590, y_mid(k)), (B[k][0], y_mid(k))], fill="#1F2A36", width=3)

# ---- notes and legend
D.note(1000, 2080, [
    "Design patterns in this diagram",
    "Builder: Order.OrderBuilder assembles and validates an Order step by step.",
    "Observer: Service (subject) notifies OrderObserver objects (CustomerNotifier, OrderActivityLog).",
    "Strategy: PricingCatalogue maps each Category to a PricingStrategy (Table 1 rates).",
    "State: Order delegates advance()/cancel() to its OrderState object.",
    "Decorator: Tax(DeliveryFee(Discount(ProductTotalBill))) builds the bill in the brief's order.",
], width=880)
lx, ly = 4520, 2200
D.d.rounded_rectangle([lx, ly, 5300, ly + 730], radius=14, outline="#555", width=2, fill="white")
D.d.text((lx + 20, ly + 14), "Legend", font=D.f_label_b, fill="black")
for i, (colour, text) in enumerate([("#E6F4EA", "Builder"), ("#EFE7F7", "Observer"), ("#FDF0DC", "Strategy"),
                                     ("#E3EEFA", "State"), ("#FBE6EC", "Decorator"), ("#F4F6F9", "Other classes")]):
    y = ly + 56 + i * 38
    D.d.rectangle([lx + 20, y, lx + 60, y + 26], fill=colour, outline="#2F3E4E", width=2)
    D.d.text((lx + 74, y + 13), text, font=D.f_label, fill="black", anchor="lm")
samples = [("inherit", "inheritance (extends)"), ("realize", "realisation (implements)"),
           ("assoc", "navigable association"), ("compose", "composition (whole owns part)"),
           ("aggregate", "aggregation (whole holds part)"), ("depend", "dependency / «create»"),
           ("nest", "nested (static inner) class")]
for i, (kind, text) in enumerate(samples):
    y = ly + 300 + i * 52
    D.edge([(lx + 150, y), (lx + 30, y)] if kind in ("compose", "aggregate", "nest") else [(lx + 30, y), (lx + 150, y)],
           kind)
    D.d.text((lx + 180, y), text, font=D.f_label, fill="black", anchor="lm")
D.d.text((lx + 20, ly + 680), "Red labels are multiplicities.", font=D.f_label, fill="#7A1F1F")
