const fs = require('fs');
const path = require('path');

const lines = fs.readFileSync('generate-initializers.js', 'utf8').split('\n');
const arrayCode = lines.slice(3, 654).join('\n').replace(/^const catalogItems\s*=\s*/, '').replace(/;\s*$/, '');
const rawItems = eval('(' + arrayCode + ')');

const EMOJI_BY_CAT = {
  "ELECTRONICS": "💻",
  "FASHION": "👕",
  "HOME": "🏡",
  "ACCESSORIES": "🔌",
  "BEAUTY": "💄",
  "SPORTS": "🏃",
  "BOOKS": "📚",
  "GROCERY": "🍎",
  "AUTOMOTIVE": "🚗",
  "TRAVEL": "✈️"
};

const formattedProducts = rawItems.map(item => {
  const name = item.name.replace(/\\"/g, '"');
  const desc = item.description.replace(/\\"/g, '"');
  const price = parseFloat(item.price);
  const emoji = EMOJI_BY_CAT[item.category] || "📦";
  return {
    id: item.id,
    name: name,
    category: item.category,
    price: price,
    stock: 25,
    reserved: 0,
    emoji: emoji,
    description: desc,
    imageUrl: item.imageUrl
  };
});

let appJs = fs.readFileSync('frontend/app.js', 'utf8');

// 1. Update EMOJI_MAP
const oldEmojiMapRegex = /\/\/ Catalog emoji lookup[\s\S]*?const EMOJI_MAP = \{[\s\S]*?\};/;
const newEmojiMap = `// Catalog emoji lookup
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
};`;

appJs = appJs.replace(oldEmojiMapRegex, newEmojiMap);

// 2. Update state.products
const oldProductsRegex = /products:\s*\[[\s\S]*?\n  \],\s*\n  cart:/;
const productsJson = JSON.stringify(formattedProducts, null, 4)
  .split('\n')
  .map((line, idx) => (idx === 0 ? line : '    ' + line))
  .join('\n');

const newProductsBlock = `products: ${productsJson},\n  cart:`;
appJs = appJs.replace(oldProductsRegex, newProductsBlock);

// 3. Update loadProductsFromBackend mapping to include imageUrl
const oldLoadProductsRegex = /state\.products = data\.map\(p => \(\{[\s\S]*?\}\)\);/;
const newLoadProductsCode = `state.products = data.map(p => ({
          id: p.id,
          name: p.name,
          category: p.category || "ELECTRONICS",
          price: Number(p.price) || 0,
          stock: 25,
          reserved: 0,
          emoji: EMOJI_MAP[p.category] || "📦",
          description: p.description,
          imageUrl: p.imageUrl || ""
        }));`;

appJs = appJs.replace(oldLoadProductsRegex, newLoadProductsCode);

// 4. Update renderProducts image rendering
const oldCardHtmlRegex = /<div class="product-badge">\$\{prod\.category\}<\/div>\s*<div class="product-icon">\$\{prod\.emoji\}<\/div>\s*<div class="product-body">/;
const newCardHtml = `<div class="product-badge">\${prod.category}</div>
      <div class="product-img-wrapper">
        <img src="\${prod.imageUrl || ''}" alt="\${prod.name}" class="product-img" loading="lazy" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
        <div class="product-img-fallback">\${prod.emoji || '📦'}</div>
      </div>
      <div class="product-body">`;

appJs = appJs.replace(oldCardHtmlRegex, newCardHtml);

// 5. Update nav product count badge
const oldCountBadgeRegex = /const countBadge = document\.getElementById\("productCountBadge"\);\s*if \(countBadge\) countBadge\.textContent = filtered\.length;/;
const newCountBadgeCode = `const countBadge = document.getElementById("productCountBadge");
  if (countBadge) countBadge.textContent = state.products ? state.products.length : filtered.length;`;

appJs = appJs.replace(oldCountBadgeRegex, newCountBadgeCode);

fs.writeFileSync('frontend/app.js', appJs, 'utf8');
console.log('Successfully updated frontend/app.js with 70 products and photography display!');
