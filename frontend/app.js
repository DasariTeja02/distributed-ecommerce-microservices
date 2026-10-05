/**
 * Distributed E-Commerce Microservices — Interactive Control Center Application Logic
 *
 * Centralized Control Center Architecture:
 * - Centralized SERVICE_CONFIG as Single Source of Truth
 * - Dynamic Actuator Health Polling & Latency Measurement (ms)
 * - Clickable Service Mesh Cards (Open Service & Swagger)
 * - Observability Dashboards (Prometheus, Grafana, Zipkin)
 * - Real API Gateway (:8080) Routing & JWT/RBAC Enforcement
 * - Real PostgreSQL & MongoDB Persistence
 * - Real Server-Sent Events (SSE) & Apache Kafka Event Stream
 */

const CONFIG = window.SERVICE_CONFIG || {
  gateway: { url: "http://localhost:8080", port: 8080 }
};

const API_BASE = CONFIG.gateway ? CONFIG.gateway.url : "http://localhost:8080";

// Catalog emoji lookup
const EMOJI_MAP = {
  "ELECTRONICS": "⚡",
  "FASHION": "👕",
  "HOME": "🏡",
  "ACCESSORIES": "🔌",
  "BEAUTY": "💄",
  "SPORTS": "🏃",
  "BOOKS": "📚",
  "GROCERY": "🍎",
  "AUTOMOTIVE": "🚗",
  "TRAVEL": "✈️",
  "LAPTOPS": "💻",
  "SMARTPHONES": "📱",
  "AUDIO": "🎧",
  "WEARABLES": "⌚"
};

// Centralized Currency & Pricing Utility (Requirement 5)
const CURRENCY = (window.SERVICE_CONFIG && window.SERVICE_CONFIG.currency) || {
  code: "INR",
  symbol: "₹",
  usdToInrRate: 83.5,
  format: function(amount) {
    if (amount === null || amount === undefined || isNaN(amount)) return "₹0";
    return "₹" + Math.round(Number(amount)).toLocaleString("en-IN");
  }
};

