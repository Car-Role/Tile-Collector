package com.tilecollector;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class TileCollectorPanel extends PluginPanel
{
	private final TileCollectorPlugin plugin;

	private final JLabel totalVisitedLabel = new JLabel();
	private final JLabel totalWalkableLabel = new JLabel();
	private final JLabel currentLevelLabel = new JLabel();
	private final JLabel nextLevelLabel = new JLabel();
	private final JProgressBar levelProgressBar = new JProgressBar();
	
	// Estimated total walkable tiles in OSRS (all areas, floors, instances)
	// Based on community data, map analysis, and Reddit research:
	// - Total map area: ~4.9 million tiles (46x26 chunks)
	// - But most is water, mountains, walls, etc.
	// Realistic walkable estimate:
	// - Surface overworld (walkable land): ~800,000 - 1,000,000 tiles
	// - Underground dungeons/caves: ~200,000 - 300,000 tiles
	// - Multi-floor buildings: ~50,000 - 100,000 tiles
	// - Instances (raids, minigames): ~50,000 - 100,000 tiles
	// Total estimate: ~1,200,000 walkable tiles
	private static final int ESTIMATED_TOTAL_TILES = 1200000;
	
	// XP calculation: 13,034,431 XP (level 99) ÷ 1,200,000 tiles = ~10.86 XP per tile
	private static final double XP_PER_TILE = 13034431.0 / ESTIMATED_TOTAL_TILES;
	
	// OSRS XP table for levels 1-99
	private static final int[] XP_TABLE = {
		0, 83, 174, 276, 388, 512, 650, 801, 969, 1154, 1358, 1584, 1833, 2107, 2411,
		2746, 3115, 3523, 3973, 4470, 5018, 5624, 6291, 7028, 7842, 8740, 9730, 10824,
		12031, 13363, 14833, 16456, 18247, 20224, 22406, 24815, 27473, 30408, 33648,
		37224, 41171, 45529, 50339, 55649, 61512, 67983, 75127, 83014, 91721, 101333,
		111945, 123660, 136594, 150872, 166636, 184040, 203254, 224466, 247886, 273742,
		302288, 333804, 368599, 407015, 449428, 496254, 547953, 605032, 668051, 737627,
		814445, 899257, 992895, 1096278, 1210421, 1336443, 1475581, 1629200, 1798808,
		1986068, 2192818, 2421087, 2673114, 2951373, 3258594, 3597792, 3972294, 4385776,
		4842295, 5346332, 5902831, 6517253, 7195629, 7944614, 8771558, 9684577, 10692629,
		11805606, 13034431, 14391160
	};

	public TileCollectorPanel(TileCollectorPlugin plugin, TileCollectorConfig config)
	{
		super(false);
		this.plugin = plugin;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		// Title
		JPanel titlePanel = new JPanel();
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		titlePanel.setLayout(new BorderLayout());

		JLabel title = new JLabel("Tile Collector");
		title.setForeground(Color.WHITE);
		title.setFont(new Font("Arial", Font.BOLD, 16));
		titlePanel.add(title, BorderLayout.CENTER);

		add(titlePanel, BorderLayout.NORTH);

		// Stats panel
		JPanel statsPanel = new JPanel();
		statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		statsPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.insets = new Insets(5, 5, 5, 5);

		// Current Level
		JLabel levelTitleLabel = new JLabel("Exploration Level:");
		levelTitleLabel.setForeground(Color.WHITE);
		statsPanel.add(levelTitleLabel, gbc);

		gbc.gridy++;
		currentLevelLabel.setForeground(new Color(255, 215, 0)); // Gold color
		currentLevelLabel.setFont(new Font("Arial", Font.BOLD, 24));
		statsPanel.add(currentLevelLabel, gbc);

		gbc.gridy++;
		JSeparator separator1 = new JSeparator();
		statsPanel.add(separator1, gbc);

		// Tiles walked
		gbc.gridy++;
		JLabel visitedTitleLabel = new JLabel("Tiles Walked:");
		visitedTitleLabel.setForeground(Color.WHITE);
		statsPanel.add(visitedTitleLabel, gbc);

		gbc.gridy++;
		totalVisitedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		totalVisitedLabel.setFont(new Font("Arial", Font.BOLD, 14));
		statsPanel.add(totalVisitedLabel, gbc);

		gbc.gridy++;
		JSeparator separator2 = new JSeparator();
		statsPanel.add(separator2, gbc);

		// Total walkable in world
		gbc.gridy++;
		JLabel walkableTitleLabel = new JLabel("Total Walkable in World:");
		walkableTitleLabel.setForeground(Color.WHITE);
		statsPanel.add(walkableTitleLabel, gbc);

		gbc.gridy++;
		totalWalkableLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		totalWalkableLabel.setFont(new Font("Arial", Font.BOLD, 14));
		statsPanel.add(totalWalkableLabel, gbc);

		gbc.gridy++;
		JSeparator separator3 = new JSeparator();
		statsPanel.add(separator3, gbc);

		// Progress to next level
		gbc.gridy++;
		nextLevelLabel.setForeground(Color.WHITE);
		nextLevelLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		statsPanel.add(nextLevelLabel, gbc);

		gbc.gridy++;
		levelProgressBar.setStringPainted(true);
		levelProgressBar.setForeground(new Color(50, 205, 50));
		levelProgressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statsPanel.add(levelProgressBar, gbc);

		add(statsPanel, BorderLayout.CENTER);

		// Initial update
		update();
	}

	public void update()
	{
		SwingUtilities.invokeLater(() -> {
			int tilesWalked = plugin.getVisitedTiles().size();
			
			// Convert tiles to XP: each tile = ~29 XP
			// Formula: tiles × XP_PER_TILE = tiles × (13,034,431 / 450,000)
			int currentXP = (int) (tilesWalked * XP_PER_TILE);
			
			// Calculate current level
			int currentLevel = getLevelForXP(currentXP);
			
			// Calculate progress to next level
			int currentLevelXP = XP_TABLE[currentLevel - 1];
			int nextLevelXP = currentLevel < 99 ? XP_TABLE[currentLevel] : XP_TABLE[98];
			int xpIntoLevel = currentXP - currentLevelXP;
			int xpNeeded = nextLevelXP - currentLevelXP;
			double progressPercent = currentLevel < 99 ? (double) xpIntoLevel / xpNeeded * 100 : 100;
			
			// Update labels
			currentLevelLabel.setText(String.format("Level %d", currentLevel));
			totalVisitedLabel.setText(String.format("%,d / %,d", tilesWalked, ESTIMATED_TOTAL_TILES));
			totalWalkableLabel.setText(String.format("%,d tiles (estimated)", ESTIMATED_TOTAL_TILES));
			
			if (currentLevel < 99)
			{
				nextLevelLabel.setText(String.format("Progress to Level %d:", currentLevel + 1));
				levelProgressBar.setValue((int) progressPercent);
				// Calculate tiles needed for next level
				int tilesNeeded = (int) Math.ceil((nextLevelXP - currentXP) / XP_PER_TILE);
				levelProgressBar.setString(String.format("%.1f%% (%,d tiles)", progressPercent, tilesNeeded));
			}
			else
			{
				nextLevelLabel.setText("Max Level Achieved!");
				levelProgressBar.setValue(100);
				levelProgressBar.setString("99/99");
			}
		});
	}
	
	private int getLevelForXP(int xp)
	{
		for (int level = 98; level >= 0; level--)
		{
			if (xp >= XP_TABLE[level])
			{
				return level + 1;
			}
		}
		return 1;
	}
}
