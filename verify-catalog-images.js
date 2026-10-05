const https = require('https');

const products = [
  // 1. ELECTRONICS (prod-101 - prod-112)
  {
    id: "prod-101",
    name: "Apple MacBook Pro 16\" M3 Max",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-102",
    name: "Dell XPS 15 OLED Touch",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-103",
    name: "Samsung Galaxy S24 Ultra",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-104",
    name: "Sony WH-1000XM5 Wireless Headphones",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-105",
    name: "Apple Watch Ultra 2 Titanium",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-106",
    name: "Bose QuietComfort Ultra",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-107",
    name: "OnePlus 12 5G Smartphone",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-108",
    name: "JBL Charge 5 Portable Bluetooth Speaker",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-109",
    name: "Keychron K2 Wireless Mechanical Keyboard",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-110",
    name: "Logitech MX Master 3S Wireless Mouse",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-111",
    name: "LG UltraFine 27\" 4K IPS Monitor",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-112",
    name: "Anker 737 24,000mAh Power Bank (140W)",
    category: "ELECTRONICS",
    imageUrl: "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=600&q=80"
  },

  // 2. FASHION (prod-113 - prod-118)
  {
    id: "prod-113",
    name: "Classic Heavyweight Organic Cotton T-Shirt",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-114",
    name: "Urban Streetwear Oversized Fleece Hoodie",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-115",
    name: "Slim Fit Stretch Selvedge Denim Jeans",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-116",
    name: "Nike Air Max 270 Athletic Sneakers",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-117",
    name: "Waterproof Everyday Commuter Backpack (25L)",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-118",
    name: "Premium Pure Linen Casual Button-Down Shirt",
    category: "FASHION",
    imageUrl: "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=600&q=80"
  },

  // 3. HOME (prod-119 - prod-123)
  {
    id: "prod-119",
    name: "Minimalist Smart LED Desk Lamp",
    category: "HOME",
    imageUrl: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-120",
    name: "Stainless Steel Vacuum Insulated Bottle (1L)",
    category: "HOME",
    imageUrl: "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-121",
    name: "Handcrafted Matte Ceramic Coffee Mug (350ml)",
    category: "HOME",
    imageUrl: "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-122",
    name: "Solid Walnut Wood Desk Organizer Tray",
    category: "HOME",
    imageUrl: "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-123",
    name: "Ambient Smart RGBIC LED Light Bar Set",
    category: "HOME",
    imageUrl: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80"
  },

  // 4. ACCESSORIES (prod-124 - prod-127)
  {
    id: "prod-124",
    name: "Magnetic Slim Armor Protective Phone Case",
    category: "ACCESSORIES",
    imageUrl: "https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-125",
    name: "Braided 100W USB-C to USB-C Fast Cable (2m)",
    category: "ACCESSORIES",
    imageUrl: "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-126",
    name: "Ergonomic Folding Aluminum Laptop Stand",
    category: "ACCESSORIES",
    imageUrl: "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-127",
    name: "GaN 65W 3-Port Fast Wall Charger",
    category: "ACCESSORIES",
    imageUrl: "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80"
  },

  // 5. BEAUTY & PERSONAL CARE (prod-128 - prod-134)
  {
    id: "prod-128",
    name: "Gentle Foaming Facial Cleanser (150ml)",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-129",
    name: "Hyaluronic Acid Hydrating Face Moisturizer",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-130",
    name: "Ultra-Light Matte Daily Sunscreen SPF 50+",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-131",
    name: "Moroccan Argan Oil Nourishing Shampoo",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-132",
    name: "Shea Butter Deep Moisture Body Lotion (400ml)",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1526947425960-945c6e72858f?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-133",
    name: "Luxury Eau De Parfum Woody Citrus (100ml)",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-134",
    name: "Ionic Professional Salon Hair Dryer",
    category: "BEAUTY",
    imageUrl: "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&w=600&q=80"
  },

  // 6. SPORTS & FITNESS (prod-135 - prod-141)
  {
    id: "prod-135",
    name: "FIFA Quality Pro Match Football (Size 5)",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-136",
    name: "English Willow Grade 1 Cricket Bat",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-137",
    name: "High-Impact Leather Cricket Ball (Pack of 2)",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-138",
    name: "Carbon Fiber Performance Tennis Racket",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-139",
    name: "High-Density Non-Slip Eco Yoga Mat (6mm)",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-140",
    name: "Cast Iron Hexagonal Dumbbell Set (Pair 10kg)",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1586401100295-7a8096fd231a?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-141",
    name: "Heavy Duty Resistance Bands Workout Set",
    category: "SPORTS",
    imageUrl: "https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=600&q=80"
  },

  // 7. BOOKS & STATIONERY (prod-142 - prod-148)
  {
    id: "prod-142",
    name: "Clean Architecture & Domain Design Guide",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-143",
    name: "Modern Java & Spring Boot Microservices",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-144",
    name: "High-Performance Python Distributed Systems",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-145",
    name: "Data Structures & Algorithms in Depth",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-146",
    name: "Premium Hardcover Dotted Grid Journal (A5)",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-147",
    name: "Matte Black Executive Fountain Pen Set",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-148",
    name: "Undated Daily Productivity Desk Planner",
    category: "BOOKS",
    imageUrl: "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=600&q=80"
  },

  // 8. GROCERY & FOOD (prod-149 - prod-155)
  {
    id: "prod-149",
    name: "Single Origin Arabica Whole Bean Coffee (500g)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1587734195503-904fca47e0e9?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-150",
    name: "Darjeeling Premium Organic Black Tea (250g)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-151",
    name: "Himalayan Aged Long Grain Basmati Rice (5kg)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-152",
    name: "Italian Bronze-Cut Penne Rigate Pasta (1kg)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1612927601601-6638404737ce?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-153",
    name: "Cold Pressed Extra Virgin Spanish Olive Oil (1L)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-154",
    name: "Artisanal 70% Dark Single Origin Belgian Chocolate",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1548907040-4baa42d10919?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-155",
    name: "Gourmet Roasted Salted Almonds & Cashews (500g)",
    category: "GROCERY",
    imageUrl: "https://images.unsplash.com/photo-1509358271058-acd22cc93898?auto=format&fit=crop&w=600&q=80"
  },

  // 9. AUTOMOTIVE (prod-156 - prod-162)
  {
    id: "prod-156",
    name: "Magnetic Wireless Fast Car Charger Vent Mount",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-157",
    name: "Ultra HD 4K Dash Camera with Night Vision",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-158",
    name: "Cordless High-Power Portable Car Vacuum Cleaner",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-159",
    name: "Digital Preset Portable Tyre Inflator Pump",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-160",
    name: "Dual Port 60W USB-C PD Fast Car Charger",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-161",
    name: "Premium Microfiber Car Detailing Towel Kit",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1607860108855-64acf2078ed9?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-162",
    name: "Organic Essential Oil Vent Car Air Freshener",
    category: "AUTOMOTIVE",
    imageUrl: "https://images.unsplash.com/photo-1615397349754-cfa2066a298e?auto=format&fit=crop&w=600&q=80"
  },

  // 10. TRAVEL & LUGGAGE (prod-163 - prod-170)
  {
    id: "prod-163",
    name: "Lightweight Polycarbonate Cabin Suitcase (55cm)",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-164",
    name: "Expandable Hardside Check-in Luggage (75cm)",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1581553680321-4fffae59fccd?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-165",
    name: "Water-Resistant Convertible Travel Duffel (45L)",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-166",
    name: "RFID Blocking Leather Passport Travel Wallet",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-167",
    name: "Waterproof 6-Piece Packing Cubes Organizer",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-168",
    name: "Ergonomic Memory Foam Travel Neck Pillow",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-169",
    name: "TSA Approved Leak-Proof Silicone Travel Bottles",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: "prod-170",
    name: "Heavy Duty Luggage Strap with Combination Lock",
    category: "TRAVEL",
    imageUrl: "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=600&q=80"
  }
];

console.log(`Verifying ${products.length} product image URLs...`);

async function checkAll() {
  let ok = 0;
  let fail = 0;
  for (const p of products) {
    try {
      const code = await new Promise((resolve) => {
        const req = https.get(p.imageUrl, (res) => {
          res.resume();
          resolve(res.statusCode);
        });
        req.on('error', () => resolve(500));
        req.setTimeout(5000, () => {
          req.destroy();
          resolve(408);
        });
      });
      if (code === 200) {
        ok++;
      } else {
        fail++;
        console.error(`FAILED: ${p.id} ${p.name} -> HTTP ${code}`);
      }
    } catch (e) {
      fail++;
      console.error(`ERROR: ${p.id} ${p.name} -> ${e.message}`);
    }
  }
  console.log(`Results: ${ok} SUCCESS, ${fail} FAILED out of ${products.length}`);
}

checkAll();
