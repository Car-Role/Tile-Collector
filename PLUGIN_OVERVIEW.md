# Tile Collector Plugin - Technical Overview

## Plugin Architecture

### Core Components

1. **TileCollectorPlugin.java** - Main plugin class
   - Manages plugin lifecycle (startup/shutdown)
   - Tracks visited tiles using a HashSet of WorldPoints
   - Scans scenes for walkable tiles using collision data
   - Handles player movement tracking via GameTick events
   - Persists data to RuneLite config
   - Provides statistics calculations

2. **TileCollectorConfig.java** - Configuration interface
   - Highlight color customization
   - Border color and width settings
   - Fill/border toggle options
   - Render distance limits
   - Plane filtering options
   - Hidden data storage for visited tiles

3. **TileCollectorOverlay.java** - Rendering overlay
   - Draws highlighted tiles in the game world
   - Filters tiles based on configuration
   - Uses Perspective API for 3D tile rendering
   - Implements distance culling for performance

4. **TileCollectorPanel.java** - Side panel UI
   - Displays total visited tiles count
   - Shows walkable tiles in current scene
   - Calculates and displays exploration percentage
   - Progress bar visualization
   - Reset functionality with confirmation dialog

## How It Works

### Tile Tracking Logic

1. **Scene Scanning**
   - On game state change to LOGGED_IN, scans current scene
   - Iterates through all tiles in the scene (104x104 grid)
   - Uses collision data to determine walkability
   - Stores walkable tiles as WorldPoint objects

2. **Walkability Detection**
   - Checks if tile has a model or paint (visual representation)
   - Queries collision map for blocking flags
   - Flag 0x100 indicates a blocked tile
   - Only tiles without blocking flags are considered walkable

3. **Movement Tracking**
   - Every game tick, checks player's current position
   - Marks current tile as visited
   - If player moved, uses line algorithm to mark intermediate tiles
   - Prevents gaps when player runs or moves quickly
   - Teleports (>10 tile distance) don't mark intermediate tiles

4. **Data Persistence**
   - On shutdown, serializes visited tiles to config string
   - Format: "x1,y1,plane1;x2,y2,plane2;..."
   - On startup, deserializes and loads visited tiles
   - Data persists across game sessions

### Rendering System

1. **Overlay Rendering**
   - Iterates through walkable tiles in current scene
   - Skips tiles that are already visited
   - Applies plane and distance filters
   - Converts WorldPoint to LocalPoint to screen polygon
   - Draws filled polygon and/or border based on config

2. **Performance Optimizations**
   - Distance culling (configurable max render distance)
   - Plane filtering (only show current plane)
   - Scene-based walkable tile caching
   - Periodic rescanning (every 100 game cycles)

### Statistics Calculation

- **Total Visited**: Size of visitedTiles HashSet
- **Scene Walkable**: Size of walkableTiles HashSet (current scene only)
- **Scene Exploration**: (visited tiles in scene / total walkable in scene) * 100

## Data Structures

### WorldPoint
- Immutable coordinate representation
- Contains x, y, and plane (floor level)
- Used for both visited and walkable tile storage
- Hashable for efficient Set operations

### HashSet Collections
- `visitedTiles`: All tiles ever walked on (persisted)
- `walkableTiles`: Walkable tiles in current scene (temporary)
- O(1) lookup for visited tile checks
- Efficient for large datasets

## API Usage

### RuneLite APIs Used
- **Client**: Game state, player position, scene access
- **Scene**: Tile data and collision information
- **CollisionData**: Walkability flags
- **Perspective**: 3D to 2D rendering conversion
- **ConfigManager**: Data persistence
- **OverlayManager**: Rendering system
- **ClientToolbar**: Side panel integration

### Event Subscriptions
- **GameStateChanged**: Detect login/logout, region changes
- **GameTick**: Track player movement every game cycle

## Configuration Storage

Visited tiles are stored as a semicolon-separated string in RuneLite's config:
```
visitedTilesData=3200,3200,0;3201,3200,0;3200,3201,0;...
```

This approach:
- ✅ Simple serialization/deserialization
- ✅ Compatible with RuneLite config system
- ✅ Human-readable for debugging
- ⚠️ May become large with extensive exploration
- ⚠️ No compression (future enhancement opportunity)

## Potential Improvements

1. **Data Compression**
   - Use run-length encoding for contiguous tiles
   - Binary format instead of text
   - Region-based chunking

2. **Enhanced Visualization**
   - Heatmap for frequently visited areas
   - Different colors for visit frequency
   - World map integration

3. **Statistics Enhancements**
   - Per-region exploration tracking
   - Global exploration percentage
   - Achievement milestones

4. **Performance**
   - Spatial indexing for large datasets
   - Incremental scene scanning
   - Background thread processing

5. **Features**
   - Export/import functionality
   - Sharing exploration data
   - Leaderboards
   - Path recording and playback

## Testing Recommendations

1. Test in various regions (cities, wilderness, dungeons)
2. Verify multi-floor buildings work correctly
3. Check performance with large visited tile counts
4. Test data persistence across sessions
5. Verify reset functionality
6. Test edge cases (teleports, death, logout during movement)

## Submission Checklist

- [ ] Create proper panel icon (16x16 or 32x32 PNG)
- [ ] Test thoroughly in-game
- [ ] Verify no console errors
- [ ] Check performance impact
- [ ] Ensure config saves/loads correctly
- [ ] Test reset functionality
- [ ] Verify overlay renders correctly
- [ ] Test on different screen resolutions
- [ ] Follow RuneLite code style guidelines
- [ ] Add proper license headers if required
- [ ] Create submission PR to RuneLite plugin hub