// Application State
const state = {
  products: [
        {
            "id": "prod-101",
            "name": "Apple MacBook Pro 16\" M3 Max",
            "category": "ELECTRONICS",
            "price": 349900,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "16-core CPU, 40-core GPU, 48GB Unified Memory, 1TB SSD Storage.",
            "imageUrl": "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-102",
            "name": "Dell XPS 15 OLED Touch",
            "category": "ELECTRONICS",
            "price": 219900,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Intel Core i9-13900H, 32GB RAM, 1TB SSD, 3.5K OLED InfinityEdge.",
            "imageUrl": "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-103",
            "name": "Samsung Galaxy S24 Ultra",
            "category": "ELECTRONICS",
            "price": 129999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Snapdragon 8 Gen 3, 200MP Camera, Galaxy AI, Titanium Gray, 512GB.",
            "imageUrl": "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-104",
            "name": "Sony WH-1000XM5 Wireless Headphones",
            "category": "ELECTRONICS",
            "price": 29990,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Industry-leading active noise canceling with Auto NC Optimizer, 30h battery.",
            "imageUrl": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-105",
            "name": "Apple Watch Ultra 2 Titanium",
            "category": "ELECTRONICS",
            "price": 89900,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Rugged 49mm titanium case, precision dual-frequency GPS, 100m water resistant.",
            "imageUrl": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-106",
            "name": "Bose QuietComfort Ultra",
            "category": "ELECTRONICS",
            "price": 35900,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Spatial audio with CustomTune sound calibration and world-class noise cancellation.",
            "imageUrl": "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-107",
            "name": "OnePlus 12 5G Smartphone",
            "category": "ELECTRONICS",
            "price": 64999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Snapdragon 8 Gen 3, 16GB RAM, 512GB ROM, 5400mAh Battery with 100W SUPERVOOC.",
            "imageUrl": "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-108",
            "name": "JBL Charge 5 Portable Bluetooth Speaker",
            "category": "ELECTRONICS",
            "price": 14999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Bold JBL Original Pro Sound, IP67 waterproof and dustproof, 20 hours of playtime.",
            "imageUrl": "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-109",
            "name": "Keychron K2 Wireless Mechanical Keyboard",
            "category": "ELECTRONICS",
            "price": 8499,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "75% layout, Gateron G Pro mechanical switches, RGB backlighting, Mac/Windows layout.",
            "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-110",
            "name": "Logitech MX Master 3S Wireless Mouse",
            "category": "ELECTRONICS",
            "price": 9995,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Quiet clicks, 8K DPI any-surface tracking, MagSpeed electromagnetic scrolling.",
            "imageUrl": "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-111",
            "name": "LG UltraFine 27\" 4K IPS Monitor",
            "category": "ELECTRONICS",
            "price": 32999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "UHD 3840x2160, HDR400, USB-C 90W PD, DCI-P3 95% color gamut with ergonomic stand.",
            "imageUrl": "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-112",
            "name": "Anker 737 24,000mAh Power Bank (140W)",
            "category": "ELECTRONICS",
            "price": 11999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💻",
            "description": "Ultra-powerful 140W two-way fast charging, smart digital display, GaNPrime technology.",
            "imageUrl": "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-113",
            "name": "Classic Heavyweight Organic Cotton T-Shirt",
            "category": "FASHION",
            "price": 999,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "240 GSM 100% combed organic cotton, pre-shrunk, breathable tailored regular fit.",
            "imageUrl": "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-114",
            "name": "Urban Streetwear Oversized Fleece Hoodie",
            "category": "FASHION",
            "price": 2499,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "400 GSM brushed fleece, double-layered hood, kangaroo pocket, relaxed dropped shoulders.",
            "imageUrl": "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-115",
            "name": "Slim Fit Stretch Selvedge Denim Jeans",
            "category": "FASHION",
            "price": 3299,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "Japanese 13.5oz stretch selvedge denim, deep indigo dye, reinforced copper rivets.",
            "imageUrl": "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-116",
            "name": "Nike Air Max 270 Athletic Sneakers",
            "category": "FASHION",
            "price": 12995,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "Large Max Air unit for responsive cushioning, breathable engineered mesh upper.",
            "imageUrl": "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-117",
            "name": "Waterproof Everyday Commuter Backpack (25L)",
            "category": "FASHION",
            "price": 4499,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "Ballistic nylon construction, dedicated 16\" padded laptop compartment, luggage pass-through.",
            "imageUrl": "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-118",
            "name": "Premium Pure Linen Casual Button-Down Shirt",
            "category": "FASHION",
            "price": 1999,
            "stock": 25,
            "reserved": 0,
            "emoji": "👕",
            "description": "100% European flax linen, garment-washed for extreme softness, lightweight resort collar.",
            "imageUrl": "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-119",
            "name": "Minimalist Smart LED Desk Lamp",
            "category": "HOME",
            "price": 2999,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏡",
            "description": "Touch dimmable, 5 color temperature modes, 10W wireless charging base, flicker-free CRI 95.",
            "imageUrl": "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-120",
            "name": "Stainless Steel Vacuum Insulated Bottle (1L)",
            "category": "HOME",
            "price": 1299,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏡",
            "description": "Double-wall 18/8 food-grade stainless steel, keeps cold for 24h and hot for 12h, sweat-proof.",
            "imageUrl": "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-121",
            "name": "Handcrafted Matte Ceramic Coffee Mug (350ml)",
            "category": "HOME",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏡",
            "description": "Artisanal stoneware ceramic, ergonomic handle, microwave and dishwasher safe.",
            "imageUrl": "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-122",
            "name": "Solid Walnut Wood Desk Organizer Tray",
            "category": "HOME",
            "price": 2199,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏡",
            "description": "Precision-carved natural American walnut wood, phone dock, cable groove, felt bottom.",
            "imageUrl": "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-123",
            "name": "Ambient Smart RGBIC LED Light Bar Set",
            "category": "HOME",
            "price": 3499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏡",
            "description": "Smart voice control via Wi-Fi, audio-reactive music sync, dynamic gradient scene presets.",
            "imageUrl": "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-124",
            "name": "Magnetic Slim Armor Protective Phone Case",
            "category": "ACCESSORIES",
            "price": 1199,
            "stock": 25,
            "reserved": 0,
            "emoji": "🔌",
            "description": "Military-grade 10ft drop protection, MagSafe magnetic ring array, soft tactile grip.",
            "imageUrl": "https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-125",
            "name": "Braided 100W USB-C to USB-C Fast Cable (2m)",
            "category": "ACCESSORIES",
            "price": 799,
            "stock": 25,
            "reserved": 0,
            "emoji": "🔌",
            "description": "E-Marker smart chip, nylon braided tensile durability 20,000+ bends, 480Mbps data transfer.",
            "imageUrl": "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-126",
            "name": "Ergonomic Folding Aluminum Laptop Stand",
            "category": "ACCESSORIES",
            "price": 2499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🔌",
            "description": "Aerospace-grade sandblasted aluminum, 6 height adjustment levels, optimal heat dissipation.",
            "imageUrl": "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-127",
            "name": "GaN 65W 3-Port Fast Wall Charger",
            "category": "ACCESSORIES",
            "price": 2799,
            "stock": 25,
            "reserved": 0,
            "emoji": "🔌",
            "description": "Dual USB-C PD 3.0 + USB-A QC 4+, foldable prongs, charges laptops and phones simultaneously.",
            "imageUrl": "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-128",
            "name": "Gentle Foaming Facial Cleanser (150ml)",
            "category": "BEAUTY",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Hydrating amino acid formula with soothing chamomile and tea tree extract for clear skin.",
            "imageUrl": "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-129",
            "name": "Hyaluronic Acid Hydrating Face Moisturizer",
            "category": "BEAUTY",
            "price": 899,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Triple molecular hyaluronic acid locking in 72-hour deep moisture with vitamin B5.",
            "imageUrl": "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-130",
            "name": "Ultra-Light Matte Daily Sunscreen SPF 50+",
            "category": "BEAUTY",
            "price": 749,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Broad spectrum UVA/UVB protection with invisible zero white cast and sebum control.",
            "imageUrl": "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-131",
            "name": "Moroccan Argan Oil Nourishing Shampoo",
            "category": "BEAUTY",
            "price": 649,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Sulfate-free keratin restorative shampoo enriched with cold-pressed organic argan oil.",
            "imageUrl": "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-132",
            "name": "Shea Butter Deep Moisture Body Lotion (400ml)",
            "category": "BEAUTY",
            "price": 599,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Ultra-rich whipped African shea butter and sweet almond oil for velvety soft skin.",
            "imageUrl": "https://images.unsplash.com/photo-1526947425960-945c6e72858f?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-133",
            "name": "Luxury Eau De Parfum Woody Citrus (100ml)",
            "category": "BEAUTY",
            "price": 2999,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "Artisanal blend of Italian bergamot, cedarwood, amber resin, and crisp vetiver.",
            "imageUrl": "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-134",
            "name": "Ionic Professional Salon Hair Dryer",
            "category": "BEAUTY",
            "price": 2499,
            "stock": 25,
            "reserved": 0,
            "emoji": "💄",
            "description": "2200W brushless motor with negative ion tech for frizz-free salon blowouts in minutes.",
            "imageUrl": "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-135",
            "name": "FIFA Quality Pro Match Football (Size 5)",
            "category": "SPORTS",
            "price": 1999,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Thermally bonded seamless surface with balanced butyl bladder for true flight trajectory.",
            "imageUrl": "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-136",
            "name": "English Willow Grade 1 Cricket Bat",
            "category": "SPORTS",
            "price": 4999,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Handcrafted unbleached Grade 1 English willow with thick contoured edges and huge sweet spot.",
            "imageUrl": "https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-137",
            "name": "High-Impact Leather Cricket Ball (Pack of 2)",
            "category": "SPORTS",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Alum tanned 4-piece Portuguese cork center match ball with hand-stitched seam.",
            "imageUrl": "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-138",
            "name": "Carbon Fiber Performance Tennis Racket",
            "category": "SPORTS",
            "price": 3499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "300g graphite composite frame with isometric head shape for optimal spin and control.",
            "imageUrl": "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-139",
            "name": "High-Density Non-Slip Eco Yoga Mat (6mm)",
            "category": "SPORTS",
            "price": 1299,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Textured natural tree rubber with laser-engraved alignment lines and carry strap.",
            "imageUrl": "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-140",
            "name": "Cast Iron Hexagonal Dumbbell Set (Pair 10kg)",
            "category": "SPORTS",
            "price": 2499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Anti-roll solid iron hex heads with knurled ergonomic chrome handles for secure grip.",
            "imageUrl": "https://images.unsplash.com/photo-1586401100295-7a8096fd231a?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-141",
            "name": "Heavy Duty Resistance Bands Workout Set",
            "category": "SPORTS",
            "price": 899,
            "stock": 25,
            "reserved": 0,
            "emoji": "🏃",
            "description": "Stackable 5-color tube bands up to 150 lbs with foam handles, door anchor, and ankle straps.",
            "imageUrl": "https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-142",
            "name": "Clean Architecture & Domain Design Guide",
            "category": "BOOKS",
            "price": 999,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "Comprehensive software engineering manual for microservices, DDD, and SOLID principles.",
            "imageUrl": "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-143",
            "name": "Modern Java & Spring Boot Microservices",
            "category": "BOOKS",
            "price": 1499,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "Master modern Java 21, Spring Cloud Gateway, Kafka Saga choreography, and Kubernetes.",
            "imageUrl": "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-144",
            "name": "High-Performance Python Distributed Systems",
            "category": "BOOKS",
            "price": 1299,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "In-depth guide to asyncio, multiprocessing, distributed task queues, and performance tuning.",
            "imageUrl": "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-145",
            "name": "Data Structures & Algorithms in Depth",
            "category": "BOOKS",
            "price": 1199,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "Rigorous technical reference covering trees, graphs, dynamic programming, and complexity.",
            "imageUrl": "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-146",
            "name": "Premium Hardcover Dotted Grid Journal (A5)",
            "category": "BOOKS",
            "price": 599,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "120 GSM acid-free ink-proof paper with dual ribbon bookmarks, pen loop, and expandable pocket.",
            "imageUrl": "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-147",
            "name": "Matte Black Executive Fountain Pen Set",
            "category": "BOOKS",
            "price": 899,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "Hand-finished medium iridium nib with piston converter and cartridge compatibility.",
            "imageUrl": "https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-148",
            "name": "Undated Daily Productivity Desk Planner",
            "category": "BOOKS",
            "price": 499,
            "stock": 25,
            "reserved": 0,
            "emoji": "📚",
            "description": "Habit tracking, priority matrix, time-blocking grid, and weekly review reflection spreads.",
            "imageUrl": "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-149",
            "name": "Single Origin Arabica Whole Bean Coffee (500g)",
            "category": "GROCERY",
            "price": 799,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "Medium-dark roasted Chikmagalur estate beans with tasting notes of dark cocoa and berries.",
            "imageUrl": "https://images.unsplash.com/photo-1587734195503-904fca47e0e9?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-150",
            "name": "Darjeeling Premium Organic Black Tea (250g)",
            "category": "GROCERY",
            "price": 649,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "Single-estate summer muscatel harvest offering fragrant floral aroma and golden liquor.",
            "imageUrl": "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-151",
            "name": "Himalayan Aged Long Grain Basmati Rice (5kg)",
            "category": "GROCERY",
            "price": 749,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "Naturally aged for 24 months for extra-long fluffiness and rich authentic culinary aroma.",
            "imageUrl": "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-152",
            "name": "Italian Bronze-Cut Penne Rigate Pasta (1kg)",
            "category": "GROCERY",
            "price": 399,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "100% durum wheat semolina extruded through bronze dies for optimal sauce adhesion.",
            "imageUrl": "https://images.unsplash.com/photo-1612927601601-6638404737ce?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-153",
            "name": "Cold Pressed Extra Virgin Spanish Olive Oil (1L)",
            "category": "GROCERY",
            "price": 1199,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "First cold press Picual olives with low acidity, robust fruity flavor, and high antioxidants.",
            "imageUrl": "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-154",
            "name": "Artisanal 70% Dark Single Origin Belgian Chocolate",
            "category": "GROCERY",
            "price": 349,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "Conched premium cocoa nibs with rich Madagascar vanilla and unrefined cane sugar.",
            "imageUrl": "https://images.unsplash.com/photo-1548907040-4baa42d10919?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-155",
            "name": "Gourmet Roasted Salted Almonds & Cashews (500g)",
            "category": "GROCERY",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "🍎",
            "description": "Slow dry-roasted jumbo nuts seasoned with hand-harvested pink Himalayan rock salt.",
            "imageUrl": "https://images.unsplash.com/photo-1509358271058-acd22cc93898?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-156",
            "name": "Magnetic Wireless Fast Car Charger Vent Mount",
            "category": "AUTOMOTIVE",
            "price": 1499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "15W fast inductive charging with ultra-strong neodymium magnetic lock and 360-degree ball joint.",
            "imageUrl": "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-157",
            "name": "Ultra HD 4K Dash Camera with Night Vision",
            "category": "AUTOMOTIVE",
            "price": 4999,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "Sony STARVIS 2 sensor, 170-degree ultra-wide angle, emergency G-sensor, and built-in Wi-Fi GPS.",
            "imageUrl": "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-158",
            "name": "Cordless High-Power Portable Car Vacuum Cleaner",
            "category": "AUTOMOTIVE",
            "price": 1899,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "9000Pa cyclone suction, washable HEPA filter, dual crevice nozzles, and 30-min run time.",
            "imageUrl": "https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-159",
            "name": "Digital Preset Portable Tyre Inflator Pump",
            "category": "AUTOMOTIVE",
            "price": 2499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "150 PSI rapid inflation with digital pressure gauge auto-shutoff and emergency SOS torch.",
            "imageUrl": "https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-160",
            "name": "Dual Port 60W USB-C PD Fast Car Charger",
            "category": "AUTOMOTIVE",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "All-metal compact body with smart power allocation supporting PPS and Quick Charge 4.0.",
            "imageUrl": "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-161",
            "name": "Premium Microfiber Car Detailing Towel Kit",
            "category": "AUTOMOTIVE",
            "price": 499,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "Pack of 6 edgeless 800 GSM plush towels for scratch-free buffing, waxing, and drying.",
            "imageUrl": "https://images.unsplash.com/photo-1607860108855-64acf2078ed9?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-162",
            "name": "Organic Essential Oil Vent Car Air Freshener",
            "category": "AUTOMOTIVE",
            "price": 399,
            "stock": 25,
            "reserved": 0,
            "emoji": "🚗",
            "description": "Subtle cedar and fresh lavender botanical extract diffusing natural scent for up to 60 days.",
            "imageUrl": "https://images.unsplash.com/photo-1615397349754-cfa2066a298e?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-163",
            "name": "Lightweight Polycarbonate Cabin Suitcase (55cm)",
            "category": "TRAVEL",
            "price": 4499,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Scratch-resistant German Makrolon shell with 360-degree silent spinner wheels and TSA lock.",
            "imageUrl": "https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-164",
            "name": "Expandable Hardside Check-in Luggage (75cm)",
            "category": "TRAVEL",
            "price": 6999,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Heavy duty 105L volume with interior compression dividers and aircraft-grade aluminum trolley.",
            "imageUrl": "https://images.unsplash.com/photo-1581553680321-4fffae59fccd?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-165",
            "name": "Water-Resistant Convertible Travel Duffel (45L)",
            "category": "TRAVEL",
            "price": 2299,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Weatherproof 900D tarpaulin with stowable ergonomic backpack straps and separate shoe pocket.",
            "imageUrl": "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-166",
            "name": "RFID Blocking Leather Passport Travel Wallet",
            "category": "TRAVEL",
            "price": 799,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Full-grain vegetable tanned leather holding passport, boarding passes, SIM cards, and cash.",
            "imageUrl": "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-167",
            "name": "Waterproof 6-Piece Packing Cubes Organizer",
            "category": "TRAVEL",
            "price": 999,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Breathable ripstop nylon cubes with double compression zippers saving 60% luggage space.",
            "imageUrl": "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-168",
            "name": "Ergonomic Memory Foam Travel Neck Pillow",
            "category": "TRAVEL",
            "price": 699,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "360-degree orthopedic chin support with breathable magnetic therapy fabric and earplug kit.",
            "imageUrl": "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-169",
            "name": "TSA Approved Leak-Proof Silicone Travel Bottles",
            "category": "TRAVEL",
            "price": 449,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Set of 4 food-grade squeezable 90ml bottles with triple-sealing caps and ID window.",
            "imageUrl": "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "prod-170",
            "name": "Heavy Duty Luggage Strap with Combination Lock",
            "category": "TRAVEL",
            "price": 399,
            "stock": 25,
            "reserved": 0,
            "emoji": "✈️",
            "description": "Durable cross-woven nylon strap with 3-digit customizable security lock and luggage tag.",
            "imageUrl": "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=600&q=80"
        }
    ],
  cart: [],
  orders: [],
  payments: [],
  kafkaEvents: [],
  isSagaRunning: false,
  eventSource: null,
  authToken: localStorage.getItem("ecommerce_jwt") || null,
  user: JSON.parse(localStorage.getItem("ecommerce_user") || '{"username":"admin","role":"ROLE_ADMIN","userId":"admin-001"}'),
  serviceHealth: {}
};

