import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uml import Diagram  # noqa: E402

out = sys.argv[1]
P = "EUK00361749_"
CORE, BUILDER, OBSERVER, STRATEGY, STATE, DECORATOR = ("#F4F6F9", "#E6F4EA", "#EFE7F7", "#FDF0DC", "#E3EEFA",
                                                      "#FBE6EC")
D = Diagram(5330, 3260, base=22)
d = D.d
d.text((40, 24), "FreshMart Grocery Delivery System - UML class diagram (Kingston ID prefix EUK00361749_)",
       font=D.f_name, fill="black")
d.text((40, 66), "Member types omit the EUK00361749_ prefix for space. + public  - private  # protected  ~ package; "
                 "underlined = static; italic = abstract. Colours mark the five design patterns (legend, bottom right).",
       font=D.f_label, fill="#333")

# ---------------------------------------------------------------- column 1: application, observer, accounts
D.box("main", 40, 130, P + "Main", fields=["- service : Service", "- notifier : CustomerNotifier",
                                            "- activityLog : OrderActivityLog", "- invoiceFolder : Path"],
      methods=[("+ main(args : String[]) : void", "u"), "- run() : void", "- administratorMenu(a : Account) : void",
               "- customerMenu(c : Account) : void", "- addProduct() / updateProduct() : void",
               "- placeOrder() : void", "- cancelOrder() : void", "- viewInvoice() : void",
               "- showNotifications(c) / showActivityLog()", "- readText / readInt / readDate / confirm"], width=860)
D.box("observer", 40, 820, P + "OrderObserver", stereotype="interface", pattern="Observer", fields=None,
      methods=[("+ orderStatusChanged(order : Order, previousState : OrderState) : void", "i")], fill=OBSERVER,
      width=860)
D.box("notifier", 40, 1080, P + "CustomerNotifier", pattern="ConcreteObserver", fill=OBSERVER,
      fields=["- messagesByCustomer : HashMap<String, List<String>>"],
      methods=["+ orderStatusChanged(order, previousState) : void", "+ collectMessages(nic : String) : List<String>",
               "+ countMessages(nic : String) : int"], width=860)
D.box("log", 40, 1420, P + "OrderActivityLog", pattern="ConcreteObserver", fill=OBSERVER,
      fields=["- clock : Clock", "- entries : List<String>"],
      methods=["+ orderStatusChanged(order, previousState) : void", "+ getEntries() : List<String>"], width=860)
D.box("account", 40, 1820, P + "Account", stereotype="abstract", abstract=True,
      fields=["- accountId : String", "- name : String", "- email : String", "- passwordHash : String"],
      methods=["# Account(accountId, name, email, passwordHash)", "+ getAccountId() / getName() / getEmail() : String",
               "~ getPasswordHash() : String", "+ checkPassword(password : char[]) : boolean",
               ("+ getRole() : String", "i"), ("+ getMenuTitle() : String", "i")], width=860)
D.box("admin", 40, 2330, P + "Admin", fields=[("+ ADMIN_ACCOUNT_ID : String = \"ADMIN\"", "u")],
      methods=["+ Admin(name, email, passwordHash)", "+ getRole() : String", "+ getMenuTitle() : String"], width=410)
D.box("customer", 490, 2330, P + "Customer", fields=["- contactNumber : String", "- deliveryAddress : String"],
      methods=["+ Customer(nic, name, contact,", "      address, email, hash)", "+ getContactNumber() : String",
               "+ getDeliveryAddress() : String", "+ getRole() / getMenuTitle()", "+ toString() : String"], width=410)
D.box("password", 40, 2800, P + "Password", stereotype="utility",
      fields=[("+ MINIMUM_LENGTH : int = 8", "u"), ("+ ITERATIONS : int", "u")],
      methods=[("+ hash(password : char[]) : String", "u"), ("+ verify(pw, hash) : boolean", "u")],
      width=410)
D.box("validation", 490, 2800, P + "Validation", stereotype="utility", fields=None,
      methods=[("+ requireText(v, field, max) : String", "u"), ("+ validateEmail(v) : String", "u"),
               ("+ validateNic(v) : String", "u"), ("+ validatePhone(v) : String", "u"),
               ("+ validateProductId(v) : String", "u"), ("+ fitWidth(text, width) : String", "u")], width=410)

