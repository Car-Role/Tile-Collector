package com.tilecollector;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TileCollectorPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TileCollectorPlugin.class);
		RuneLite.main(args);
	}
}