// ============================================================================
// Initialization
// ============================================================================
document.addEventListener("DOMContentLoaded", async () => {
  initTabs();
  initCartDrawer();
  initSagaControls();
  initAuthControls();
  initGatewayHeaderButton();
  initStorefrontControls();
  initInventoryControls();

  // Render dynamic Control Center UI components
  renderQuickAccessPanel();
  renderServiceMeshCards();
  renderObservabilityCards();
  renderSwaggerCards();

  // Auto-login to obtain a valid JWT if missing
  if (!state.authToken) {
    await performLogin("admin", "admin123", true);
  } else {
    updateAuthDisplay();
  }

  // Initial SSE & backend sync
  initSseStream();
  loadBackendData();

  // Initial immediate health checks & setup periodic polling (Requirements 10 & 12)
  syncServiceMeshHealth(false);
  syncObservabilityHealth(false);

  if (state.meshPollInterval) clearInterval(state.meshPollInterval);
  if (state.obsPollInterval) clearInterval(state.obsPollInterval);

  state.meshPollInterval = setInterval(() => syncServiceMeshHealth(false), 12000);
  state.obsPollInterval = setInterval(() => syncObservabilityHealth(false), 8000);

  window.addEventListener("beforeunload", () => {
    if (state.meshPollInterval) clearInterval(state.meshPollInterval);
    if (state.obsPollInterval) clearInterval(state.obsPollInterval);
  });
});

// Helper for authorized backend requests through API Gateway
function getAuthHeaders(extra = {}) {
  const headers = { "Content-Type": "application/json", ...extra };
  if (state.authToken) {
    headers["Authorization"] = `Bearer ${state.authToken}`;
  }
  return headers;
}

// ============================================================================
// Tab Navigation & Role View Filtering
// ============================================================================
function initTabs() {
  const tabButtons = document.querySelectorAll(".nav-tabs .nav-btn:not(#navCartTabBtn)");
  const tabPanels = document.querySelectorAll(".tab-panel");

  tabButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetTabId = btn.getAttribute("data-tab");

      tabButtons.forEach(b => b.classList.remove("active"));
      tabPanels.forEach(p => p.classList.remove("active"));

      btn.classList.add("active");
      const targetPanel = document.getElementById(targetTabId);
      if (targetPanel) {
        targetPanel.classList.add("active");
      }

      if (targetTabId === "topology-tab") {
        syncServiceMeshHealth(false);
      } else if (targetTabId === "gateway-tab") {
        loadGatewaySystemServices();
      } else if (targetTabId === "orders-tab") {
        loadOrdersFromBackend();
      } else if (targetTabId === "inventory-tab") {
        loadInventoryFromBackend();
      } else if (targetTabId === "payments-tab") {
        loadPaymentsFromBackend();
      } else if (targetTabId === "kafka-tab") {
        loadKafkaEventsFromBackend();
      }
    });
  });

  // Dedicated Nav Cart Tab Button (Requirement 3: Visible dynamic cart in top nav)
  const navCartBtn = document.getElementById("navCartTabBtn");
  if (navCartBtn) {
    navCartBtn.addEventListener("click", () => {
      toggleCart(true);
    });
  }

  // Filter chips in storefront
  const filterChips = document.querySelectorAll(".filter-chip");
  filterChips.forEach(chip => {
    chip.addEventListener("click", () => {
      filterChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      renderProducts(chip.getAttribute("data-category"));
    });
  });
}

function initStorefrontControls() {
  const searchInput = document.getElementById("productSearchInput");
  const sortSelect = document.getElementById("productSortSelect");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      const activeChip = document.querySelector(".filter-chip.active");
      const category = activeChip ? activeChip.getAttribute("data-category") : "ALL";
      renderProducts(category);
    });
  }

  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      const activeChip = document.querySelector(".filter-chip.active");
      const category = activeChip ? activeChip.getAttribute("data-category") : "ALL";
      renderProducts(category);
    });
  }
}

function initInventoryControls() {
  const searchInput = document.getElementById("inventorySearchInput");
  const stockFilter = document.getElementById("inventoryStockFilter");

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      renderInventoryTable();
    });
  }

  if (stockFilter) {
    stockFilter.addEventListener("change", () => {
      renderInventoryTable();
    });
  }
}

function applyRoleRestrictions() {
  const isAdmin = state.user && state.user.role === "ROLE_ADMIN";
  
  // Show / hide admin-specific views based on RBAC matrix
  const inventoryTabBtn = document.querySelector('[data-tab="inventory-tab"]');
  const topologyTabBtn = document.querySelector('[data-tab="topology-tab"]');
  const gatewayTabBtn = document.querySelector('[data-tab="gateway-tab"]');
  const kafkaTabBtn = document.querySelector('[data-tab="kafka-tab"]');

  if (!isAdmin) {
    // Customer mode: can view storefront, cart, orders
    if (inventoryTabBtn) inventoryTabBtn.title = "View Only (Customer Scope)";
  }
}

// ============================================================================
// Top Bar Clickable Gateway Button (Requirement 10)
// ============================================================================
function initGatewayHeaderButton() {
  const gatewayBtn = document.getElementById("gatewayStatusBtn");
  if (!gatewayBtn) return;

  gatewayBtn.addEventListener("click", (e) => {
    // Primary: Open Gateway :8080 in new tab
    const url = CONFIG.gateway ? CONFIG.gateway.url : "http://localhost:8080";
    window.open(url, "_blank");
  });
}

// ============================================================================
// Authentication & RBAC Controls (Requirements 21 & 22)
// ============================================================================
function initAuthControls() {
  const authWidgetBtn = document.getElementById("authWidgetBtn");
  const authModalBackdrop = document.getElementById("authModalBackdrop");
  const closeAuthModalBtn = document.getElementById("closeAuthModalBtn");
  const quickAdminBtn = document.getElementById("quickLoginAdminBtn");
  const quickCustBtn = document.getElementById("quickLoginCustomerBtn");
  const logoutBtn = document.getElementById("authLogoutBtn");

  if (authWidgetBtn && authModalBackdrop) {
    authWidgetBtn.addEventListener("click", () => {
      authModalBackdrop.classList.add("active");
    });
  }

  if (closeAuthModalBtn && authModalBackdrop) {
    closeAuthModalBtn.addEventListener("click", () => {
      authModalBackdrop.classList.remove("active");
    });
  }

  if (authModalBackdrop) {
    authModalBackdrop.addEventListener("click", (e) => {
      if (e.target === authModalBackdrop) {
        authModalBackdrop.classList.remove("active");
      }
    });
  }

  if (quickAdminBtn) {
    quickAdminBtn.addEventListener("click", () => performLogin("admin", "admin123"));
  }

  if (quickCustBtn) {
    quickCustBtn.addEventListener("click", () => performLogin("customer1", "password123"));
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      state.authToken = null;
      state.user = { username: "guest", role: "ANONYMOUS", userId: "guest-0" };
      localStorage.removeItem("ecommerce_jwt");
      localStorage.removeItem("ecommerce_user");
      updateAuthDisplay();
      if (authModalBackdrop) authModalBackdrop.classList.remove("active");
      logToConsole("[Auth] Logged out. Session is now unauthenticated.", "info");
    });
  }
}

