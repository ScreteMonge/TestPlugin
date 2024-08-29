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
		FORESTRY_REST,
		SIT,
		LOUNGE,
		DEEP_LOUNGE,
		SLEEP,
		RELAX,
		DEEP_SLEEP,
		DEAD,
		CROSS_ARMS,
		FOLD_HANDS,
		ARMS_ON_HIPS,
		WEAPON_ON_SHOULDER,
		GUARD_STANCE,
		LEAN_BACK,
		LEAN_SIDE,
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
			keyName = "spinRest",
			name = "Enable Spin Resting",
			description = "Whenever you use the Spin emote, it will instead make you Rest",
			position = 2
	)
	default boolean allowSpinRest()
	{
		return false;
	}

	@ConfigItem(
			keyName = "allowPublicResting",
			name = "Enable Public Resting",
			description = "Whenever another player uses the Spin emote, it will instead make them Rest",
			position = 3
	)
	default boolean allowPublicRest()
	{
		return true;
	}

	@ConfigItem(
			keyName = "autoRest",
			name = "Auto-Resting",
			description = "Makes you automatically Rest after a certain period of idle time",
			position = 4
	)
	default boolean autoRest() { return false; }

	@ConfigItem(
			keyName = "autoRestTimer",
			name = "Auto-Rest Timer",
			description = "Idle time (in seconds) before Auto-Resting if enabled",
			position = 5
	)
	default int autoRestTimer()	{ return 30; }

	@ConfigItem(
			keyName = "customAnimation",
			name = "Custom Animation",
			description = "The AnimationId that the 'Custom' Rest Mode plays",
			position = 6
	)
	default int customAnimation() { return 768; }
}
