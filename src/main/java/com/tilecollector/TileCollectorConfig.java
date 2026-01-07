package com.tilecollector;

import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup("tilecollector")
public interface TileCollectorConfig extends Config
{
	@ConfigSection(
		name = "Rendering",
		description = "Tile rendering settings",
		position = 0
	)
	String renderingSection = "rendering";

	@ConfigSection(
		name = "Performance",
		description = "Performance optimization settings",
		position = 1
	)
	String performanceSection = "performance";

	@ConfigItem(
		keyName = "potatoMode",
		name = "Potato Mode",
		description = "Disables tile rendering to save system resources. Tiles are still tracked in the background.",
		position = 0,
		section = performanceSection
	)
	default boolean potatoMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight Color",
		description = "Color of tiles that haven't been walked on yet",
		position = 1,
		section = renderingSection
	)
	@Alpha
	default Color highlightColor()
	{
		return new Color(255, 255, 0, 55); // Yellow with ~22% opacity (increased from 35 by ~8%)
	}

	@ConfigItem(
		keyName = "borderColor",
		name = "Border Color",
		description = "Border color for highlighted tiles",
		position = 2,
		section = renderingSection
	)
	@Alpha
	default Color borderColor()
	{
		return new Color(255, 255, 0, 200);
	}

	@ConfigItem(
		keyName = "fillTiles",
		name = "Fill Tiles",
		description = "Fill the tiles with color instead of just showing borders",
		position = 3,
		section = renderingSection
	)
	default boolean fillTiles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBorder",
		name = "Show Border",
		description = "Show border around highlighted tiles",
		position = 4,
		section = renderingSection
	)
	default boolean showBorder()
	{
		return false; // Borders off by default for cleaner look
	}

	@ConfigItem(
		keyName = "borderWidth",
		name = "Border Width",
		description = "Width of the tile border",
		position = 5,
		section = renderingSection
	)
	@Range(min = 1, max = 5)
	default int borderWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "onlyCurrentPlane",
		name = "Only Current Plane",
		description = "Only show tiles on the current plane",
		position = 6,
		section = renderingSection
	)
	default boolean onlyCurrentPlane()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxRenderDistance",
		name = "Max Render Distance",
		description = "Maximum distance to render highlighted tiles (0 = unlimited)",
		position = 7,
		section = performanceSection
	)
	@Range(min = 0, max = 50)
	default int maxRenderDistance()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "visitedTilesData",
		name = "",
		description = "",
		hidden = true
	)
	default String visitedTilesData()
	{
		return "";
	}

	@ConfigItem(
		keyName = "visitedTilesData",
		name = "",
		description = "",
		hidden = true
	)
	void setVisitedTilesData(String data);
}