async function handleAuthSubmit(event) {
  event.preventDefault();
  const username = document.getElementById("authUsernameInput")?.value?.trim();
  const password = document.getElementById("authPasswordInput")?.value?.trim();
  if (!username || !password) return;
  await performLogin(username, password);
}

async function performLogin(username, password, silent = false) {
  const errBox = document.getElementById("authErrorMsg");
  if (errBox) errBox.style.display = "none";

  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (res.ok) {
      const data = await res.json();
      state.authToken = data.token;
      state.user = {
        username: data.username,
        role: data.roles && data.roles.length > 0 ? data.roles[0] : "ROLE_CUSTOMER",
        userId: data.userId
      };

      localStorage.setItem("ecommerce_jwt", state.authToken);
      localStorage.setItem("ecommerce_user", JSON.stringify(state.user));

      updateAuthDisplay();
      const modal = document.getElementById("authModalBackdrop");
      if (modal) modal.classList.remove("active");

      if (!silent) {
        logToConsole(`[Auth] Authenticated as ${state.user.username} (${state.user.role}). JWT token stored.`, "success");
      }
      applyRoleRestrictions();
    } else {
      if (!silent && errBox) {
        errBox.textContent = "Invalid credentials. Please verify username and password.";
        errBox.style.display = "block";
      }
    }
  } catch (err) {
    if (!silent) {
      console.warn("Auth request failed (Gateway offline):", err.message);
      if (errBox) {
        errBox.textContent = `API Gateway at ${API_BASE} unavailable.`;
        errBox.style.display = "block";
      }
    }
  }
}

function updateAuthDisplay() {
  const nameEl = document.getElementById("currentUserName");
  const roleEl = document.getElementById("currentUserRole");
  if (nameEl) nameEl.textContent = state.user.username;
  if (roleEl) {
    roleEl.textContent = state.user.role;
    roleEl.style.color = state.user.role === "ROLE_ADMIN" ? "#ede9fe" : "#a7f3d0";
  }
}

// ============================================================================
// Dynamic UI Component Renderers (Using Centralized SERVICE_CONFIG)
// ============================================================================

