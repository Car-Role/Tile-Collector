# Setup Instructions

## Panel Icon

The plugin requires a panel icon at `src/main/resources/panel_icon.png`. 

To create the icon:
1. Create a 16x16 or 32x32 PNG image
2. Design it to represent tile collection (e.g., a grid pattern, footsteps, or map icon)
3. Save it as `src/main/resources/panel_icon.png`

Alternatively, you can use any existing RuneLite plugin icon as a temporary placeholder.

## Building the Plugin

1. Ensure you have Java 11 or higher installed
2. Run the build command:
   ```bash
   ./gradlew build
   ```
   On Windows:
   ```bash
   gradlew.bat build
   ```

3. The compiled JAR will be in `build/libs/Tile-Collector-1.0.0.jar`

## Testing the Plugin

1. Copy the JAR to your RuneLite plugins folder:
   - Windows: `%USERPROFILE%\.runelite\plugins`
   - macOS/Linux: `~/.runelite/plugins`

2. Start RuneLite
3. The plugin should appear in the plugin list
4. Enable "Tile Collector"
5. Click the panel icon in the sidebar to open the stats panel

## Submitting to RuneLite Plugin Hub

Before submitting:
1. Create a proper panel icon (16x16 or 32x32 PNG)
2. Test thoroughly in-game
3. Ensure all features work as expected
4. Follow RuneLite's plugin hub submission guidelines

## Known Issues

- Some deprecated API warnings exist but don't affect functionality
- The walkable tile detection uses collision data which should work for most scenarios
- Very large numbers of visited tiles may impact performance (consider implementing data compression)

## Future Enhancements

Potential improvements:
- Add heatmap visualization for frequently visited areas
- Export/import visited tile data
- Per-region statistics
- Achievement system for exploration milestones
- Integration with world map overlay