# ---------------------------------------------------------------- column 2: service, store, sorter
D.box("service", 1000, 130, P + "Service", pattern="Subject (Observer)", fill=OBSERVER,
      fields=["- dataFile : Path", "- clock : Clock", "- pricingCatalogue : PricingCatalogue",
              "- billCalculator : BillCalculator", "- orderObservers : List<OrderObserver>", "- store : Store",
              "- loggedInAccountId : String"],
      methods=["+ addOrderObserver(o : OrderObserver) : void", "- notifyOrderObservers(order, previousState) : void",
               "+ setupAdministrator(name, email, password) : void", "+ registerCustomer(nic, name, phone, address,",
               "      email, password) : void", "+ login(email, password) : Account", "+ logout() : void",
               "+ updateProfile(...) / changePassword(...) : void", "+ addProduct(p) / updateProduct(p) : void",
               "+ updateStockQuantity(id, qty) : void", "+ updateStockStatus(id, status) : void",
               "+ removeProduct(id) : void", "+ productExists(id) : boolean",
               "+ viewInventory(c : Category) : List<Product>", "+ searchProducts(text) : List<Product>",
               "+ findOrderableProduct(id) : Product", "+ quoteOrder(basket, date, slot) : Invoice",
               "+ placeOrder(basket, date, slot) : Order", "+ viewOrderHistory() : List<Order>",
               "+ cancelOrder(orderId) : void", "+ advanceOrderStatus(orderId) : Order",
               "+ generateInvoice(orderId) : String", "- prepareOrder(basket, date, slot) : OrderBuilder",
               "- commitChanges(updated : Store) : void", "- generateNextOrderId() : String"], width=900)
D.box("store", 1000, 1650, P + "Store",
      fields=["- accountsById : LinkedHashMap<String, Account>", "- productsById : LinkedHashMap<String, Product>",
              "- ordersById : LinkedHashMap<String, Order>"],
      methods=["+ Store() / Store(original : Store)", "+ getAllAccounts / getAllProducts / getAllOrders : List",
               "+ findAccount / findProduct / findOrder(id)", "~ saveAccount / saveProduct / saveOrder(...)",
               "~ deleteProduct(id) : void", "+ saveToFile(file : Path) : void",
               ("+ loadFromFile(file : Path) : Store", "u")], width=900)
D.box("sorter", 40, 590, P + "Sorter", stereotype="utility", fields=None,
      methods=[("+ mergeSort(items : List<T>, order : Comparator<T>) : List<T>", "u"),
               ("- merge(left, right, order) : List<T>", "u")], width=900)

# ---------------------------------------------------------------- column 3: orders and products
D.box("order", 2000, 130, P + "Order", stereotype="record", pattern="Context (State)", fill=STATE,
      fields=["- orderId : String", "- customerId : String", "- customerDetails : String",
              "- deliveryDate : LocalDate", "- timeSlot : TimeSlot", "- state : OrderState",
              "- lines : List<OrderLine>"],
      methods=["+ advance() : Order", "+ cancel() : Order", "- withState(s : OrderState) : Order",
               "+ toString() : String"], width=780)
D.box("builder", 2000, 730, P + "Order." + P + "OrderBuilder", pattern="Builder", fill=BUILDER,
      fields=["- catalogue : PricingCatalogue", "- today : LocalDate",
              "- linesByProductId : LinkedHashMap<String, OrderLine>", "- customer : Customer",
              "- deliveryDate : LocalDate", "- timeSlot : TimeSlot"],
      methods=["+ OrderBuilder(catalogue, today)", "+ forCustomer(c : Customer) : OrderBuilder",
               "+ addProduct(p : Product, qty : int) : OrderBuilder", "+ deliverOn(date : LocalDate) : OrderBuilder",
               "+ inTimeSlot(slot : TimeSlot) : OrderBuilder", "+ validate() : List<OrderLine>",
               "+ build(orderId : String) : Order"], width=780)
