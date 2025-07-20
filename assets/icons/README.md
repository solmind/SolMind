# SolMind App Icons

This directory contains SVG icon designs for the SolMind application.

## Icon Variants

### 1. `solmind-icon.svg` (512x512)
- **Style:** Detailed brain design with neural connections
- **Features:** Dual brain hemispheres, neural nodes, glowing effects
- **Best for:** High-resolution displays, marketing materials
- **Colors:** Full Solana gradient (purple to green to blue)

### 2. `solmind-icon-simple.svg` (512x512)
- **Style:** Simplified brain outline
- **Features:** Clean brain shape with minimal neural patterns
- **Best for:** Mobile app icons, favicons
- **Colors:** Simplified Solana gradient (purple to green)

### 3. `solmind-monogram.svg` (512x512)
- **Style:** "SM" text monogram with subtle brain elements
- **Features:** Bold typography with background brain hints
- **Best for:** Social media profiles, compact spaces
- **Colors:** Full Solana gradient with glowing effects

## Usage Guidelines

### Mobile App Icons
- Use `solmind-icon-simple.svg` for best clarity at small sizes
- Convert to PNG at required resolutions: 192x192, 144x144, 96x96, 72x72, 48x48

### Web/Desktop
- Use `solmind-icon.svg` for full detail
- Can be used as favicon when converted to ICO format

### Brand Identity
- `solmind-monogram.svg` for letterheads, business cards
- All variants maintain consistent Solana brand colors

## Color Palette
- **Primary Purple:** #9945FF
- **Solana Green:** #14F195  
- **Secondary Blue:** #00D4FF
- **Background:** Gradient from #667eea to #764ba2

## Converting to Other Formats

### PNG Conversion
```bash
# Using Inkscape
inkscape --export-png=icon-192.png --export-width=192 --export-height=192 solmind-icon-simple.svg

# Using ImageMagick
convert -background transparent solmind-icon-simple.svg -resize 192x192 icon-192.png
```

### ICO Conversion (for favicon)
```bash
# Convert SVG to ICO
convert solmind-icon-simple.svg -define icon:auto-resize=16,32,48,64,128 favicon.ico
```

## Design Principles
- **Scalability:** All icons are vector-based for infinite scalability
- **Brand Consistency:** Uses official Solana color palette
- **Readability:** Simple variants ensure clarity at small sizes
- **Modern Aesthetic:** Clean, gradient-based design matching app UI