// 1. Quick Access Toolbar (Requirement 13 & 20)
function renderQuickAccessPanel() {
  const container = document.getElementById("quickAccessGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  // Exclude frontend, currency, and ensure valid port & url (fixes "undefined :undefined")
  const keys = Object.keys(cfg).filter(k => k !== "frontend" && k !== "currency" && cfg[k].port && cfg[k].url);

  container.innerHTML = keys.map(k => {
    const svc = cfg[k];
    const isObs = ["prometheus", "grafana", "zipkin"].includes(k);
    return `
      <button class="quick-chip ${isObs ? 'chip-obs' : ''}" onclick="window.open('${svc.url}', '_blank')">
        <span>${svc.icon || '⚡'}</span>
        <span>${svc.name}</span>
        <span class="chip-port">:${svc.port}</span>
      </button>
    `;
  }).join("");
}

// 2. Clickable Service Mesh Cards (Requirements 2, 4, 8)
function renderServiceMeshCards() {
  const container = document.getElementById("meshCardsContainer");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const coreKeys = ["eureka", "configServer", "gateway", "product", "order", "inventory", "payment", "notification"];

  container.innerHTML = coreKeys.map(k => {
    const svc = cfg[k];
    const hasSwagger = !!svc.swagger;

    return `
      <div class="mesh-card" id="mesh-card-${svc.key}" onclick="handleCardClick(event, '${svc.url}')">
        <div class="mesh-card-header">
          <div class="mesh-icon">${svc.icon || '📦'}</div>
          <div class="mesh-info">
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
              <h3>${svc.name}</h3>
              <span class="status-pill offline" id="pill-${svc.key}">● OFFLINE</span>
            </div>
            <p>${svc.subName || svc.role}</p>
            <div class="mesh-meta-row">
              <span class="port-tag">Port :${svc.port}</span>
              <span class="latency-badge" id="latency-${svc.key}">-- ms</span>
            </div>
          </div>
        </div>
        <div class="mesh-card-actions">
          <button class="btn-card-action" onclick="event.stopPropagation(); window.open('${svc.url}', '_blank');">
            <span>↗ OPEN SERVICE</span>
          </button>
          ${hasSwagger ? `
            <button class="btn-card-action swagger" onclick="event.stopPropagation(); window.open('${svc.swagger}', '_blank');">
              <span>📑 SWAGGER</span>
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join("");
}

// 3. Observability Cards (Requirements 5, 7, 9, 12)
function renderObservabilityCards() {
  const container = document.getElementById("observabilityGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const obsKeys = ["prometheus", "grafana", "zipkin"];

  container.innerHTML = obsKeys.map(k => {
    const svc = cfg[k];
    return `
      <div class="observability-card" id="obs-card-${svc.key}" onclick="handleCardClick(event, '${svc.url}')">
        <div>
          <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:0.75rem;">
            <span style="font-size: 2.2rem;">${svc.icon || '📊'}</span>
            <div style="display:flex; align-items:center; gap:0.4rem;">
              <span class="latency-badge" id="latency-${svc.key}">-- ms</span>
              <span class="status-pill offline" id="pill-${svc.key}">● UNKNOWN</span>
            </div>
          </div>
          <h3>${svc.name}</h3>
          <p style="font-size:0.78rem; color:var(--text-muted); margin-top:0.25rem;">${svc.subName}</p>
          <div style="display:flex; justify-content:space-between; align-items:center; margin-top:0.6rem;">
            <span class="port-tag">Port :${svc.port}</span>
            <span class="obs-card-checked" id="checked-${svc.key}" style="font-size:0.72rem; color:var(--text-dim);">Last checked: --:--:--</span>
          </div>
          <div class="obs-card-error" id="obs-error-${svc.key}" style="display:none; margin-top:0.6rem; font-size:0.75rem; color:var(--color-danger); background:var(--color-danger-soft); padding:0.35rem 0.6rem; border-radius:var(--radius-sm); border:1px solid rgba(231,111,81,0.25);"></div>
        </div>
        <div class="mesh-card-actions">
          <button class="btn-card-action" onclick="event.stopPropagation(); window.open('${svc.url}', '_blank');">
            <span>↗ OPEN ${svc.name.toUpperCase()}</span>
          </button>
        </div>
      </div>
    `;
  }).join("");
}

// 4. Interactive Swagger / OpenAPI Directory (Requirement 11)
function renderSwaggerCards() {
  const container = document.getElementById("swaggerGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const swaggerKeys = ["gateway", "product", "order", "inventory", "payment", "notification"];

  container.innerHTML = swaggerKeys.map(k => {
    const svc = cfg[k];
    return `
      <div class="swagger-card" onclick="window.open('${svc.swagger}', '_blank')">
        <div>
          <h4 style="font-size:0.88rem; font-weight:700; display:flex; align-items:center; gap:0.45rem;">
            <span>${svc.icon}</span> ${svc.name}
          </h4>
          <span class="chip-port" style="margin-top:0.25rem; display:inline-block;">Port :${svc.port}</span>
        </div>
        <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); window.open('${svc.swagger}', '_blank');">
          <span>📑 OPEN SWAGGER</span>
        </button>
      </div>
    `;
  }).join("");
}

function handleCardClick(event, url) {
  // If clicked directly on an action button, let the button handler fire
  if (event.target.closest("button") || event.target.closest("a")) return;
  window.open(url, "_blank");
}

// ============================================================================
// Real Dynamic Health Checking & Latency Measurement (Requirements 5, 6, 7)
// ============================================================================
async function syncServiceMeshHealth(manual = false) {
  if (!window.SERVICE_CONFIG) return;
  const cfg = window.SERVICE_CONFIG;
  // Exclude non-microservice entries and observability tools (handled by syncObservabilityHealth)
  const keys = Object.keys(cfg).filter(k => 
    k !== "frontend" && 
    k !== "currency" && 
    !["prometheus", "grafana", "zipkin"].includes(k) &&
    cfg[k].port && 
    cfg[k].url
  );

  const refreshBtn = document.getElementById("refreshHealthBtn");
  const refreshIcon = document.getElementById("refreshHealthIcon");
  const refreshText = document.getElementById("refreshHealthText");

  if (manual && refreshBtn) {
    refreshBtn.disabled = true;
    if (refreshIcon) refreshIcon.style.animation = "spin 0.8s linear infinite";
    if (refreshText) refreshText.textContent = "Checking...";
  }

  // Also query Eureka services via Gateway DiscoveryClient if accessible
  let eurekaServices = [];
  try {
    const sysRes = await fetch(`${API_BASE}/api/system/services`, {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(1500)
    });
    if (sysRes.ok) {
      eurekaServices = await sysRes.json();
    }
  } catch (_) {
    // Gateway or endpoint may be momentarily quiet
  }

  for (const k of keys) {
    const svc = cfg[k];
    if (!svc.health && !svc.url) continue;

    const targetUrl = svc.health || svc.url;
    const pill = document.getElementById(`pill-${svc.key}`);
    const latencyEl = document.getElementById(`latency-${svc.key}`);

    const startTime = performance.now();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    try {
      // Direct health probe
      const res = await fetch(targetUrl, {
        signal: controller.signal,
        headers: { "Accept": "application/json" }
      });
      clearTimeout(timeoutId);

      const latencyMs = Math.round(performance.now() - startTime);

      if (res.ok) {
        let isUp = true;
        try {
          const data = await res.json();
          if (data && data.status && data.status !== "UP") {
            isUp = false;
          }
        } catch (_) {
          // Non-JSON 200 OK (e.g. Prometheus /-/healthy returns plain text)
          isUp = true;
        }

        if (isUp) {
          updatePillStatus(pill, latencyEl, "HEALTHY", "up", latencyMs);
          state.serviceHealth[svc.key] = { status: "UP", latency: latencyMs };
        } else {
          updatePillStatus(pill, latencyEl, "DEGRADED", "degraded", latencyMs);
          state.serviceHealth[svc.key] = { status: "DEGRADED", latency: latencyMs };
        }
      } else if (res.status === 401 || res.status === 403) {
        // Service is alive but secured
        updatePillStatus(pill, latencyEl, "HEALTHY", "up", latencyMs);
        state.serviceHealth[svc.key] = { status: "UP", latency: latencyMs };
      } else {
        updatePillStatus(pill, latencyEl, "DEGRADED", "degraded", latencyMs);
        state.serviceHealth[svc.key] = { status: "DEGRADED", latency: latencyMs };
      }
    } catch (err) {
      clearTimeout(timeoutId);
      
      // Check if Eureka discovery reports this service as UP
      const matchingEureka = eurekaServices.find(es => 
        (es.serviceName && es.serviceName.toLowerCase().includes(svc.key.toLowerCase())) ||
        es.port === svc.port
      );

      if (matchingEureka && matchingEureka.status === "UP") {
        updatePillStatus(pill, latencyEl, "HEALTHY", "up", 15);
        state.serviceHealth[svc.key] = { status: "UP", latency: 15 };
      } else {
        updatePillStatus(pill, latencyEl, "OFFLINE", "down", null);
        state.serviceHealth[svc.key] = { status: "DOWN", latency: null };
      }
    }
  }

  // Update Gateway Indicator in Header
  const gwHealth = state.serviceHealth["gateway"];
  const gwDot = document.getElementById("gatewayDot");
  const gwText = document.getElementById("gatewayStatusText");
  if (gwDot && gwText) {
    if (gwHealth && gwHealth.status === "UP") {
      gwDot.className = "status-dot green";
      gwText.textContent = `Gateway :8080 (${gwHealth.latency}ms)`;
    } else {
      gwDot.className = "status-dot red";
      gwText.textContent = "Gateway OFFLINE";
    }
  }

  // Update Last Checked Timestamp (Requirement 9)
  const lastCheckedEl = document.getElementById("healthLastChecked");
  if (lastCheckedEl) {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('en-US', { hour12: false });
    lastCheckedEl.textContent = `Last checked: ${timeStr}`;
  }

  // Also synchronize observability stack health
  syncObservabilityHealth(manual);

  // Restore refresh button
  if (refreshBtn) {
    refreshBtn.disabled = false;
    if (refreshIcon) refreshIcon.style.animation = "";
    if (refreshText) refreshText.textContent = "Refresh Health";
  }
}

// ============================================================================
// Observability Health Polling & Telemetry (Requirements 4-15)
// ============================================================================
async function syncObservabilityHealth(manual = false) {
  const refreshBtn = document.getElementById("refreshObsBtn");
  const refreshIcon = document.getElementById("refreshObsIcon");
  const refreshText = document.getElementById("refreshObsText");
  const lastCheckedEl = document.getElementById("obsLastChecked");

  if (manual && refreshBtn) {
    refreshBtn.disabled = true;
    if (refreshIcon) refreshIcon.style.animation = "spin 0.8s linear infinite";
    if (refreshText) refreshText.textContent = "Checking...";
  }

  try {
    const res = await fetch("/api/obs-health", {
      headers: { "Accept": "application/json" },
      signal: AbortSignal.timeout(3000)
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const timestamp = data.timestamp || new Date().toLocaleTimeString('en-US', { hour12: false });
    if (lastCheckedEl) lastCheckedEl.textContent = `Last checked: ${timestamp}`;

    // Update Compact Summary Banner (Requirement 15)
    const summaryBadge = document.getElementById("obsSummaryBadge");
    const summaryMsg = document.getElementById("obsSummaryMsg");
    if (data.summary) {
      if (summaryBadge) {
        summaryBadge.textContent = `${data.summary.online} / ${data.summary.total} ONLINE`;
        if (data.summary.online === data.summary.total) {
          summaryBadge.className = "obs-summary-badge healthy";
        } else if (data.summary.online > 0) {
          summaryBadge.className = "obs-summary-badge degraded";
        } else {
          summaryBadge.className = "obs-summary-badge offline";
        }
      }
      if (summaryMsg) {
        if (data.summary.online === data.summary.total) {
          summaryMsg.textContent = "✓ Observability stack operational";
          summaryMsg.className = "obs-summary-msg healthy";
        } else if (data.summary.online > 0) {
          summaryMsg.textContent = "⚠ Partial observability availability";
          summaryMsg.className = "obs-summary-msg degraded";
        } else {
          summaryMsg.textContent = "✕ Observability stack unavailable";
          summaryMsg.className = "obs-summary-msg offline";
        }
      }
    }

    // Update Individual Cards & Summary Pills (Requirements 5, 7, 9, 13)
    const obsKeys = ["prometheus", "grafana", "zipkin"];
    obsKeys.forEach(k => {
      const svcResult = data.services && data.services[k];
      const pill = document.getElementById(`pill-${k}`);
      const latencyEl = document.getElementById(`latency-${k}`);
      const checkedEl = document.getElementById(`checked-${k}`);
      const errorEl = document.getElementById(`obs-error-${k}`);
      const summaryPill = document.getElementById(`obs-pill-summary-${k}`);
      const cfgSvc = window.SERVICE_CONFIG && window.SERVICE_CONFIG[k];
      const svcName = cfgSvc ? cfgSvc.name : k;

      if (checkedEl) checkedEl.textContent = `Last checked: ${timestamp}`;

      if (svcResult && svcResult.status === "UP") {
        updatePillStatus(pill, latencyEl, "HEALTHY", "up", svcResult.latency);
        if (errorEl) errorEl.style.display = "none";
        if (summaryPill) {
          summaryPill.innerHTML = `${svcName} <span class="dot-healthy">●</span> HEALTHY`;
          summaryPill.className = "obs-pill healthy";
        }
        state.serviceHealth[k] = { status: "UP", latency: svcResult.latency };
      } else if (svcResult && svcResult.status === "DEGRADED") {
        updatePillStatus(pill, latencyEl, "DEGRADED", "degraded", svcResult.latency);
        if (errorEl) {
          errorEl.textContent = `${svcName} responded with HTTP ${svcResult.statusCode}`;
          errorEl.style.display = "block";
        }
        if (summaryPill) {
          summaryPill.innerHTML = `${svcName} <span class="dot-degraded">●</span> DEGRADED`;
          summaryPill.className = "obs-pill degraded";
        }
        state.serviceHealth[k] = { status: "DEGRADED", latency: svcResult.latency };
      } else {
        updatePillStatus(pill, latencyEl, "OFFLINE", "down", null);
        if (errorEl) {
          errorEl.textContent = `${svcName} is not reachable at localhost:${cfgSvc ? cfgSvc.port : ''}`;
          errorEl.style.display = "block";
        }
        if (summaryPill) {
          summaryPill.innerHTML = `${svcName} <span class="dot-offline">●</span> OFFLINE`;
          summaryPill.className = "obs-pill offline";
        }
        state.serviceHealth[k] = { status: "DOWN", latency: null };
      }
    });
  } catch (err) {
    console.warn("Failed to query /api/obs-health:", err);
  } finally {
    if (refreshBtn) {
      refreshBtn.disabled = false;
      if (refreshIcon) refreshIcon.style.animation = "";
      if (refreshText) refreshText.textContent = "Refresh";
    }
  }
}

function updatePillStatus(pill, latencyEl, statusText, statusClass, latencyMs) {
  if (pill) {
    pill.className = `status-pill ${statusClass}`;
    pill.textContent = `● ${statusText}`;
  }
  if (latencyEl) {
    latencyEl.textContent = latencyMs !== null ? `${latencyMs} ms` : "-- ms";
  }
}

// ============================================================================
// Safe Service Discovery Registry Endpoint (Requirement 24)
// ============================================================================
async function loadGatewaySystemServices() {
  const tbody = document.getElementById("gatewayServicesTableBody");
  if (!tbody) return;

  try {
    const res = await fetch(`${API_BASE}/api/system/services`, {
      headers: getAuthHeaders()
    });

    if (res.ok) {
      const services = await res.json();
      if (Array.isArray(services) && services.length > 0) {
        tbody.innerHTML = services.map(s => `
          <tr>
            <td><strong>${s.serviceName}</strong></td>
            <td>
              <span class="status-chip ${s.status === 'UP' ? 'confirmed' : 'cancelled'}">
                ${s.status === 'UP' ? '✓ UP' : '✕ DOWN'}
              </span>
            </td>
            <td><code>${s.host}</code></td>
            <td><code style="color:#67e8f9;">${s.port}</code></td>
            <td>${s.instanceCount}</td>
            <td>
              <button class="btn btn-sm btn-outline" onclick="window.open('http://${s.host}:${s.port}', '_blank')">
                <span>↗ Open</span>
              </button>
            </td>
          </tr>
        `).join("");
        return;
      }
    }
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No services currently registered in Eureka.</td></tr>`;
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state" style="color:var(--color-danger);">Failed to query /api/system/services: ${err.message}</td></tr>`;
  }
}

// ============================================================================
// Real Backend API Sync (Storefront, Orders, Inventory, Payments, Kafka)
// ============================================================================
async function loadBackendData() {
  await Promise.allSettled([
    loadProductsFromBackend(),
    loadOrdersFromBackend(),
    loadInventoryFromBackend(),
    loadPaymentsFromBackend(),
    loadKafkaEventsFromBackend(),
    syncServiceMeshHealth()
  ]);
}

async function loadProductsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/products`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        state.products = data.map(p => ({
          id: p.id,
          name: p.name,
          category: p.category || "ELECTRONICS",
          price: Number(p.price) || 0,
          stock: 25,
          reserved: 0,
          emoji: EMOJI_MAP[p.category] || "📦",
          description: p.description,
          imageUrl: p.imageUrl || ""
        }));
      }
    }
  } catch (err) {
    console.warn("Backend products endpoint not reachable, using seed state:", err.message);
  }
  renderProducts("ALL");
}

async function loadOrdersFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/orders`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        state.orders = data.map(o => ({
          orderId: o.id,
          customerEmail: o.customerId,
          items: Array.isArray(o.items) ? o.items.map(i => `${i.productId} (x${i.quantity})`).join(", ") : "Items",
          totalAmount: Number(o.totalAmount) || 0,
          status: o.status,
          createdAt: o.createdAt ? o.createdAt.replace("T", " ").substring(0, 19) : new Date().toISOString()
        }));
      }
    }
  } catch (err) {
    console.warn("Backend orders endpoint not reachable:", err.message);
  }
  renderOrdersTable();
}

async function loadInventoryFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/inventory`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        data.forEach(inv => {
          const prod = state.products.find(p => p.id === inv.productId);
          if (prod) {
            prod.stock = inv.availableQuantity;
            prod.reserved = inv.reservedQuantity;
          }
        });
      }
    }
  } catch (err) {
    console.warn("Backend inventory endpoint not reachable:", err.message);
  }
  renderInventoryTable();
  renderProducts("ALL");
}

async function loadPaymentsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/payments`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        state.payments = data.map(p => ({
          txId: p.transactionId || p.id,
          orderId: p.orderId,
          customer: p.customerId,
          amount: Number(p.amount) || 0,
          status: p.status,
          timestamp: p.createdAt ? p.createdAt.replace("T", " ").substring(0, 19) : new Date().toISOString()
        }));
      }
    }
  } catch (err) {
    console.warn("Backend payments endpoint not reachable:", err.message);
  }
  renderPaymentsTable();
}