D.box("line", 2000, 1400, P + "OrderLine", stereotype="record",
      fields=["- productId : String", "- name : String", "- category : Category", "- unitPrice : BigDecimal",
              "- quantity : int"],
      methods=[("+ fromProduct(p : Product, qty : int) : OrderLine", "u"), "+ lineTotal() : BigDecimal"], width=780)
D.box("product", 2000, 1830, P + "Product", stereotype="record",
      fields=[("+ LOW_STOCK_LIMIT : int = 5", "u"), "- productId : String", "- name : String",
              "- category : Category", "- unitPrice : BigDecimal", "- stockQuantity : int",
              "- stockStatus : StockStatus"],
      methods=[("+ statusForQuantity(qty : int) : StockStatus", "u"), "+ withStockQuantity(qty) : Product",
               "+ withStockStatus(s : StockStatus) : Product"], width=780)
D.box("category", 2000, 2380, P + "Category", stereotype="enumeration",
      fields=["VEGETABLES", "FRUITS", "DAIRY", "HOUSEHOLD_ESSENTIALS"], methods=["+ toString() : String"], width=370)
D.box("stock", 2410, 2380, P + "StockStatus", stereotype="enumeration",
      fields=["IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"], methods=["+ toString() : String"], width=370)
D.box("slot", 2410, 2730, P + "TimeSlot", stereotype="enumeration",
      fields=["MORNING_09_12", "AFTERNOON_12_16", "EVENING_16_20"], methods=["+ toString() : String"], width=370)

# ---------------------------------------------------------------- column 4: State pattern
D.box("state", 2900, 130, P + "OrderState", stereotype="abstract", pattern="State", fill=STATE, abstract=True,
      fields=["- code : String", "- displayName : String"],
      methods=["# OrderState(code, displayName)", "+ advance() : OrderState", "+ cancel() : OrderState",
               "+ isCancellable() : boolean", "+ isFinal() : boolean", "+ getCode() : String",
               ("+ fromCode(code : String) : OrderState", "u"), "+ equals / hashCode / toString"], width=640)
sy = 620
for key, cls, methods in [("placed", "PlacedState", ["+ advance() : OrderState", "+ cancel() : OrderState",
                                                     "+ isCancellable() : boolean"]),
                          ("preparing", "PreparingState", ["+ advance() : OrderState", "+ cancel() : OrderState",
                                                           "+ isCancellable() : boolean"]),
                          ("ofd", "OutForDeliveryState", ["+ advance() : OrderState"]),
                          ("delivered", "DeliveredState", ["+ isFinal() : boolean"]),
                          ("cancelled", "CancelledState", ["+ isFinal() : boolean"])]:
    box = D.box(key, 3040, sy, P + cls, pattern="ConcreteState", fill=STATE, fields=None, methods=methods, width=500)
    sy = box[3] + 40

# ---------------------------------------------------------------- column 5: Decorator
D.box("calc", 3680, 130, P + "BillCalculator", fill=DECORATOR, pattern="Decorator client",
      fields=["- catalogue : PricingCatalogue"],
      methods=["+ buildBill(lines : List<OrderLine>) : Bill", "+ calculate(lines : List<OrderLine>) : Invoice"],
      width=640)
D.box("bill", 3680, 470, P + "Bill", stereotype="interface", pattern="Component", fill=DECORATOR, fields=None,
      methods=[("+ getLines() : List<OrderLine>", "i"), ("+ getProductTotal() : BigDecimal", "i"),
               ("+ getDiscount() : BigDecimal", "i"), ("+ getDiscountFor(line) : BigDecimal", "i"),
               ("+ getDeliveryFee() : BigDecimal", "i"), ("+ getTax() : BigDecimal", "i"),
               ("+ getAmount() : BigDecimal", "i"), ("+ describeLayers() : String", "i")], width=640)
D.box("ptb", 3680, 1010, P + "ProductTotalBill", pattern="ConcreteComponent", fill=DECORATOR,
      fields=["- lines : List<OrderLine>", "- productTotal : BigDecimal"],
      methods=["+ getProductTotal() : BigDecimal", "+ getAmount() = product total", "+ (zero discount, fee, tax)"],
      width=640)
