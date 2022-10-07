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
			name = "Rest Mode",
			description = "Configures which Rest animation to play for yourself",
			position = 1
	)
	default RestMode restMode() { return RestMode.REST; }

	@ConfigItem(
			keyName = "allowOthersRest",
			name = "Allow Others Rest",
			description = "Determines whether you can see other players Rest if they have the plugin and use the Spin emote",
			position = 2
	)
	default boolean allowOthersRest()
	{
		return true;
	}

	@ConfigItem(
			keyName = "autoRest",
			name = "Auto-Resting",
			description = "Makes you automatically Rest after a certain period of idle time",
			position = 3
	)
	default boolean autoRest() { return false; }

	@ConfigItem(
			keyName = "autoRestTimer",
			name = "Auto-Rest Timer (Sec)",
			description = "Idle time (in seconds) before Auto-Resting if enabled",
			position = 4
	)
	default int autoRestTimer()	{ return 30; }
}
