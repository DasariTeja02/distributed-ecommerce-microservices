package com.ecommerce.product.config;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Configuration
public class ProductDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);

    public static final List<Product> CATALOG = createCatalog();

    private static List<Product> createCatalog() {
        Instant now = Instant.now();
        return List.<Product>of(
            Product.builder().id("prod-101").name("Apple MacBook Pro 16\" M3 Max")
                    .description("16-core CPU, 40-core GPU, 48GB Unified Memory, 1TB SSD Storage.")
                    .category("ELECTRONICS").brand("Apple").price(new BigDecimal("349900.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-102").name("Dell XPS 15 OLED Touch")
                    .description("Intel Core i9-13900H, 32GB RAM, 1TB SSD, 3.5K OLED InfinityEdge.")
                    .category("ELECTRONICS").brand("Dell").price(new BigDecimal("219900.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-103").name("Samsung Galaxy S24 Ultra")
                    .description("Snapdragon 8 Gen 3, 200MP Camera, Galaxy AI, Titanium Gray, 512GB.")
                    .category("ELECTRONICS").brand("Samsung").price(new BigDecimal("129999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-104").name("Sony WH-1000XM5 Wireless Headphones")
                    .description("Industry-leading active noise canceling with Auto NC Optimizer, 30h battery.")
                    .category("ELECTRONICS").brand("Sony").price(new BigDecimal("29990.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-105").name("Apple Watch Ultra 2 Titanium")
                    .description("Rugged 49mm titanium case, precision dual-frequency GPS, 100m water resistant.")
                    .category("ELECTRONICS").brand("Apple").price(new BigDecimal("89900.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-106").name("Bose QuietComfort Ultra")
                    .description("Spatial audio with CustomTune sound calibration and world-class noise cancellation.")
                    .category("ELECTRONICS").brand("Bose").price(new BigDecimal("35900.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-107").name("OnePlus 12 5G Smartphone")
                    .description("Snapdragon 8 Gen 3, 16GB RAM, 512GB ROM, 5400mAh Battery with 100W SUPERVOOC.")
                    .category("ELECTRONICS").brand("OnePlus").price(new BigDecimal("64999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-108").name("JBL Charge 5 Portable Bluetooth Speaker")
                    .description("Bold JBL Original Pro Sound, IP67 waterproof and dustproof, 20 hours of playtime.")
                    .category("ELECTRONICS").brand("JBL").price(new BigDecimal("14999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-109").name("Keychron K2 Wireless Mechanical Keyboard")
                    .description("75% layout, Gateron G Pro mechanical switches, RGB backlighting, Mac/Windows layout.")
                    .category("ELECTRONICS").brand("Keychron").price(new BigDecimal("8499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-110").name("Logitech MX Master 3S Wireless Mouse")
                    .description("Quiet clicks, 8K DPI any-surface tracking, MagSpeed electromagnetic scrolling.")
                    .category("ELECTRONICS").brand("Logitech").price(new BigDecimal("9995.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-111").name("LG UltraFine 27\" 4K IPS Monitor")
                    .description("UHD 3840x2160, HDR400, USB-C 90W PD, DCI-P3 95% color gamut with ergonomic stand.")
                    .category("ELECTRONICS").brand("LG").price(new BigDecimal("32999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-112").name("Anker 737 24,000mAh Power Bank (140W)")
                    .description("Ultra-powerful 140W two-way fast charging, smart digital display, GaNPrime technology.")
                    .category("ELECTRONICS").brand("Anker").price(new BigDecimal("11999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-113").name("Classic Heavyweight Organic Cotton T-Shirt")
                    .description("240 GSM 100% combed organic cotton, pre-shrunk, breathable tailored regular fit.")
                    .category("FASHION").brand("Teja Originals").price(new BigDecimal("999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-114").name("Urban Streetwear Oversized Fleece Hoodie")
                    .description("400 GSM brushed fleece, double-layered hood, kangaroo pocket, relaxed dropped shoulders.")
                    .category("FASHION").brand("Teja Originals").price(new BigDecimal("2499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-115").name("Slim Fit Stretch Selvedge Denim Jeans")
                    .description("Japanese 13.5oz stretch selvedge denim, deep indigo dye, reinforced copper rivets.")
                    .category("FASHION").brand("DenimCraft").price(new BigDecimal("3299.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-116").name("Nike Air Max 270 Athletic Sneakers")
                    .description("Large Max Air unit for responsive cushioning, breathable engineered mesh upper.")
                    .category("FASHION").brand("Nike").price(new BigDecimal("12995.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-117").name("Waterproof Everyday Commuter Backpack (25L)")
                    .description("Ballistic nylon construction, dedicated 16\" padded laptop compartment, luggage pass-through.")
                    .category("FASHION").brand("AeroTravel").price(new BigDecimal("4499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-118").name("Premium Pure Linen Casual Button-Down Shirt")
                    .description("100% European flax linen, garment-washed for extreme softness, lightweight resort collar.")
                    .category("FASHION").brand("Teja Originals").price(new BigDecimal("1999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-119").name("Minimalist Smart LED Desk Lamp")
                    .description("Touch dimmable, 5 color temperature modes, 10W wireless charging base, flicker-free CRI 95.")
                    .category("HOME").brand("Lumina").price(new BigDecimal("2999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-120").name("Stainless Steel Vacuum Insulated Bottle (1L)")
                    .description("Double-wall 18/8 food-grade stainless steel, keeps cold for 24h and hot for 12h, sweat-proof.")
                    .category("HOME").brand("HydroPure").price(new BigDecimal("1299.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-121").name("Handcrafted Matte Ceramic Coffee Mug (350ml)")
                    .description("Artisanal stoneware ceramic, ergonomic handle, microwave and dishwasher safe.")
                    .category("HOME").brand("ClayHouse").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-122").name("Solid Walnut Wood Desk Organizer Tray")
                    .description("Precision-carved natural American walnut wood, phone dock, cable groove, felt bottom.")
                    .category("HOME").brand("GroveCraft").price(new BigDecimal("2199.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-123").name("Ambient Smart RGBIC LED Light Bar Set")
                    .description("Smart voice control via Wi-Fi, audio-reactive music sync, dynamic gradient scene presets.")
                    .category("HOME").brand("GlowSync").price(new BigDecimal("3499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-124").name("Magnetic Slim Armor Protective Phone Case")
                    .description("Military-grade 10ft drop protection, MagSafe magnetic ring array, soft tactile grip.")
                    .category("ACCESSORIES").brand("ArmorFit").price(new BigDecimal("1199.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-125").name("Braided 100W USB-C to USB-C Fast Cable (2m)")
                    .description("E-Marker smart chip, nylon braided tensile durability 20,000+ bends, 480Mbps data transfer.")
                    .category("ACCESSORIES").brand("HyperCord").price(new BigDecimal("799.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-126").name("Ergonomic Folding Aluminum Laptop Stand")
                    .description("Aerospace-grade sandblasted aluminum, 6 height adjustment levels, optimal heat dissipation.")
                    .category("ACCESSORIES").brand("ErgoPro").price(new BigDecimal("2499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-127").name("GaN 65W 3-Port Fast Wall Charger")
                    .description("Dual USB-C PD 3.0 + USB-A QC 4+, foldable prongs, charges laptops and phones simultaneously.")
                    .category("ACCESSORIES").brand("Anker").price(new BigDecimal("2799.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-128").name("Gentle Foaming Facial Cleanser (150ml)")
                    .description("Hydrating amino acid formula with soothing chamomile and tea tree extract for clear skin.")
                    .category("BEAUTY").brand("CeraGlow").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-129").name("Hyaluronic Acid Hydrating Face Moisturizer")
                    .description("Triple molecular hyaluronic acid locking in 72-hour deep moisture with vitamin B5.")
                    .category("BEAUTY").brand("AquaDerma").price(new BigDecimal("899.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-130").name("Ultra-Light Matte Daily Sunscreen SPF 50+")
                    .description("Broad spectrum UVA/UVB protection with invisible zero white cast and sebum control.")
                    .category("BEAUTY").brand("SolGuard").price(new BigDecimal("749.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-131").name("Moroccan Argan Oil Nourishing Shampoo")
                    .description("Sulfate-free keratin restorative shampoo enriched with cold-pressed organic argan oil.")
                    .category("BEAUTY").brand("Botanica").price(new BigDecimal("649.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-132").name("Shea Butter Deep Moisture Body Lotion (400ml)")
                    .description("Ultra-rich whipped African shea butter and sweet almond oil for velvety soft skin.")
                    .category("BEAUTY").brand("SilkVelvet").price(new BigDecimal("599.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1526947425960-945c6e72858f?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-133").name("Luxury Eau De Parfum Woody Citrus (100ml)")
                    .description("Artisanal blend of Italian bergamot, cedarwood, amber resin, and crisp vetiver.")
                    .category("BEAUTY").brand("Maison Noir").price(new BigDecimal("2999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-134").name("Ionic Professional Salon Hair Dryer")
                    .description("2200W brushless motor with negative ion tech for frizz-free salon blowouts in minutes.")
                    .category("BEAUTY").brand("ProStylist").price(new BigDecimal("2499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-135").name("FIFA Quality Pro Match Football (Size 5)")
                    .description("Thermally bonded seamless surface with balanced butyl bladder for true flight trajectory.")
                    .category("SPORTS").brand("Puma").price(new BigDecimal("1999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-136").name("English Willow Grade 1 Cricket Bat")
                    .description("Handcrafted unbleached Grade 1 English willow with thick contoured edges and huge sweet spot.")
                    .category("SPORTS").brand("SG Sports").price(new BigDecimal("4999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-137").name("High-Impact Leather Cricket Ball (Pack of 2)")
                    .description("Alum tanned 4-piece Portuguese cork center match ball with hand-stitched seam.")
                    .category("SPORTS").brand("Kookaburra").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-138").name("Carbon Fiber Performance Tennis Racket")
                    .description("300g graphite composite frame with isometric head shape for optimal spin and control.")
                    .category("SPORTS").brand("Wilson").price(new BigDecimal("3499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-139").name("High-Density Non-Slip Eco Yoga Mat (6mm)")
                    .description("Textured natural tree rubber with laser-engraved alignment lines and carry strap.")
                    .category("SPORTS").brand("Manduka").price(new BigDecimal("1299.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-140").name("Cast Iron Hexagonal Dumbbell Set (Pair 10kg)")
                    .description("Anti-roll solid iron hex heads with knurled ergonomic chrome handles for secure grip.")
                    .category("SPORTS").brand("FitIron").price(new BigDecimal("2499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1586401100295-7a8096fd231a?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-141").name("Heavy Duty Resistance Bands Workout Set")
                    .description("Stackable 5-color tube bands up to 150 lbs with foam handles, door anchor, and ankle straps.")
                    .category("SPORTS").brand("FlexPro").price(new BigDecimal("899.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-142").name("Clean Architecture & Domain Design Guide")
                    .description("Comprehensive software engineering manual for microservices, DDD, and SOLID principles.")
                    .category("BOOKS").brand("TechPress").price(new BigDecimal("999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-143").name("Modern Java & Spring Boot Microservices")
                    .description("Master modern Java 21, Spring Cloud Gateway, Kafka Saga choreography, and Kubernetes.")
                    .category("BOOKS").brand("O'Reilly Media").price(new BigDecimal("1499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-144").name("High-Performance Python Distributed Systems")
                    .description("In-depth guide to asyncio, multiprocessing, distributed task queues, and performance tuning.")
                    .category("BOOKS").brand("Packt Publishing").price(new BigDecimal("1299.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-145").name("Data Structures & Algorithms in Depth")
                    .description("Rigorous technical reference covering trees, graphs, dynamic programming, and complexity.")
                    .category("BOOKS").brand("MIT Press").price(new BigDecimal("1199.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-146").name("Premium Hardcover Dotted Grid Journal (A5)")
                    .description("120 GSM acid-free ink-proof paper with dual ribbon bookmarks, pen loop, and expandable pocket.")
                    .category("BOOKS").brand("Leuchtturm").price(new BigDecimal("599.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-147").name("Matte Black Executive Fountain Pen Set")
                    .description("Hand-finished medium iridium nib with piston converter and cartridge compatibility.")
                    .category("BOOKS").brand("Parker").price(new BigDecimal("899.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-148").name("Undated Daily Productivity Desk Planner")
                    .description("Habit tracking, priority matrix, time-blocking grid, and weekly review reflection spreads.")
                    .category("BOOKS").brand("Papercraft").price(new BigDecimal("499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-149").name("Single Origin Arabica Whole Bean Coffee (500g)")
                    .description("Medium-dark roasted Chikmagalur estate beans with tasting notes of dark cocoa and berries.")
                    .category("GROCERY").brand("Blue Tokai").price(new BigDecimal("799.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1587734195503-904fca47e0e9?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-150").name("Darjeeling Premium Organic Black Tea (250g)")
                    .description("Single-estate summer muscatel harvest offering fragrant floral aroma and golden liquor.")
                    .category("GROCERY").brand("Makaibari").price(new BigDecimal("649.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-151").name("Himalayan Aged Long Grain Basmati Rice (5kg)")
                    .description("Naturally aged for 24 months for extra-long fluffiness and rich authentic culinary aroma.")
                    .category("GROCERY").brand("Daawat").price(new BigDecimal("749.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-152").name("Italian Bronze-Cut Penne Rigate Pasta (1kg)")
                    .description("100% durum wheat semolina extruded through bronze dies for optimal sauce adhesion.")
                    .category("GROCERY").brand("Barilla").price(new BigDecimal("399.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1612927601601-6638404737ce?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-153").name("Cold Pressed Extra Virgin Spanish Olive Oil (1L)")
                    .description("First cold press Picual olives with low acidity, robust fruity flavor, and high antioxidants.")
                    .category("GROCERY").brand("Borges").price(new BigDecimal("1199.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-154").name("Artisanal 70% Dark Single Origin Belgian Chocolate")
                    .description("Conched premium cocoa nibs with rich Madagascar vanilla and unrefined cane sugar.")
                    .category("GROCERY").brand("Callebaut").price(new BigDecimal("349.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1548907040-4baa42d10919?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-155").name("Gourmet Roasted Salted Almonds & Cashews (500g)")
                    .description("Slow dry-roasted jumbo nuts seasoned with hand-harvested pink Himalayan rock salt.")
                    .category("GROCERY").brand("NuttyNaturals").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1509358271058-acd22cc93898?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-156").name("Magnetic Wireless Fast Car Charger Vent Mount")
                    .description("15W fast inductive charging with ultra-strong neodymium magnetic lock and 360-degree ball joint.")
                    .category("AUTOMOTIVE").brand("Spigen").price(new BigDecimal("1499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-157").name("Ultra HD 4K Dash Camera with Night Vision")
                    .description("Sony STARVIS 2 sensor, 170-degree ultra-wide angle, emergency G-sensor, and built-in Wi-Fi GPS.")
                    .category("AUTOMOTIVE").brand("70mai").price(new BigDecimal("4999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-158").name("Cordless High-Power Portable Car Vacuum Cleaner")
                    .description("9000Pa cyclone suction, washable HEPA filter, dual crevice nozzles, and 30-min run time.")
                    .category("AUTOMOTIVE").brand("Baseus").price(new BigDecimal("1899.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-159").name("Digital Preset Portable Tyre Inflator Pump")
                    .description("150 PSI rapid inflation with digital pressure gauge auto-shutoff and emergency SOS torch.")
                    .category("AUTOMOTIVE").brand("Michelin").price(new BigDecimal("2499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-160").name("Dual Port 60W USB-C PD Fast Car Charger")
                    .description("All-metal compact body with smart power allocation supporting PPS and Quick Charge 4.0.")
                    .category("AUTOMOTIVE").brand("Anker").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-161").name("Premium Microfiber Car Detailing Towel Kit")
                    .description("Pack of 6 edgeless 800 GSM plush towels for scratch-free buffing, waxing, and drying.")
                    .category("AUTOMOTIVE").brand("Chemical Guys").price(new BigDecimal("499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1607860108855-64acf2078ed9?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-162").name("Organic Essential Oil Vent Car Air Freshener")
                    .description("Subtle cedar and fresh lavender botanical extract diffusing natural scent for up to 60 days.")
                    .category("AUTOMOTIVE").brand("Little Trees").price(new BigDecimal("399.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1615397349754-cfa2066a298e?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-163").name("Lightweight Polycarbonate Cabin Suitcase (55cm)")
                    .description("Scratch-resistant German Makrolon shell with 360-degree silent spinner wheels and TSA lock.")
                    .category("TRAVEL").brand("American Tourister").price(new BigDecimal("4499.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-164").name("Expandable Hardside Check-in Luggage (75cm)")
                    .description("Heavy duty 105L volume with interior compression dividers and aircraft-grade aluminum trolley.")
                    .category("TRAVEL").brand("Samsonite").price(new BigDecimal("6999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1581553680321-4fffae59fccd?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-165").name("Water-Resistant Convertible Travel Duffel (45L)")
                    .description("Weatherproof 900D tarpaulin with stowable ergonomic backpack straps and separate shoe pocket.")
                    .category("TRAVEL").brand("The North Face").price(new BigDecimal("2299.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-166").name("RFID Blocking Leather Passport Travel Wallet")
                    .description("Full-grain vegetable tanned leather holding passport, boarding passes, SIM cards, and cash.")
                    .category("TRAVEL").brand("Bellroy").price(new BigDecimal("799.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-167").name("Waterproof 6-Piece Packing Cubes Organizer")
                    .description("Breathable ripstop nylon cubes with double compression zippers saving 60% luggage space.")
                    .category("TRAVEL").brand("Eagle Creek").price(new BigDecimal("999.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-168").name("Ergonomic Memory Foam Travel Neck Pillow")
                    .description("360-degree orthopedic chin support with breathable magnetic therapy fabric and earplug kit.")
                    .category("TRAVEL").brand("Cabeau").price(new BigDecimal("699.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-169").name("TSA Approved Leak-Proof Silicone Travel Bottles")
                    .description("Set of 4 food-grade squeezable 90ml bottles with triple-sealing caps and ID window.")
                    .category("TRAVEL").brand("GoToob").price(new BigDecimal("449.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build(),
            Product.builder().id("prod-170").name("Heavy Duty Luggage Strap with Combination Lock")
                    .description("Durable cross-woven nylon strap with 3-digit customizable security lock and luggage tag.")
                    .category("TRAVEL").brand("TravelSmart").price(new BigDecimal("399.00"))
                    .available(true).imageUrl("https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=600&q=80").createdAt(now).updatedAt(now).build()
        );
    }

    @Bean
    public CommandLineRunner initProductDatabase(ProductRepository productRepository) {
        return args -> {
            try {
                int inserted = 0;
                int updated = 0;
                for (Product p : CATALOG) {
                    if (!productRepository.existsById(p.getId())) {
                        productRepository.save(p);
                        inserted++;
                    } else {
                        productRepository.findById(p.getId()).ifPresent(existing -> {
                            existing.setImageUrl(p.getImageUrl());
                            existing.setDescription(p.getDescription());
                            existing.setPrice(p.getPrice());
                            existing.setBrand(p.getBrand());
                            productRepository.save(existing);
                        });
                        updated++;
                    }
                }
                log.info("Catalog synchronization completed: {} new products inserted, {} updated with real photography. Total active products in MongoDB: {}", 
                    inserted, updated, productRepository.count());
            } catch (Exception e) {
                log.warn("MongoDB is currently offline or unreachable ({}). Product Service will use in-memory fallback catalog.", e.getMessage());
            }
        };
    }
}