async function loadKafkaEventsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/orders/kafka-events`, { headers: getAuthHeaders() });
    if (res.ok) {
      const events = await res.json();
      if (Array.isArray(events)) {
        state.kafkaEvents = events;
        renderKafkaEvents();
      }
    }
  } catch (err) {
    console.warn("Backend kafka-events endpoint not reachable:", err.message);
  }
}

// ============================================================================
// Real-Time Server-Sent Events (SSE) Stream
// ============================================================================
function initSseStream() {
  try {
    if (state.eventSource) {
      state.eventSource.close();
    }

    state.eventSource = new EventSource(`${API_BASE}/api/orders/events/stream`);

    state.eventSource.onopen = () => {
      logToConsole("[SSE Stream] Connected to live backend Saga event bus at :8080", "success");
    };

    state.eventSource.onerror = () => {
      // Backend / Kafka may be offline
      const terminalTitle = document.querySelector(".terminal-title");
      if (terminalTitle) {
        terminalTitle.textContent = "broker-9092 : KAFKA OFFLINE / RECONNECTING...";
      }
    };

    const eventNames = [
      "OrderCreatedEvent",
      "InventoryReservedEvent",
      "InventoryReservationFailedEvent",
      "PaymentCompletedEvent",
      "PaymentFailedEvent",
      "OrderConfirmedEvent",
      "OrderCancelledEvent",
      "InventoryReleasedEvent",
      "NotificationDispatchedEvent"
    ];

    eventNames.forEach(evtName => {
      state.eventSource.addEventListener(evtName, (event) => {
        try {
          const payload = JSON.parse(event.data);
          handleBackendSagaEvent(evtName, payload);
        } catch (e) {
          console.error("Failed to parse SSE event payload", e);
        }
      });
    });

  } catch (err) {
    console.warn("SSE connection error:", err.message);
  }
}

function handleBackendSagaEvent(eventType, payload) {
  logToConsole(`[Kafka Event Stream] Received ${eventType} for order: ${payload.orderId || payload.key}`, "kafka");
  recordKafkaEvent(payload.topic || "domain-events", payload.key || payload.orderId, eventType, payload);

  switch (eventType) {
    case "OrderCreatedEvent":
      highlightSagaNode("gateway", "Forwarded (200 OK)", "success-step", 1);
      highlightSagaNode("order", "State: PENDING", "active-processing", 2);
      highlightSagaNode("kafka", "order-events (P0)", "success-step", 3);
      loadOrdersFromBackend();
      break;

    case "InventoryReservedEvent":
      highlightSagaNode("inventory", "Stock Reserved", "success-step", 4);
      loadInventoryFromBackend();
      break;

    case "InventoryReservationFailedEvent":
      highlightSagaNode("inventory", "Out of Stock (400)", "failed-step", 4);
      break;

    case "PaymentCompletedEvent":
      highlightSagaNode("payment", `Approved (${CURRENCY.format(payload.amount || 0)})`, "success-step", 5);
      loadPaymentsFromBackend();
      break;

    case "PaymentFailedEvent":
      highlightSagaNode("payment", "DECLINED (402)", "failed-step", 5);
      break;

    case "OrderConfirmedEvent":
      highlightSagaNode("order", "State: CONFIRMED", "success-step", 2);
      loadOrdersFromBackend();
      break;

    case "OrderCancelledEvent":
      highlightSagaNode("order", "State: CANCELLED (Compensated)", "failed-step", 2);
      loadOrdersFromBackend();
      break;

    case "InventoryReleasedEvent":
      highlightSagaNode("inventory", "Stock Compensated (+Qty)", "warn-step", 4);
      loadInventoryFromBackend();
      break;

    case "NotificationDispatchedEvent":
      highlightSagaNode("notification", "Alert Dispatched (SSE)", "success-step", 5);
      completeSagaRun(true);
      break;
  }
}

function recordKafkaEvent(topic, key, eventType, payload) {
  const now = new Date().toISOString().substring(11, 23);
  state.kafkaEvents.unshift({
    timestamp: now,
    topic: topic,
    key: key || "key-default",
    eventType: eventType,
    payload: payload
  });

  if (state.kafkaEvents.length > 50) {
    state.kafkaEvents.pop();
  }

  const badge = document.getElementById("kafkaEventCount");
  if (badge) badge.textContent = state.kafkaEvents.length;

  renderKafkaEvents();
}

// ============================================================================
// Storefront & Cart Operations (Requirements 3, 4, 5, 18)
// ============================================================================
function renderProducts(category = "ALL") {
  const grid = document.getElementById("productsGrid");
  if (!grid) return;

  const searchInput = document.getElementById("productSearchInput");
  const sortSelect = document.getElementById("productSortSelect");
  const searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : "";
  const sortOption = sortSelect ? sortSelect.value : "default";

  let filtered = [...state.products];

  // Category Filter
  if (category && category !== "ALL") {
    filtered = filtered.filter(p => p.category === category);
  }

  // Search Filter
  if (searchQuery) {
    filtered = filtered.filter(p => 
      (p.name && p.name.toLowerCase().includes(searchQuery)) ||
      (p.description && p.description.toLowerCase().includes(searchQuery)) ||
      (p.category && p.category.toLowerCase().includes(searchQuery)) ||
      (p.brand && p.brand.toLowerCase().includes(searchQuery))
    );
  }

  // Sorting
  if (sortOption === "price-asc") {
    filtered.sort((a, b) => a.price - b.price);
  } else if (sortOption === "price-desc") {
    filtered.sort((a, b) => b.price - a.price);
  } else if (sortOption === "name-asc") {
    filtered.sort((a, b) => a.name.localeCompare(b.name));
  } else if (sortOption === "name-desc") {
    filtered.sort((a, b) => b.name.localeCompare(a.name));
  }

  const countBadge = document.getElementById("productCountBadge");
  if (countBadge) countBadge.textContent = state.products ? state.products.length : filtered.length;

  if (filtered.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 2.5rem; text-align: center; color: var(--text-muted); background: var(--bg-card); border-radius: var(--radius-lg); border: 1px dashed var(--border-subtle);">
        <p style="font-size: 1.1rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.5rem;">No products found</p>
        <p style="font-size: 0.85rem;">Try adjusting your search query or selecting a different category.</p>
      </div>
    `;
    return;
  }

  grid.innerHTML = filtered.map(prod => `
    <div class="product-card">
      <div class="product-badge">${prod.category}</div>
      <div class="product-img-wrapper">
        <img src="${prod.imageUrl || ''}" alt="${prod.name}" class="product-img" loading="lazy" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
        <div class="product-img-fallback">${prod.emoji || '📦'}</div>
      </div>
      <div class="product-body">
        <h3 class="product-title">${prod.name}</h3>
        <p class="product-desc">${prod.description}</p>
        <div class="product-pricing">
          <span class="product-price">${CURRENCY.format(prod.price)}</span>
          <span class="product-stock ${prod.stock > 0 ? '' : 'out-of-stock'}">
            ${prod.stock > 0 ? `● In Stock (${prod.stock})` : '● Out of Stock'}
          </span>
        </div>
        <button class="btn btn-primary" onclick="addToCart('${prod.id}')" ${prod.stock > 0 ? '' : 'disabled'}>
          <span>🛒 Add to Cart</span>
        </button>
      </div>
    </div>
  `).join("");
}

