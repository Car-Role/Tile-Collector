# Quick Start Guide

## What You Need to Do Before Building

### 1. Create the Panel Icon
The plugin needs an icon file at `src/main/resources/panel_icon.png`

**Quick Solution:**
- Create a simple 16x16 or 32x32 PNG image
- You can use any image editor or online tool
- Suggested design: A grid pattern, footsteps, or map marker
- Save as `src/main/resources/panel_icon.png`

**Temporary Workaround:**
You can copy an icon from another RuneLite plugin or use a placeholder image temporarily.

### 2. Build the Plugin
```bash
# On Windows
gradlew.bat build

# On macOS/Linux
./gradlew build
```

The JAR file will be created at: `build/libs/Tile-Collector-1.0.0.jar`

### 3. Install in RuneLite
1. Copy the JAR to your RuneLite plugins folder:
   - **Windows**: `%USERPROFILE%\.runelite\plugins`
   - **macOS/Linux**: `~/.runelite/plugins`
2. Restart RuneLite
3. Find "Tile Collector" in the plugin list and enable it

## How to Use

1. **Enable the Plugin**
   - Open RuneLite's plugin panel (wrench icon)
   - Search for "Tile Collector"
   - Toggle it on

2. **Open the Stats Panel**
   - Click the Tile Collector icon in the sidebar
   - View your exploration statistics

3. **Start Exploring**
   - Walk around in-game
   - Yellow highlighted tiles = not visited yet
   - Walk on them to mark them as visited
   - Watch your stats increase!

4. **Customize Settings**
   - Right-click the plugin in the plugin list
   - Click "Configure"
   - Adjust colors, render distance, etc.

## What the Plugin Does

- **Highlights unvisited tiles** in yellow (customizable)
- **Tracks your progress** - shows how many tiles you've walked on
- **Saves your data** - progress persists between sessions
- **Shows statistics** - total tiles, scene exploration percentage
- **Reset option** - start fresh anytime

## Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| Highlight Color | Color of unvisited tiles | Yellow (transparent) |
| Border Color | Border color for tiles | Yellow (opaque) |
| Fill Tiles | Fill tiles with color | Yes |
| Show Border | Show tile borders | Yes |
| Border Width | Thickness of borders | 2 |
| Only Current Plane | Show only current floor | Yes |
| Max Render Distance | How far to show tiles | 20 tiles |

## Tips

- **Performance**: If you experience lag, reduce the render distance
- **Visibility**: Adjust colors if tiles are hard to see in certain areas
- **Exploration**: The plugin works everywhere - cities, wilderness, dungeons
- **Multi-floor**: Each floor level is tracked separately
- **Reset**: Use the reset button to start a fresh exploration challenge

## Troubleshooting

**Plugin won't load:**
- Ensure you have Java 11 or higher
- Check that the JAR is in the correct plugins folder
- Restart RuneLite completely

**No tiles showing:**
- Make sure the plugin is enabled
- Check that you're in a walkable area
- Verify render distance isn't set to 0
- Try adjusting the highlight color

**Stats not updating:**
- The panel updates every game tick
- Try walking to a new tile
- Check that you're logged into the game

**Data not saving:**
- Data saves automatically on plugin shutdown
- Make sure RuneLite closes properly
- Check RuneLite has write permissions to its config folder

## Next Steps

Once you've tested the plugin and it works well:

1. Create a proper icon (make it look nice!)
2. Test extensively in different areas
3. Consider submitting to the RuneLite Plugin Hub
4. Share with the OSRS community!

## Support

For issues or questions:
- Check the PLUGIN_OVERVIEW.md for technical details
- Review the README.md for feature information
- Test in different game scenarios
- Consider contributing improvements!

Enjoy exploring every tile in Gielinor! 🗺️
