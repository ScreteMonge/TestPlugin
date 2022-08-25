package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("resting")
public interface RestingConfig extends Config
{
	enum RestMode
	{
		REST,
		SIT,
		LOUNGE,
		SLEEP

	}
	@ConfigItem(
			keyName = "restMode",
			name = "Rest mode",
			description = "Configures which Rest animation to play for yourself",
			position = 1
	)
	default RestMode restMode() { return RestMode.REST; }

	@ConfigItem(
			keyName = "allowOthersRest",
			name = "Allow others to rest",
			description = "Determines whether you can see other players Rest if they have the plugin and use the Spin emote",
			position = 2
	)
	default boolean allowOthersRest()
	{
		return true;
	}
}
