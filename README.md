# Tile Collector

A RuneLite plugin that tracks and visualizes every tile you've walked on in Old School RuneScape. Gamifies exploration with an XP-based leveling system inspired by OSRS skills.

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/car_role)

## WARNING

This is a resource-intensive plugin. If you have an old computer, try using "potato mode" or decreasing the render scale in plugin settings.

## Features

- **Visual Tile Highlighting**: Highlights all walkable tiles you haven't visited yet with customizable colors
- **Real-time Tracking**: Automatically marks tiles as visited as you walk through the game
- **XP-Based Leveling System**: Earn XP for each new tile visited, with levels 1-99 following the OSRS XP curve
- **Statistics Panel**: View detailed stats including:
  - Total tiles visited across all of OSRS
  - Current exploration level and XP
  - Progress bar to next level
  - Walkable tiles in current scene
- **Flood-Fill Reachability**: Only shows tiles you can actually path to (no inaccessible areas)
- **Anti-Cheat Protection**: Validates tile visits and uses SHA-256 integrity hashing to prevent data tampering
- **Potato Mode**: Disable tile rendering to save system resources while still tracking progress
- **Persistent Data**: Your visited tiles are saved and loaded between sessions
- **Customizable Display**:
  - Adjustable highlight and border colors
  - Toggle fill and border options
  - Configurable render distance
  - Option to show only current plane

## How It Works

The plugin uses a flood-fill algorithm from the player's position to determine which tiles are actually reachable. As you move around, it tracks your position and marks tiles as visited. Tiles that haven't been visited yet are highlighted in the game world.

The plugin intelligently handles:
- **Gates and doors**: Rescans when you pass through interactive obstacles
- **Scene changes**: Accumulates reachable tiles as you explore
- **Multiple planes**: Tracks tiles on all floors/levels

## Configuration Options

### Rendering
| Option | Description | Default |
|--------|-------------|---------|
| Highlight Color | Color of unvisited tiles | Yellow (22% opacity) |
| Border Color | Border color for highlighted tiles | Yellow (78% opacity) |
| Fill Tiles | Fill tiles with color | Enabled |
| Show Border | Show border around tiles | Disabled |
| Border Width | Border thickness (1-5) | 2 |
| Only Current Plane | Show only tiles on your current plane | Enabled |

### Performance
| Option | Description | Default |
|--------|-------------|---------|
| Potato Mode | Disable rendering to save resources | Disabled |
| Max Render Distance | Maximum distance to render highlights (0 = unlimited) | 20 |

## Usage

1. Enable the plugin in RuneLite
2. Open the Tile Collector panel from the sidebar (tile icon)
3. Walk around the game - tiles will automatically be marked as visited
4. View your level and progress in the side panel

## Technical Details

- **Flood-fill reachability**: Uses BFS algorithm to find all tiles reachable from player position
- **Collision detection**: Respects OSRS collision flags (walls, objects, terrain)
- **Smart rescanning**: Only rescans when player moves significantly or passes through obstacles
- **Accumulated discovery**: Tiles from multiple scan positions are combined for full coverage
- **Anti-cheat validation**: 
  - Runtime: Validates tile visits based on player movement speed and position
  - Storage: SHA-256 hash verification prevents config file tampering
- **Estimated ~1.2 million walkable tiles** in OSRS (surface, dungeons, instances)


## License

BSD 2-Clause License - see RuneLite's licensing requirements.
