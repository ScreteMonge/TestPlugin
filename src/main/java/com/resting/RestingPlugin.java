package com.resting;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;

@Slf4j
@PluginDescriptor(
		name = "Resting",
		description = "Allows the player a resting idle animation",
		tags = {"immersion", "rest", "emote"}
)
public class RestingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private RestingConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	private final Map<Player, WorldPoint> restMap = new HashMap<>();
	private final int MAGIC_LUNAR_DREAM_Z = 1056;
	private final int EMOTE_SPIN = 2107;
	private final int IDLE_POSE = 808;
	private final int CROSS_ARMS_POSE = 2256;
	private final int FOLD_HANDS_POSE = 2578;
	private final int SIT_POSE = 4851;
	private final int DEEP_LOUNGE_POSE = 5814;
	private final int DEAD_POSE = 6280;
	private final int LOUNGE_POSE = 6284;
	private final int REST_POSE = 6296;
	private final int ARMS_ON_HIPS_POSE = 6393;
	private final int LEAN_BACK_POSE = 6913;
	private final int LEAN_SIDE_POSE = 6914;
	private final int MAGIC_LUNAR_DREAM_SITTING_DOWN = 7627;
	private final int RELAX_POSE = 8409;
	private final int FORESTRY_REST_POSE = 10084;
	private final int WEAPON_ON_SHOULDER_POSE = 10169;
	private final int GUARD_STANCE_POSE = 10598;
	private final int DEEP_SLEEP_POSE = 11332;
	private int autoRestTimer = 0;
	private WorldPoint autoRestWorldPoint;
	private final Random random = new Random();

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(this::clearAllRests);
	}

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invoke(this::loadRunWidget);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (config.autoRest())
		{
			Player player = client.getLocalPlayer();
			WorldPoint playerLocation = player.getWorldLocation();
			autoRestTimer++;

			if (!playerLocation.equals(autoRestWorldPoint))
			{
				autoRestTimer = 0;
				autoRestWorldPoint = playerLocation;
			}

			if (restMap.containsKey(player)
					|| client.getWidget(ComponentID.BANK_CONTAINER) != null
					|| client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS) != null
					|| client.getWidget(ComponentID.DIALOG_NPC_HEAD_MODEL) != null
					|| client.getWidget(ComponentID.DIALOG_PLAYER_TEXT) != null
					|| client.getWidget(ComponentID.BANK_PIN_CONTAINER) != null
					|| player.getAnimation() != -1)
			{
				autoRestTimer = 0;
			}

			if (autoRestTimer >= config.autoRestTimer())
			{
				autoRestTimer = 0;
				startRest(player);
			}
		}

		for (Map.Entry<Player, WorldPoint> entry : restMap.entrySet())
		{
			Player player = entry.getKey();
			WorldPoint worldPoint = entry.getValue();

			if (!player.getWorldLocation().equals(worldPoint))
			{
				stopRest(player);
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() instanceof Player)
		{
			Player player = (Player) event.getActor();
			int animationID = player.getAnimation();

			if (player == client.getLocalPlayer())
			{
				autoRestTimer = 0;
			}

			if (restMap.containsKey(player) && animationID != IDLE_POSE && animationID != AnimationID.IDLE)
			{
				stopRest(player);
			}
			else if (!restMap.containsKey(player) && animationID == EMOTE_SPIN)
			{
				if (player == client.getLocalPlayer() && config.allowSpinRest() || (player != client.getLocalPlayer() && config.allowPublicRest()))
					startRest(player);
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuEntry().getOption().equals("Rest (private)"))
		{
			startRest(client.getLocalPlayer());
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() instanceof Player)
		{
			final Player player = (Player) event.getActor();

			if (restMap.containsKey(player))
			{
				stopRest(player);
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int eventId = event.getGroupId();
		if (eventId == InterfaceID.MINIMAP)
		{
			loadRunWidget();
		}

		if (!restMap.containsKey(client.getLocalPlayer()))
			return;

		if (eventId == InterfaceID.BANK
				|| eventId == InterfaceID.DIALOG_OPTION
				|| eventId == InterfaceID.DIALOG_NPC
				|| eventId == InterfaceID.DIALOG_PLAYER
				|| eventId == InterfaceID.BANK_PIN)
		{
			autoRestTimer = 0;
			stopRest(client.getLocalPlayer());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String key = event.getKey();
		if (key.equals("autoRest") || key.equals("autoRestTimer"))
		{
			autoRestTimer = 0;
			return;
		}

		if (key.equals("allowPublicResting") && !config.allowPublicRest())
		{
			clearAllRests();
			return;
		}

		if (key.equals("restMode") || key.equals("customAnimation"))
		{
			Player player = client.getLocalPlayer();
			if (!restMap.containsKey(player))
			{
				return;
			}

			stopRest(player);
			startRest(player);
		}
	}

	public void startRest(Player player)
	{

		if (restMap.containsKey(player))
		{
			if (player == client.getLocalPlayer())
			{
				stopRest(player);
			}
			return;
		}

		if (player == client.getLocalPlayer())
		{
			removeWeapon();
			player.setAnimation(AnimationID.IDLE);

			switch (config.restMode())
			{
				case REST:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case FORESTRY_REST:
					player.setIdlePoseAnimation(FORESTRY_REST_POSE);
					break;
				case SIT:
					player.setIdlePoseAnimation(SIT_POSE);
					break;
				case LOUNGE:
					player.setIdlePoseAnimation(LOUNGE_POSE);
					break;
				case DEEP_LOUNGE:
					player.setIdlePoseAnimation(DEEP_LOUNGE_POSE);
					break;
				case SLEEP:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.createSpotAnim(0, MAGIC_LUNAR_DREAM_Z, 0, 0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case RELAX:
					player.setIdlePoseAnimation(RELAX_POSE);
					break;
				case DEEP_SLEEP:
					player.setIdlePoseAnimation(DEEP_SLEEP_POSE);
					break;
				case DEAD:
					player.setIdlePoseAnimation(DEAD_POSE);
					break;
				case CROSS_ARMS:
					player.setIdlePoseAnimation(CROSS_ARMS_POSE);
					break;
				case FOLD_HANDS:
					player.setIdlePoseAnimation(FOLD_HANDS_POSE);
					break;
				case ARMS_ON_HIPS:
					player.setIdlePoseAnimation(ARMS_ON_HIPS_POSE);
					break;
				case WEAPON_ON_SHOULDER:
					player.setIdlePoseAnimation(WEAPON_ON_SHOULDER_POSE);
					break;
				case GUARD_STANCE:
					player.setIdlePoseAnimation(GUARD_STANCE_POSE);
					break;
				case LEAN_BACK:
					player.setIdlePoseAnimation(LEAN_BACK_POSE);
					break;
				case LEAN_SIDE:
					player.setIdlePoseAnimation(LEAN_SIDE_POSE);
					break;
				case CUSTOM:
					player.setIdlePoseAnimation(config.customAnimation());
			}
		}
		else
		{
			int randomInt = random.nextInt(5);
			switch (randomInt)
			{
				case 0:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case 1:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.createSpotAnim(0, MAGIC_LUNAR_DREAM_Z, 0, 0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case 2:
					player.setIdlePoseAnimation(SIT_POSE);
					break;
				case 3:
					player.setIdlePoseAnimation(LOUNGE_POSE);
					break;
				case 4:
					player.setIdlePoseAnimation(RELAX_POSE);
			}
		}

		restMap.put(player, player.getWorldLocation());
	}

	public void removeWeapon()
	{
		Player player = client.getLocalPlayer();
		PlayerComposition comp = player.getPlayerComposition();
		int[] kits = comp.getEquipmentIds();
		kits[KitType.WEAPON.getIndex()] = 0;
		kits[KitType.SHIELD.getIndex()] = 0;
		player.getPlayerComposition().setHash();
	}

	public void stopRest(Player player)
	{
		player.setAnimation(AnimationID.IDLE);

		if (player == client.getLocalPlayer())
		{
			returnWeapon();
			autoRestTimer = 0;
		}

		returnIdleAnimation(player);
		player.removeSpotAnim(0);
		restMap.remove(player);
	}

	public void clearAllRests()
	{
		for (Map.Entry<Player, WorldPoint> entry : restMap.entrySet())
		{
			Player player = entry.getKey();
			stopRest(player);
		}
	}

	public void returnWeapon()
	{
		Player player = client.getLocalPlayer();
		PlayerComposition comp = player.getPlayerComposition();
		int[] kits = comp.getEquipmentIds();

		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipmentContainer == null)
		{
			return;
		}

		Item weapon = equipmentContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon != null)
		{
			kits[KitType.WEAPON.getIndex()] = weapon.getId() + PlayerComposition.ITEM_OFFSET;
		}

		Item shield = equipmentContainer.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		if (shield != null)
		{
			kits[KitType.SHIELD.getIndex()] = shield.getId() + PlayerComposition.ITEM_OFFSET;
		}

		player.getPlayerComposition().setHash();
	}

	public void returnIdleAnimation(Player player)
	{
		if (player == client.getLocalPlayer())
		{
			ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
			if (equipmentContainer == null)
			{
				player.setIdlePoseAnimation(IDLE_POSE);
				return;
			}

			Item weapon = equipmentContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon == null)
				player.setIdlePoseAnimation(IDLE_POSE);

			if (weapon != null)
			{
				for (IdlePoses idlePoses : IdlePoses.values())
				{
					if (idlePoses.getWeaponType() == weapon.getId())
					{
						player.setIdlePoseAnimation(idlePoses.getAnimationId());
						return;
					}
				}

				player.setIdlePoseAnimation(IDLE_POSE);
			}
			return;
		}

		PlayerComposition comp = player.getPlayerComposition();
		int[] kits = comp.getEquipmentIds();
		int weaponId = kits[KitType.WEAPON.getIndex()] - PlayerComposition.ITEM_OFFSET;

		for (IdlePoses idlePoses : IdlePoses.values())
		{
			if (idlePoses.getWeaponType() == weaponId)
			{
				player.setIdlePoseAnimation(idlePoses.getAnimationId());
				return;
			}
		}

		player.setIdlePoseAnimation(IDLE_POSE);
	}

	public void loadRunWidget()
	{
		Widget widget = client.getWidget(ComponentID.MINIMAP_TOGGLE_RUN_ORB);
		if (widget == null)
		{
			return;
		}

		widget.setAction(1, "Rest (private)");
	}

	@Provides
	RestingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RestingConfig.class);
	}

}