function initCartDrawer() {
  const openBtn = document.getElementById("openCartBtn");
  const navCartBtn = document.getElementById("navCartTabBtn");
  const closeBtn = document.getElementById("closeCartBtn");
  const backdrop = document.getElementById("cartBackdrop");
  const checkoutBtn = document.getElementById("checkoutBtn");

  if (openBtn) openBtn.addEventListener("click", () => toggleCart(true));
  if (navCartBtn) navCartBtn.addEventListener("click", () => toggleCart(true));
  if (closeBtn) closeBtn.addEventListener("click", () => toggleCart(false));
  if (backdrop) backdrop.addEventListener("click", () => toggleCart(false));

  if (checkoutBtn) {
    checkoutBtn.addEventListener("click", () => {
      if (state.cart.length === 0) {
        alert("Your cart is empty! Add products first.");
        return;
      }
      toggleCart(false);
      document.querySelector('[data-tab="saga-tab"]')?.click();
      runLiveSaga(true);
    });
  }

  // Ensure cart is synchronized on initial load
  renderCart();
}

function toggleCart(show) {
  const drawer = document.getElementById("cartDrawer");
  const backdrop = document.getElementById("cartBackdrop");
  if (drawer && backdrop) {
    if (show) {
      renderCart(); // Synchronize before drawer slides in
      drawer.classList.add("active");
      backdrop.classList.add("active");
    } else {
      drawer.classList.remove("active");
      backdrop.classList.remove("active");
    }
  }
}

function addToCart(productId) {
  const prod = state.products.find(p => p.id === productId);
  if (!prod) return;

  const existing = state.cart.find(c => c.productId === productId);
  if (existing) {
    existing.qty += 1;
  } else {
    state.cart.push({
      productId: prod.id,
      name: prod.name,
      price: prod.price,
      emoji: prod.emoji,
      qty: 1
    });
  }
  renderCart();

  // Subtle micro-animation on cart badges
  const navBadge = document.getElementById("navCartCount");
  if (navBadge) {
    navBadge.style.transform = "scale(1.3)";
    setTimeout(() => navBadge.style.transform = "scale(1)", 200);
  }
}

function changeCartQty(productId, delta) {
  const item = state.cart.find(c => c.productId === productId);
  if (!item) return;

  item.qty += delta;
  if (item.qty <= 0) {
    removeFromCart(productId);
  } else {
    renderCart();
  }
}

function removeFromCart(productId) {
  state.cart = state.cart.filter(item => item.productId !== productId);
  renderCart();
}

function renderCart() {
  const navBadge = document.getElementById("navCartCount");
  const headerBadge = document.getElementById("cartCount");
  const drawerBadge = document.getElementById("cartDrawerCount");
  const listContainer = document.getElementById("cartItemsList");
  const totalDisplay = document.getElementById("cartTotalDisplay");

  const totalQty = state.cart.reduce((sum, item) => sum + item.qty, 0);
  const totalAmount = state.cart.reduce((sum, item) => sum + (item.price * item.qty), 0);

  // Synchronize all Cart count badges (Requirements 3 & 4)
  if (navBadge) navBadge.textContent = totalQty;
  if (headerBadge) headerBadge.textContent = totalQty;
  if (drawerBadge) drawerBadge.textContent = totalQty;
  if (totalDisplay) totalDisplay.textContent = CURRENCY.format(totalAmount);

  if (!listContainer) return;

  if (state.cart.length === 0) {
    listContainer.innerHTML = `<div class="empty-cart-msg">Your cart is empty. Add products from the Storefront!</div>`;
    return;
  }

  listContainer.innerHTML = state.cart.map(item => `
    <div class="cart-item">
      <div style="flex:1;">
        <div class="cart-item-title">${item.emoji} ${item.name}</div>
        <div class="cart-item-meta" style="color:var(--text-muted); font-size:0.78rem; margin-top:0.2rem;">
          ${CURRENCY.format(item.price)} each &bull; Line Total: <strong style="color:var(--color-primary);">${CURRENCY.format(item.qty * item.price)}</strong>
        </div>
        <div style="display:flex; align-items:center; gap:0.5rem; margin-top:0.5rem;">
          <button class="btn btn-sm btn-outline" style="padding:0.1rem 0.5rem; min-width:24px; font-weight:700;" onclick="changeCartQty('${item.productId}', -1)">-</button>
          <span style="font-weight:700; font-size:0.85rem; min-width:20px; text-align:center;">${item.qty}</span>
          <button class="btn btn-sm btn-outline" style="padding:0.1rem 0.5rem; min-width:24px; font-weight:700;" onclick="changeCartQty('${item.productId}', 1)">+</button>
          <button class="cart-item-remove" style="margin-left:auto; background:none; border:none; color:var(--color-danger); cursor:pointer; font-size:0.75rem; font-weight:600;" onclick="removeFromCart('${item.productId}')">✕ Remove</button>
        </div>
      </div>
    </div>
  `).join("");
}

// ============================================================================
// Ledger Tables Rendering (Requirements 18, 19, 20)
// ============================================================================
function renderOrdersTable() {
  const tbody = document.getElementById("ordersTableBody");
  if (!tbody) return;

  const countBadge = document.getElementById("orderCountBadge");
  if (countBadge) countBadge.textContent = state.orders.length;

  if (state.orders.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No orders in database. Place an order from the Storefront or run the Saga!</td></tr>`;
    return;
  }

  tbody.innerHTML = state.orders.map(order => `
    <tr>
      <td><code>${order.orderId}</code></td>
      <td>${order.customerEmail}</td>
      <td>${order.items}</td>
      <td><strong style="color:var(--color-primary);">${CURRENCY.format(order.totalAmount)}</strong></td>
      <td>
        <span class="status-chip ${order.status.toLowerCase()}">
          ${order.status === 'CONFIRMED' ? '✓' : order.status === 'CANCELLED' ? '✕' : '⏳'} ${order.status}
        </span>
      </td>
      <td>${order.createdAt}</td>
      <td>
        <button class="btn btn-sm btn-primary" onclick="inspectOrder('${order.orderId}')">Trace</button>
      </td>
    </tr>
  `).join("");
}

function renderInventoryTable() {
  const tbody = document.getElementById("inventoryTableBody");
  if (!tbody) return;

  const searchInput = document.getElementById("inventorySearchInput");
  const stockFilter = document.getElementById("inventoryStockFilter");
  const searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : "";
  const stockOption = stockFilter ? stockFilter.value : "ALL";

  let filtered = [...state.products];

  // Search filter (id, name, category, brand, description)
  if (searchQuery) {
    filtered = filtered.filter(p =>
      (p.id && p.id.toLowerCase().includes(searchQuery)) ||
      (p.name && p.name.toLowerCase().includes(searchQuery)) ||
      (p.category && p.category.toLowerCase().includes(searchQuery)) ||
      (p.brand && p.brand.toLowerCase().includes(searchQuery)) ||
      (p.description && p.description.toLowerCase().includes(searchQuery))
    );
  }

  // Stock level filter
  if (stockOption === "LOW") {
    filtered = filtered.filter(p => (p.stock || 0) < 15 && (p.stock || 0) > 0);
  } else if (stockOption === "OUT") {
    filtered = filtered.filter(p => (p.stock || 0) <= 0);
  } else if (stockOption === "HEALTHY") {
    filtered = filtered.filter(p => (p.stock || 0) >= 15);
  }

  // Update match count badge
  const matchBadge = document.getElementById("inventoryMatchBadge");
  if (matchBadge) {
    matchBadge.textContent = `${filtered.length} of ${state.products.length} items`;
  }

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
          <div style="font-size: 1.05rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.35rem;">No inventory records match</div>
          <div style="font-size: 0.82rem;">Try adjusting your search query or selecting a different stock level filter.</div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = filtered.map(prod => {
    const totalPhysical = (prod.stock || 0) + (prod.reserved || 0);
    const pct = Math.min(100, Math.round(((prod.stock || 0) / 30) * 100));
    const fillClass = pct < 25 ? 'danger' : pct < 50 ? 'warning' : '';

    return `
      <tr>
        <td><code>${prod.id}</code></td>
        <td>
          <div style="display:flex; align-items:center; gap:0.6rem;">
            <span style="font-size:1.2rem;">${prod.emoji || '📦'}</span>
            <div>
              <strong>${prod.name}</strong>
              <div style="font-size:0.75rem; color:var(--text-muted);">${prod.category}</div>
            </div>
          </div>
        </td>
        <td><span style="color:#34d399; font-weight:700;">${prod.stock}</span></td>
        <td><span style="color:#fbbf24; font-weight:600;">${prod.reserved}</span></td>
        <td><strong>${totalPhysical}</strong></td>
        <td>
          <div class="stock-bar-wrapper">
            <div class="stock-bar-bg">
              <div class="stock-bar-fill ${fillClass}" style="width: ${pct}%"></div>
            </div>
            <span style="font-size:0.75rem; color:var(--text-muted);">${pct}%</span>
          </div>
        </td>
        <td>
          <button class="btn btn-sm btn-primary" onclick="quickRestock('${prod.id}')">+10 Stock</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function quickRestock(productId) {
  const prod = state.products.find(p => p.id === productId);
  if (!prod) return;

  const newQty = (prod.stock || 0) + 10;

  try {
    const res = await fetch(`${API_BASE}/api/inventory`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        productId: productId,
        availableQuantity: newQty
      })
    });

    if (res.ok) {
      prod.stock = newQty;
      renderInventoryTable();
      renderProducts("ALL");
      logToConsole(`[Inventory Service :8083] Restocked +10 units for ${prod.name} in PostgreSQL. New Available: ${newQty}`, "dispatch");
    } else {
      prod.stock += 10;
      renderInventoryTable();
    }
  } catch (err) {
    prod.stock += 10;
    renderInventoryTable();
  }
}

