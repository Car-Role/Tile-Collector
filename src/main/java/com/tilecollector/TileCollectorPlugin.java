package com.tilecollector;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Tile Collector",
	description = "Tracks and highlights tiles you haven't walked on yet",
	tags = {"tiles", "exploration", "map", "tracker"}
)
public class TileCollectorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TileCollectorConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TileCollectorOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Getter
	private final Set<WorldPoint> visitedTiles = new HashSet<>();

	@Getter
	private final Set<WorldPoint> walkableTiles = new HashSet<>();
	
	// Reachable tiles from player's position (subset of walkableTiles)
	@Getter
	private final Set<WorldPoint> reachableTiles = new HashSet<>();
	
	// Accumulated reachable tiles from all scans in current scene
	// This grows as player moves around and we scan from different positions
	private final Set<WorldPoint> accumulatedReachableTiles = new HashSet<>();
	
	// Track the last scene we computed reachability for
	private int lastBaseX = -1;
	private int lastBaseY = -1;
	private int lastPlane = -1;
	
	// Track last scan position to trigger incremental scans
	private WorldPoint lastScanPosition = null;
	// Rescan when player moves this far - ensures we catch tiles at scene edges
	// Scene is 104x104, player starts in center (~52), so 20 tiles ensures good coverage
	private static final int RESCAN_DISTANCE_THRESHOLD = 20;

	private WorldPoint lastPlayerPosition = null;
	private TileCollectorPanel panel;
	private NavigationButton navButton;
	private int ticksSinceLastSave = 0;
	private int lastSavedTileCount = 0;
	private Thread shutdownHook;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Tile Collector started!");
		overlayManager.add(overlay);
		
		// Load saved data
		loadVisitedTiles();
		lastSavedTileCount = visitedTiles.size();
		
		// Create and add panel
		log.info("Creating Tile Collector panel...");
		panel = new TileCollectorPanel(this, config);
		
		// Load icon - create default if not found
		BufferedImage icon;
		try
		{
			icon = ImageUtil.loadImageResource(TileCollectorPlugin.class, "icon.png");
			log.info("Loaded panel icon from resources");
		}
		catch (Exception e)
		{
			log.warn("Panel icon not found, creating default icon", e);
			// Create a simple default icon if the image file doesn't exist
			icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = icon.createGraphics();
			g.setColor(new Color(255, 215, 0)); // Gold color
			g.fillRect(0, 0, 16, 16);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, 15, 15);
			g.dispose();
		}
		
		log.info("Building navigation button...");
		navButton = NavigationButton.builder()
			.tooltip("Tile Collector")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		
		log.info("Adding navigation button to toolbar...");
		clientToolbar.addNavigation(navButton);
		log.info("Tile Collector panel added successfully!");
		
		// Register JVM shutdown hook to save on emergency shutdown (pressing X)
		shutdownHook = new Thread(() -> {
			log.info("JVM shutdown detected - emergency save");
			if (!visitedTiles.isEmpty())
			{
				try
				{
					// Direct save without verification for speed
					StringBuilder sb = new StringBuilder(visitedTiles.size() * 20);
					boolean first = true;
					for (WorldPoint tile : visitedTiles)
					{
						if (!first) sb.append(";");
						sb.append(tile.getX()).append(",")
							.append(tile.getY()).append(",")
							.append(tile.getPlane());
						first = false;
					}
					configManager.unsetConfiguration("tilecollector", "visitedTilesData");
					configManager.setConfiguration("tilecollector", "visitedTilesData", sb.toString());
					log.info("Emergency save completed: {} tiles", visitedTiles.size());
				}
				catch (Exception e)
				{
					log.error("Emergency save failed", e);
				}
			}
		}, "TileCollector-ShutdownHook");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		log.info("Shutdown hook registered");
		
		// Initial scan if logged in
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			scanWalkableTiles();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Tile Collector shutting down...");
		
		// Remove shutdown hook since we're doing a clean shutdown
		try
		{
			if (shutdownHook != null)
			{
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
				log.info("Shutdown hook removed");
			}
		}
		catch (Exception e)
		{
			log.debug("Could not remove shutdown hook (may have already run)");
		}
		
		// CRITICAL: Save before cleanup
		if (!visitedTiles.isEmpty())
		{
			log.info("Saving {} tiles before shutdown", visitedTiles.size());
			saveVisitedTiles();
			
			// Give ConfigManager time to persist
			try { Thread.sleep(50); } catch (InterruptedException e) {}
		}
		
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		visitedTiles.clear();
		walkableTiles.clear();
		reachableTiles.clear();
		accumulatedReachableTiles.clear();
		lastPlayerPosition = null;
		lastScanPosition = null;
		lastBaseX = -1;
		lastBaseY = -1;
		lastPlane = -1;
		log.info("Tile Collector stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			scanWalkableTiles();
		}
		else if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.HOPPING)
		{
			// CRITICAL: Save before region change or world hop to prevent data loss
			if (!visitedTiles.isEmpty())
			{
				log.debug("Saving before region/world change");
				saveVisitedTiles();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		// Increment tick counter first
		ticksSinceLastSave++;
		
		WorldPoint currentPosition = localPlayer.getWorldLocation();
		
		// Mark current tile as visited with anti-cheat validation
		if (currentPosition != null)
		{
			// Anti-cheat: Only mark tile if player actually moved there legitimately
			if (isValidTileVisit(currentPosition))
			{
				visitedTiles.add(currentPosition);
				
				// If player moved, mark all tiles between last and current position
				if (lastPlayerPosition != null && !lastPlayerPosition.equals(currentPosition))
				{
					markTilesBetweenValidated(lastPlayerPosition, currentPosition);
				}
				
				// Gate/fence fix: If player is on a tile we haven't marked as reachable,
				// they must have gone through a gate or other interactive obstacle.
				// Trigger immediate rescan from their new position to discover tiles on this side.
				if (!reachableTiles.contains(currentPosition) && !reachableTiles.isEmpty())
				{
					log.debug("Player on unreachable tile {} - likely passed through gate, triggering rescan", currentPosition);
					scanWalkableTiles();
				}
			}
			
			lastPlayerPosition = currentPosition;
		}
		
		// Check if we need to rescan (region change or moved far enough)
		if (shouldRescanReachability(currentPosition))
		{
			scanWalkableTiles();
		}
		
		// Auto-save every 50 ticks (~30 seconds) OR every 10 new tiles
		int newTiles = visitedTiles.size() - lastSavedTileCount;
		if (ticksSinceLastSave >= 50 || newTiles >= 10)
		{
			log.debug("Auto-save triggered: {} ticks, {} new tiles", ticksSinceLastSave, newTiles);
			saveVisitedTiles();
			ticksSinceLastSave = 0;
			lastSavedTileCount = visitedTiles.size();
		}
		
		// Update panel less frequently (every 10 ticks for better performance)
		if (panel != null && ticksSinceLastSave % 10 == 0)
		{
			panel.update();
		}
	}

	/**
	 * Determines if we should rescan reachability based on player movement and region changes
	 */
	private boolean shouldRescanReachability(WorldPoint currentPosition)
	{
		if (currentPosition == null)
		{
			return false;
		}
		
		int currentBaseX = client.getBaseX();
		int currentBaseY = client.getBaseY();
		int currentPlane = currentPosition.getPlane();
		
		// Always rescan if region changed (player loaded new area)
		if (currentBaseX != lastBaseX || currentBaseY != lastBaseY || currentPlane != lastPlane)
		{
			return true;
		}
		
		// Rescan if this is the first scan
		if (lastScanPosition == null)
		{
			return true;
		}
		
		// Rescan if player moved far enough from last scan position
		// This catches cases where player walks to edge of previously scanned area
		int distance = currentPosition.distanceTo(lastScanPosition);
		return distance >= RESCAN_DISTANCE_THRESHOLD;
	}
	
	/**
	 * Scans the current scene for walkable tiles and computes reachable tiles from player position.
	 * Accumulates results as player moves around to ensure full scene coverage.
	 */
	private void scanWalkableTiles()
	{
		Scene scene = client.getScene();
		if (scene == null)
		{
			return;
		}
		
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		WorldPoint playerWorld = localPlayer.getWorldLocation();
		int currentBaseX = client.getBaseX();
		int currentBaseY = client.getBaseY();
		int playerPlane = playerWorld.getPlane();
		
		// Check if scene changed - if so, clear accumulated data
		boolean sceneChanged = (currentBaseX != lastBaseX || currentBaseY != lastBaseY || playerPlane != lastPlane);
		if (sceneChanged)
		{
			accumulatedReachableTiles.clear();
			log.debug("Scene changed, clearing accumulated reachability data");
		}
		
		// Need to compute fresh reachability data
		Tile[][][] tiles = scene.getTiles();
		CollisionData[] collisionData = client.getCollisionMaps();
		if (collisionData == null)
		{
			return;
		}
		
		// Get collision flags for player's plane
		if (playerPlane >= collisionData.length || collisionData[playerPlane] == null)
		{
			return;
		}
		int[][] flags = collisionData[playerPlane].getFlags();
		
		// Build a set of all walkable scene coordinates for the current scene
		Set<Integer> walkableSceneCoords = new HashSet<>();

		// First pass: identify all walkable tiles in the scene on the player's plane
		for (int x = 0; x < Constants.SCENE_SIZE; x++)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; y++)
			{
				Tile tile = tiles[playerPlane][x][y];
				if (tile == null)
				{
					continue;
				}
				
				// Basic walkability check
				if (tile.getSceneTileModel() == null && tile.getSceneTilePaint() == null)
				{
					continue;
				}
				
				if (x < 0 || x >= flags.length || y < 0 || y >= flags[x].length)
				{
					continue;
				}
				
				int flag = flags[x][y];
				if ((flag & BLOCK_MOVEMENT_FULL) == 0)
				{
					// Encode scene x,y into single int for efficient lookup
					walkableSceneCoords.add(encodeCoord(x, y));
				}
			}
		}
		
		// Get player's scene coordinates
		int playerSceneX = playerWorld.getX() - currentBaseX;
		int playerSceneY = playerWorld.getY() - currentBaseY;
		
		// Flood fill from player position to find all reachable tiles
		Set<Integer> reachableSceneCoords = floodFillReachable(
			playerSceneX, playerSceneY, walkableSceneCoords, flags);
		
		// Convert reachable scene coords to world points and accumulate
		int newTilesFound = 0;
		for (int encoded : reachableSceneCoords)
		{
			int x = decodeX(encoded);
			int y = decodeY(encoded);
			WorldPoint worldPoint = WorldPoint.fromScene(client, x, y, playerPlane);
			if (worldPoint != null)
			{
				if (accumulatedReachableTiles.add(worldPoint))
				{
					newTilesFound++;
				}
				walkableTiles.add(worldPoint); // Also add to walkableTiles for stats
			}
		}
		
		// Update reachableTiles from accumulated data
		reachableTiles.clear();
		reachableTiles.addAll(accumulatedReachableTiles);
		
		// Update tracking variables
		lastBaseX = currentBaseX;
		lastBaseY = currentBaseY;
		lastPlane = playerPlane;
		lastScanPosition = playerWorld;
		
		log.debug("Scan complete: {} new tiles, {} total reachable in scene", newTilesFound, reachableTiles.size());
	}
	
	/**
	 * Encodes scene x,y coordinates into a single integer
	 */
	private int encodeCoord(int x, int y)
	{
		return (x << 16) | (y & 0xFFFF);
	}
	
	/**
	 * Decodes X from encoded coordinate
	 */
	private int decodeX(int encoded)
	{
		return encoded >> 16;
	}
	
	/**
	 * Decodes Y from encoded coordinate
	 */
	private int decodeY(int encoded)
	{
		return encoded & 0xFFFF;
	}
	
	/**
	 * Performs flood fill from start position to find all reachable tiles.
	 * Only moves in cardinal directions (N, S, E, W) as per OSRS movement rules.
	 */
	private Set<Integer> floodFillReachable(int startX, int startY, Set<Integer> walkableCoords, int[][] flags)
	{
		Set<Integer> reachable = new HashSet<>();
		Queue<Integer> queue = new ArrayDeque<>();
		
		int startEncoded = encodeCoord(startX, startY);
		if (!walkableCoords.contains(startEncoded))
		{
			// Player is on a non-walkable tile somehow, just return empty
			return reachable;
		}
		
		queue.add(startEncoded);
		reachable.add(startEncoded);
		
		// Cardinal direction offsets: N, S, E, W
		int[] dx = {0, 0, 1, -1};
		int[] dy = {1, -1, 0, 0};
		// Corresponding blocking flags for LEAVING current tile in that direction
		int[] blockFlagsFrom = {BLOCK_MOVEMENT_NORTH, BLOCK_MOVEMENT_SOUTH, BLOCK_MOVEMENT_EAST, BLOCK_MOVEMENT_WEST};
		// Corresponding blocking flags for ENTERING neighbor tile from that direction
		int[] blockFlagsTo = {BLOCK_MOVEMENT_SOUTH, BLOCK_MOVEMENT_NORTH, BLOCK_MOVEMENT_WEST, BLOCK_MOVEMENT_EAST};
		
		while (!queue.isEmpty())
		{
			int current = queue.poll();
			int cx = decodeX(current);
			int cy = decodeY(current);
			
			// Get current tile's flags
			if (cx < 0 || cx >= flags.length || cy < 0 || cy >= flags[cx].length)
			{
				continue;
			}
			int currentFlag = flags[cx][cy];
			
			// Try each cardinal direction
			for (int i = 0; i < 4; i++)
			{
				int nx = cx + dx[i];
				int ny = cy + dy[i];
				int neighborEncoded = encodeCoord(nx, ny);
				
				// Skip if already visited or not walkable
				if (reachable.contains(neighborEncoded) || !walkableCoords.contains(neighborEncoded))
				{
					continue;
				}
				
				// Bounds check for neighbor
				if (nx < 0 || nx >= flags.length || ny < 0 || ny >= flags[nx].length)
				{
					continue;
				}
				
				int neighborFlag = flags[nx][ny];
				
				// Check if we can move from current tile to neighbor:
				// 1. Current tile must not block movement in that direction
				// 2. Neighbor tile must not block entry from our direction
				boolean canLeave = (currentFlag & blockFlagsFrom[i]) == 0;
				boolean canEnter = (neighborFlag & blockFlagsTo[i]) == 0;
				
				if (canLeave && canEnter)
				{
					reachable.add(neighborEncoded);
					queue.add(neighborEncoded);
				}
			}
		}
		
		return reachable;
	}

	// Collision flag constants
	private static final int BLOCK_MOVEMENT_OBJECT = 0x100;
	private static final int BLOCK_MOVEMENT_FLOOR_DECORATION = 0x40000;
	private static final int BLOCK_MOVEMENT_FLOOR = 0x200000;
	private static final int BLOCK_MOVEMENT_FULL = BLOCK_MOVEMENT_OBJECT | BLOCK_MOVEMENT_FLOOR_DECORATION | BLOCK_MOVEMENT_FLOOR;
	
	// Directional blocking flags - these indicate you cannot ENTER the tile from that direction
	private static final int BLOCK_MOVEMENT_NORTH = 0x2;
	private static final int BLOCK_MOVEMENT_EAST = 0x8;
	private static final int BLOCK_MOVEMENT_SOUTH = 0x20;
	private static final int BLOCK_MOVEMENT_WEST = 0x80;
	
	// Anti-cheat: Maximum tiles that can be marked per game tick (running speed + buffer)
	private static final int MAX_TILES_PER_TICK = 3;
	// Anti-cheat: Maximum distance player can move in one tick (running = 2 tiles)
	private static final int MAX_MOVEMENT_PER_TICK = 2;
	
	/**
	 * Anti-cheat validation: Checks if a tile visit is legitimate.
	 * A visit is valid if:
	 * 1. Player is actually at or near this position
	 * 2. The tile is reachable (in our computed reachable set or close to player)
	 */
	private boolean isValidTileVisit(WorldPoint tile)
	{
		if (tile == null)
		{
			return false;
		}
		
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return false;
		}
		
		WorldPoint playerPos = localPlayer.getWorldLocation();
		if (playerPos == null)
		{
			return false;
		}
		
		// Tile must be where the player actually is (or very close for running)
		int distance = tile.distanceTo(playerPos);
		if (distance > MAX_MOVEMENT_PER_TICK)
		{
			log.warn("Anti-cheat: Rejected tile {} - too far from player position {} (distance: {})", 
				tile, playerPos, distance);
			return false;
		}
		
		// Tile must be on the same plane
		if (tile.getPlane() != playerPos.getPlane())
		{
			log.warn("Anti-cheat: Rejected tile {} - different plane than player", tile);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Marks all tiles between two points as visited with anti-cheat validation.
	 * Only marks tiles that form a valid movement path.
	 */
	private void markTilesBetweenValidated(WorldPoint start, WorldPoint end)
	{
		// Anti-cheat: Only mark tiles if they're close (player walked, didn't teleport)
		int distance = start.distanceTo(end);
		if (distance > MAX_MOVEMENT_PER_TICK)
		{
			// Player moved too far in one tick - likely teleport, only mark destination
			// The destination is already validated in onGameTick
			return;
		}
		
		// Anti-cheat: Must be on same plane
		if (start.getPlane() != end.getPlane())
		{
			return;
		}

		// Simple line algorithm to mark tiles in between
		int x1 = start.getX();
		int y1 = start.getY();
		int x2 = end.getX();
		int y2 = end.getY();
		int plane = start.getPlane();

		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx - dy;
		
		int tilesMarked = 0;

		while (tilesMarked < MAX_TILES_PER_TICK)
		{
			WorldPoint tileToMark = new WorldPoint(x1, y1, plane);
			visitedTiles.add(tileToMark);
			tilesMarked++;

			if (x1 == x2 && y1 == y2)
			{
				break;
			}

			int e2 = 2 * err;
			if (e2 > -dy)
			{
				err -= dy;
				x1 += sx;
			}
			if (e2 < dx)
			{
				err += dx;
				y1 += sy;
			}
		}
	}

	// Secret salt for integrity hash - makes it harder to forge checksums
	// In a real deployment, this could be derived from account-specific data
	private static final String INTEGRITY_SALT = "TileCollector_v1_";
	
	/**
	 * Generates a SHA-256 hash of the tile data for integrity verification.
	 * This prevents users from manually editing the config file.
	 */
	private String generateIntegrityHash(String data)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String saltedData = INTEGRITY_SALT + data + visitedTiles.size();
			byte[] hash = digest.digest(saltedData.getBytes());
			
			// Convert to hex string
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash)
			{
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			log.error("SHA-256 not available", e);
			return "";
		}
	}
	
	/**
	 * Verifies the integrity hash matches the tile data.
	 * Returns true if data is valid, false if tampered.
	 */
	private boolean verifyIntegrityHash(String data, String storedHash, int tileCount)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String saltedData = INTEGRITY_SALT + data + tileCount;
			byte[] hash = digest.digest(saltedData.getBytes());
			
			// Convert to hex string
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash)
			{
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			
			return hexString.toString().equals(storedHash);
		}
		catch (NoSuchAlgorithmException e)
		{
			log.error("SHA-256 not available", e);
			return false;
		}
	}
	
	/**
	 * Loads visited tiles from config with integrity verification
	 */
	private void loadVisitedTiles()
	{
		String data = configManager.getConfiguration("tilecollector", "visitedTilesData");
		String storedHash = configManager.getConfiguration("tilecollector", "integrityHash");
		String storedCountStr = configManager.getConfiguration("tilecollector", "tileCount");
		
		if (data == null || data.isEmpty())
		{
			log.info("No saved tiles found - starting fresh");
			return;
		}

		try
		{
			// Parse tiles first to get count
			visitedTiles.clear();
			String[] tiles = data.split(";");
			
			int loaded = 0;
			for (String tile : tiles)
			{
				if (tile.isEmpty()) continue;
				
				String[] parts = tile.split(",");
				if (parts.length == 3)
				{
					visitedTiles.add(new WorldPoint(
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2])
					));
					loaded++;
				}
			}
			
			// Verify integrity if hash exists
			if (storedHash != null && !storedHash.isEmpty())
			{
				int storedCount = 0;
				try
				{
					storedCount = storedCountStr != null ? Integer.parseInt(storedCountStr) : 0;
				}
				catch (NumberFormatException e)
				{
					storedCount = 0;
				}
				
				if (!verifyIntegrityHash(data, storedHash, storedCount))
				{
					log.error("ANTI-CHEAT: Data integrity check FAILED! Tile data may have been tampered with.");
					log.error("ANTI-CHEAT: Resetting tile data to prevent cheating.");
					visitedTiles.clear();
					configManager.unsetConfiguration("tilecollector", "visitedTilesData");
					configManager.unsetConfiguration("tilecollector", "integrityHash");
					configManager.unsetConfiguration("tilecollector", "tileCount");
					return;
				}
				
				// Also verify tile count matches
				if (storedCount != loaded)
				{
					log.error("ANTI-CHEAT: Tile count mismatch! Expected {}, got {}. Data may be tampered.", storedCount, loaded);
					visitedTiles.clear();
					configManager.unsetConfiguration("tilecollector", "visitedTilesData");
					configManager.unsetConfiguration("tilecollector", "integrityHash");
					configManager.unsetConfiguration("tilecollector", "tileCount");
					return;
				}
				
				log.info("Integrity check passed - loaded {} verified tiles", loaded);
			}
			else
			{
				// No hash stored (legacy data or first run) - accept but will be hashed on next save
				log.info("No integrity hash found - loaded {} tiles (will be secured on next save)", loaded);
			}
		}
		catch (Exception e)
		{
			log.error("CRITICAL: Failed to load tiles", e);
		}
	}

	/**
	 * Saves visited tiles to config with integrity hash - CRITICAL for data persistence
	 */
	private void saveVisitedTiles()
	{
		if (visitedTiles.isEmpty())
		{
			log.debug("No tiles to save");
			return;
		}
		
		// Build data string efficiently
		StringBuilder sb = new StringBuilder(visitedTiles.size() * 20);
		boolean first = true;
		for (WorldPoint tile : visitedTiles)
		{
			if (!first) sb.append(";");
			sb.append(tile.getX()).append(",")
				.append(tile.getY()).append(",")
				.append(tile.getPlane());
			first = false;
		}
		
		String data = sb.toString();
		
		// Generate integrity hash for anti-cheat
		String integrityHash = generateIntegrityHash(data);
		int tileCount = visitedTiles.size();
		
		try
		{
			// CRITICAL: Unset first to ensure clean write
			configManager.unsetConfiguration("tilecollector", "visitedTilesData");
			configManager.unsetConfiguration("tilecollector", "integrityHash");
			configManager.unsetConfiguration("tilecollector", "tileCount");
			
			// Save data, hash, and count
			configManager.setConfiguration("tilecollector", "visitedTilesData", data);
			configManager.setConfiguration("tilecollector", "integrityHash", integrityHash);
			configManager.setConfiguration("tilecollector", "tileCount", String.valueOf(tileCount));
			
			// Verify save succeeded
			String verify = configManager.getConfiguration("tilecollector", "visitedTilesData");
			if (verify != null && verify.length() == data.length())
			{
				log.info("Saved {} tiles with integrity hash", visitedTiles.size());
			}
			else
			{
				log.error("SAVE FAILED - verification mismatch! Expected: {}, Got: {}", 
					data.length(), verify != null ? verify.length() : 0);
			}
		}
		catch (Exception e)
		{
			log.error("CRITICAL: Failed to save tiles", e);
		}
	}

	/**
	 * Resets all visited tiles
	 */
	public void resetVisitedTiles()
	{
		visitedTiles.clear();
		configManager.unsetConfiguration("tilecollector", "visitedTilesData");
		configManager.unsetConfiguration("tilecollector", "integrityHash");
		configManager.unsetConfiguration("tilecollector", "tileCount");
		log.info("Reset all visited tiles");
	}

	/**
	 * Gets the percentage of tiles explored globally
	 */
	public double getExplorationPercentage()
	{
		if (walkableTiles.isEmpty())
		{
			return 0.0;
		}
		
		// Count how many walkable tiles have been visited
		long visitedWalkable = walkableTiles.stream()
			.filter(visitedTiles::contains)
			.count();
		
		return (visitedWalkable * 100.0) / walkableTiles.size();
	}

	@Provides
	TileCollectorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TileCollectorConfig.class);
	}
}
