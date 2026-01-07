package com.tilecollector;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class TileCollectorOverlay extends Overlay
{
	private final Client client;
	private final TileCollectorPlugin plugin;
	private final TileCollectorConfig config;

	@Inject
	private TileCollectorOverlay(Client client, TileCollectorPlugin plugin, TileCollectorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Potato Mode: Skip all rendering to save resources
		if (config.potatoMode())
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}

		int playerPlane = playerLocation.getPlane();
		int maxDistance = config.maxRenderDistance();

		// Render all reachable tiles that haven't been visited
		for (WorldPoint tile : plugin.getReachableTiles())
		{
			// Skip if already visited
			if (plugin.getVisitedTiles().contains(tile))
			{
				continue;
			}

			// Skip if not on current plane and config says to only show current plane
			if (config.onlyCurrentPlane() && tile.getPlane() != playerPlane)
			{
				continue;
			}

			// Skip if too far away
			if (maxDistance > 0 && tile.distanceTo(playerLocation) > maxDistance)
			{
				continue;
			}

			// Render the tile
			renderTile(graphics, tile);
		}

		return null;
	}

	private void renderTile(Graphics2D graphics, WorldPoint worldPoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
		if (localPoint == null)
		{
			return;
		}

		Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
		if (polygon == null)
		{
			return;
		}

		// Fill the tile
		if (config.fillTiles())
		{
			graphics.setColor(config.highlightColor());
			graphics.fillPolygon(polygon);
		}

		// Draw border
		if (config.showBorder())
		{
			graphics.setColor(config.borderColor());
			graphics.setStroke(new BasicStroke(config.borderWidth()));
			graphics.drawPolygon(polygon);
		}
	}
}