function renderPaymentsTable() {
  const tbody = document.getElementById("paymentsTableBody");
  if (!tbody) return;

  if (state.payments.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No payment records in PostgreSQL yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.payments.map(tx => `
    <tr>
      <td><code>${tx.txId}</code></td>
      <td><code>${tx.orderId}</code></td>
      <td>${tx.customer}</td>
      <td><strong style="color:var(--color-primary);">${CURRENCY.format(tx.amount)}</strong></td>
      <td>
        <span class="status-chip ${tx.status === 'SUCCESS' || tx.status === 'COMPLETED' ? 'confirmed' : tx.status === 'FAILED' ? 'cancelled' : 'refunded'}">
          ${tx.status}
        </span>
      </td>
      <td>${tx.timestamp}</td>
      <td>
        ${tx.status === 'SUCCESS' || tx.status === 'COMPLETED'
          ? `<button class="btn btn-sm btn-danger" onclick="triggerRefund('${tx.orderId}')">Refund</button>`
          : `<span style="color:var(--text-dim); font-size:0.75rem;">N/A</span>`
        }
      </td>
    </tr>
  `).join("");
}

async function triggerRefund(orderId) {
  try {
    const res = await fetch(`${API_BASE}/api/payments/refund`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        orderId: orderId,
        reason: "Customer initiated manual refund via Control Center"
      })
    });

    if (res.ok) {
      logToConsole(`[Payment Service :8084] Refund processed for order ${orderId} in PostgreSQL. Status: REFUNDED`, "warn");
      loadPaymentsFromBackend();
    }
  } catch (err) {
    console.error("Refund error:", err);
  }
}

function inspectOrder(orderId) {
  document.querySelector('[data-tab="saga-tab"]')?.click();
  logToConsole(`[Trace Inspector] Inspecting Order lifecycle for: ${orderId}`, "info");
}

// ============================================================================
// Interactive Live Saga Choreography Engine
// ============================================================================
function initSagaControls() {
  const triggerBtn = document.getElementById("triggerSagaBtn");
  const clearBtn = document.getElementById("clearConsoleBtn");

  if (triggerBtn) {
    triggerBtn.addEventListener("click", () => {
      runLiveSaga(false);
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      const consoleBody = document.getElementById("sagaConsoleBody");
      if (consoleBody) consoleBody.innerHTML = "";
    });
  }
}

function logToConsole(message, type = "info") {
  const consoleBody = document.getElementById("sagaConsoleBody");
  if (!consoleBody) return;

  const now = new Date().toISOString().substring(11, 23);
  const line = document.createElement("div");
  line.className = `log-line ${type}`;
  line.innerHTML = `<span style="color:var(--text-dim)">[${now}]</span> ${message}`;
  consoleBody.appendChild(line);
  consoleBody.scrollTop = consoleBody.scrollHeight;
}

function resetSagaVisualizer() {
  const nodes = ["gateway", "order", "kafka", "inventory", "payment", "notification"];
  nodes.forEach(id => {
    const el = document.getElementById(`node-${id}`);
    const st = document.getElementById(`status-${id}`);
    if (el) el.className = `saga-node ${id === 'kafka' ? 'kafka-hub' : ''}`;
    if (st) st.textContent = id === 'kafka' ? 'Listening' : 'Idle';
  });

  for (let i = 1; i <= 5; i++) {
    const arr = document.getElementById(`arrow-${i}`);
    if (arr) {
      arr.className = "saga-arrow";
      arr.textContent = "➔";
    }
  }
}

async function runLiveSaga(fromCart = false) {
  if (state.isSagaRunning) return;
  state.isSagaRunning = true;
  resetSagaVisualizer();

  const simulateFailure = document.getElementById("simulateFailureToggle")?.checked || false;
  const correlationId = "corr-" + Math.random().toString(36).substring(2, 10);
  const customerEmail = (state.user && state.user.username) 
    ? `${state.user.username}@domain.io` 
    : `customer.${Math.floor(100 + Math.random() * 899)}@domain.io`;

  let itemsPayload = [];

  if (fromCart && state.cart.length > 0) {
    itemsPayload = state.cart.map((c, idx) => ({
      productId: c.productId,
      quantity: c.qty,
      price: (simulateFailure && idx === 0) ? 9999.99 : c.price
    }));
  } else {
    const prod = state.products[0];
    const price = simulateFailure ? 9999.99 : prod.price; // 9999.99 triggers deterministic decline in payment service
    itemsPayload = [{
      productId: prod.id,
      quantity: 1,
      price: price
    }];
  }

  const triggerBtn = document.getElementById("triggerSagaBtn");
  if (triggerBtn) {
    triggerBtn.disabled = true;
    triggerBtn.innerHTML = `<span>⏳ Dispatching to API Gateway...</span>`;
  }

  logToConsole(`=================================================================`, "info");
  logToConsole(`[Ingress] Dispatching order request to API Gateway (:8080) | Correlation: ${correlationId}`, "dispatch");

  highlightSagaNode("gateway", "Validating & Routing", "active-processing");

  try {
    const response = await fetch(`${API_BASE}/api/orders`, {
      method: "POST",
      headers: getAuthHeaders({ "X-Correlation-Id": correlationId }),
      body: JSON.stringify({
        customerId: customerEmail,
        items: itemsPayload
      })
    });

    if (response.ok) {
      const order = await response.json();
      logToConsole(`[API Gateway ➔ Order Service :8082] Order created in PostgreSQL! ID: ${order.id}, Status: ${order.status}`, "success");
      highlightSagaNode("gateway", "200 OK (Forwarded)", "success-step", 1);
      highlightSagaNode("order", "State: PENDING", "active-processing", 2);

      loadOrdersFromBackend();

      if (fromCart) {
        state.cart = [];
        renderCart();
      }

    } else {
      const err = await response.text();
      logToConsole(`[API Gateway Error] HTTP ${response.status}: ${err}`, "error");
      highlightSagaNode("gateway", `HTTP ${response.status}`, "failed-step");
      completeSagaRun(false);
    }

  } catch (netErr) {
    logToConsole(`[Network Error] API Gateway at :8080: ${netErr.message}. Real Saga execution failed.`, "error");
    highlightSagaNode("gateway", "Unreachable (:8080)", "failed-step");
    logToConsole(`❌ Saga execution failed: API Gateway (:8080) is unreachable. No order was created. Retry after verifying Gateway and Order Service are running.`, "error");
    completeSagaRun(false);
  }
}

function completeSagaRun(success) {
  state.isSagaRunning = false;
  const triggerBtn = document.getElementById("triggerSagaBtn");
  if (triggerBtn) {
    triggerBtn.disabled = false;
    triggerBtn.innerHTML = `<span>▶ Run Live Saga Order</span>`;
  }
}

function highlightSagaNode(nodeId, statusText, statusClass, stepNum) {
  const node = document.getElementById(`node-${nodeId}`);
  const status = document.getElementById(`status-${nodeId}`);

  if (node) {
    node.className = `saga-node ${nodeId === 'kafka' ? 'kafka-hub' : ''} ${statusClass}`;
  }
  if (status) {
    status.textContent = statusText;
  }
  if (stepNum && stepNum <= 5) {
    const arr = document.getElementById(`arrow-${stepNum}`);
    if (arr) {
      arr.classList.add("active");
    }
  }
}

function renderKafkaEvents() {
  const terminal = document.getElementById("kafkaTerminalBody");
  if (!terminal) return;

  if (state.kafkaEvents.length === 0) {
    terminal.innerHTML = `<div class="event-record"><span class="time">[Listening]</span> Waiting for domain events on topic order-events...</div>`;
    return;
  }

  terminal.innerHTML = state.kafkaEvents.map(evt => `
    <div class="event-record">
      <span class="time">[${evt.timestamp || 'Now'}]</span>
      <span class="topic">[TOPIC: ${evt.topic}]</span>
      <span class="type">${evt.eventType}</span>
      <span class="key">Key: ${evt.key}</span>
      <div class="payload-preview">${JSON.stringify(evt.payload, null, 2)}</div>
    </div>
  `).join("");
}
