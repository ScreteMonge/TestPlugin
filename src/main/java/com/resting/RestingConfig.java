package com.resting;

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
		SLEEP,
		RELAX,
		CROSS_ARMS,
		FOLD_HANDS,
		CUSTOM

	}
	@ConfigItem(
			keyName = "restMode",
			name = "Rest Mode",
			description = "Configures which Rest animation to play for yourself",
			position = 1
	)
	default RestMode restMode() { return RestMode.REST; }

	@ConfigItem(
			keyName = "allowPublicResting",
			name = "Allow Public Resting",
			description = "Whenever you or another player uses the Spin emote, it will instead make them Rest",
			position = 2
	)
	default boolean allowPublicRest()
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
			name = "Auto-Rest Timer",
			description = "Idle time (in seconds) before Auto-Resting if enabled",
			position = 4
	)
	default int autoRestTimer()	{ return 30; }

	@ConfigItem(
			keyName = "customAnimation",
			name = "Custom Animation",
			description = "The AnimationId that the 'Custom' Rest Mode plays",
			position = 5
	)
	default int customAnimation() { return 768; }
}