D.box("decorator", 3680, 1390, P + "BillDecorator", stereotype="abstract", pattern="Decorator", fill=DECORATOR,
      abstract=True, fields=["- wrappedBill : Bill", "- catalogue : PricingCatalogue"],
      methods=["# BillDecorator(wrapped : Bill, catalogue)", "# getWrappedBill() : Bill",
               "# strategyFor(line : OrderLine) : PricingStrategy", ("# getLayerName() : String", "i"),
               "+ all Bill methods delegate to wrappedBill"], width=640)
D.box("discount", 3680, 1890, P + "DiscountDecorator", pattern="ConcreteDecorator", fill=DECORATOR,
      fields=[("+ DISCOUNT_THRESHOLD : BigDecimal = 10000", "u"), "- discountByProductId : LinkedHashMap",
              "- totalDiscount : BigDecimal"],
      methods=["+ getDiscount() / getDiscountFor(line)", "+ getAmount() : wrapped - discount"], width=640)
D.box("delivery", 3680, 2280, P + "DeliveryFeeDecorator", pattern="ConcreteDecorator", fill=DECORATOR,
      fields=["- deliveryFee : BigDecimal"], methods=["+ getDeliveryFee() : BigDecimal",
                                                      "+ getAmount() : wrapped + fee"], width=640)
D.box("tax", 3680, 2590, P + "TaxDecorator", pattern="ConcreteDecorator", fill=DECORATOR,
      fields=["- tax : BigDecimal"], methods=["+ getTax() : BigDecimal", "+ getAmount() : wrapped + tax"], width=640)
D.box("invoice", 3680, 2930, P + "Invoice", stereotype="record",
      fields=["- lines : List<OrderLine>", "- productTotal, discount, deliveryFee,", "  tax, finalPayable : BigDecimal"],
      methods=[("+ fromBill(bill : Bill) : Invoice", "u"), "+ toDocument(order : Order) : String",
               "+ toString() : String", ("+ formatMoney(amount) : String", "u")], width=640)

# ---------------------------------------------------------------- column 6: Strategy
D.box("catalogue", 4520, 130, P + "PricingCatalogue", pattern="Strategy context", fill=STRATEGY,
      fields=["- strategies : EnumMap<Category, PricingStrategy>"],
      methods=[("+ standard() : PricingCatalogue", "u"), "+ strategyFor(c : Category) : PricingStrategy",
               "+ withStrategy(c, s) : PricingCatalogue", "+ requiredMinimum(lines) : BigDecimal"], width=760)
D.box("strategy", 4520, 560, P + "PricingStrategy", stereotype="interface", pattern="Strategy", fill=STRATEGY,
      fields=None, methods=[("+ getDeliveryFee() : BigDecimal", "i"), ("+ getMinimumOrderValue() : BigDecimal", "i"),
                            ("+ calculateDiscount(value : BigDecimal) : BigDecimal", "i"),
                            ("+ calculateTax(value : BigDecimal) : BigDecimal", "i"), ("+ describe() : String", "i")],
      width=760)
D.box("rate", 4520, 960, P + "RatePricingStrategy", stereotype="abstract", fill=STRATEGY, abstract=True,
      fields=["- deliveryFee : BigDecimal", "- minimumOrderValue : BigDecimal", "- discountRate : BigDecimal",
              "- taxRate : BigDecimal"],
      methods=["# RatePricingStrategy(fee, minimum, discount, tax)", "+ calculateDiscount(value) : BigDecimal",
               "+ calculateTax(value) : BigDecimal", "+ describe() : String",
               ("~ roundMoney(amount) : BigDecimal", "u")], width=760)
cy = 1530
for key, cls, values in [("veg", "VegetablesPricing", "300 / 2,000 / 5% / 5%"),
                         ("fruit", "FruitsPricing", "350 / 2,500 / 5% / 5%"),
                         ("dairy", "DairyPricing", "400 / 3,000 / 8% / 8%"),
                         ("house", "HouseholdEssentialsPricing", "500 / 4,000 / 10% / 10%")]:
    box = D.box(key, 4660, cy, P + cls, pattern="ConcreteStrategy", fill=STRATEGY, fields=None,
                methods=[f"+ {cls}()  [{values}]"], width=620)
    cy = box[3] + 36

exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "class_edges.py"), encoding="utf-8").read())
D.img.save(out)
print("saved", D.img.size)
