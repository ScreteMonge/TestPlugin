package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface RestingConfig extends Config
{
	@ConfigItem(
			keyName = "sleepWhileResting",
			name = "Sleep while resting",
			description = "Applies a snore animation while Resting",
			position = 1
	)
	default boolean sleepWhileResting()
	{
		return false;
	}

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
