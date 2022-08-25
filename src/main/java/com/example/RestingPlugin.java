package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
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
	private final Map<Player, Integer> weaponMap = new HashMap<>();
	private final Map<Player, Integer> idlePoseMap = new HashMap<>();
	private final int MAGIC_LUNAR_DREAM_Z = 1056;
	private final int REST_POSE = 6296;
	private final int SIT_POSE = 4851;
	private final int LOUNGE_POSE = 6284;
	private final int MAGIC_LUNAR_DREAM_SITTING_DOWN = 7627;
	private final int EMOTE_SPIN = 2107;

	@Override
	protected void shutDown() throws Exception
	{
		for (Map.Entry<Player, WorldPoint> entry : restMap.entrySet())
		{
			Player player = entry.getKey();
			player.setAnimation(-1);
			if (player.getGraphic() == MAGIC_LUNAR_DREAM_Z)
			{
				player.setGraphic(-1);
			}
			returnWeapon(player);
		}
		restMap.clear();
	}

	// Create rest option for fires
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final Player player = client.getLocalPlayer();
		final WorldPoint worldPoint = player.getWorldLocation();
		final Tile targetTile = client.getSelectedSceneTile();

		if (event.getIdentifier() == ObjectID.FIRE_26185)
		{
			client.createMenuEntry(-1).setOption("Rest (private)").setTarget(event.getTarget()).setType(MenuAction.RUNELITE).onClick(e ->
			{
				if (angledToFire(targetTile, worldPoint, player.getOrientation()))
				{
					startRest(player);
					sendChatMessage("You settle comfortably beside the fire.");
				}
				else
				{
					sendChatMessage("You must be adjacent to and facing a fire to rest in this way.");
				}
			});
		}
	}

	// Create three ways to rest: right-click on fires, right click run orb, and "spin" emote for public resting
	// Prevent resting during combat
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() instanceof Player)
		{
			final Player player = (Player) event.getActor();

			if (restMap.containsKey(player))
			{
				stopRest(player, AnimationID.IDLE);
			}
		}
	}

	// Create rest option behind right-click run orb. Create rest option behind the Spin emote (and prevent it effecting Spinning Wheels)
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		final Player player = client.getLocalPlayer();
		final MenuEntry entry = event.getMenuEntries()[0];

		if (event.getFirstEntry().getOption().equals("Toggle Run"))
		{
			client.createMenuEntry(1).setOption("Rest (private)").setTarget(entry.getTarget()).setType(MenuAction.RUNELITE).onClick(e ->
					startRest(player));
		}

		if (event.getFirstEntry().getOption().equals("Spin") && event.getMenuEntries().length == 2)
		{
			event.getFirstEntry().setOption("Rest (public)");
		}
	}

	// Loop sleep Z animations if config set to Sleep
	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		final Player player;

		if (event.getActor() instanceof Player) {
			player = (Player) event.getActor();
		}
		else
		{
			return;
		}

		if (restMap.containsKey(player) && config.restMode().equals(RestingConfig.RestMode.SLEEP))
		{
			player.setGraphic(MAGIC_LUNAR_DREAM_Z);
			player.setSpotAnimFrame(0);
		}
	}

	// If a resting player moves, cancel the rest
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (config.allowOthersRest())
		{
			List<Player> players = client.getPlayers();
			for (Player player : players)
			{
				if (restMap.containsKey(player) && hasMoved(player))
				{
					stopRest(player, AnimationID.IDLE);
				}
			}
		}
		else
		{
			Player player = client.getLocalPlayer();
			if (restMap.containsKey(player) && hasMoved(player))
			{
				stopRest(player, AnimationID.IDLE);
			}
		}
	}

	// Start rest if Spin emote is used. Cancel rest on any other animation change (like during skilling)
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		final int newAnimation = event.getActor().getAnimation();

		if (event.getActor() instanceof Player)
		{
			final Player player = (Player) event.getActor();
			if (!config.allowOthersRest() && player != client.getLocalPlayer())
			{
				return;
			}

			if (restMap.containsKey(player) && newAnimation == EMOTE_SPIN)
			{
				if (player == client.getLocalPlayer())
				{
					sendChatMessage("You're already resting!");
				}
				player.setAnimation(AnimationID.IDLE);
				return;
			}

			if (!restMap.containsKey(player) && newAnimation == EMOTE_SPIN)
			{
				player.setAnimation(AnimationID.IDLE);
				startRest(player);
				return;
			}

			if (restMap.containsKey(player) && newAnimation != AnimationID.IDLE)
			{
				stopRest(player, newAnimation);
			}
		}
	}

	// If others can no longer rest, reset their animations
	// If rest mode/animation is changed for local player, change animations
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!config.allowOthersRest())
		{
			for (Map.Entry<Player, WorldPoint> entry : restMap.entrySet())
			{
				Player player = entry.getKey();
				if (player != client.getLocalPlayer())
				{
					stopRest(player, AnimationID.IDLE);
				}
			}
		}

		Player player = client.getLocalPlayer();
		if (restMap.containsKey(player) && event.getKey().equals("restMode"))
		{
			player.setAnimation(AnimationID.IDLE);
			switch (config.restMode())
			{
				case REST:
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case SIT:
					player.setIdlePoseAnimation(SIT_POSE);
					break;
				case LOUNGE:
					player.setIdlePoseAnimation(LOUNGE_POSE);
					break;
				case SLEEP:
					player.setIdlePoseAnimation(REST_POSE);
					player.setGraphic(MAGIC_LUNAR_DREAM_Z);
					player.setSpotAnimFrame(0);
					player.setGraphicHeight(0);
			}
		}
	}

	public void startRest(Player player)
	{
		if (player == null)
		{
			return;
		}

		if (restMap.containsKey(player))
		{
			if (player == client.getLocalPlayer())
			{
				sendChatMessage("You're already resting!");
				return;
			}
			return;
		}

		removeWeapon(player);

		if (player == client.getLocalPlayer())
		{
			switch (config.restMode())
			{
				case REST:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case SIT:
					player.setIdlePoseAnimation(SIT_POSE);
					break;
				case LOUNGE:
					player.setIdlePoseAnimation(LOUNGE_POSE);
					break;
				case SLEEP:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					player.setGraphic(MAGIC_LUNAR_DREAM_Z);
					player.setSpotAnimFrame(0);
					player.setGraphicHeight(0);
			}
		}
		else
		{
			int randomNumber = (int) (Math.random() * 4 + 1);
			switch (randomNumber)
			{
				case 1:
					player.setIdlePoseAnimation(SIT_POSE);
					break;
				case 2:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					break;
				case 3:
					player.setIdlePoseAnimation(LOUNGE_POSE);
					break;
				case 4:
					player.setAnimation(MAGIC_LUNAR_DREAM_SITTING_DOWN);
					player.setAnimationFrame(0);
					player.setIdlePoseAnimation(REST_POSE);
					player.setGraphic(MAGIC_LUNAR_DREAM_Z);
					player.setSpotAnimFrame(0);
					player.setGraphicHeight(0);
			}
		}

		final WorldPoint restLocation = player.getWorldLocation();
		restMap.put(player, restLocation);
	}

	public void stopRest(Player player, int newAnimationID)
	{
		player.setAnimation(newAnimationID);
		if (player.getGraphic() == MAGIC_LUNAR_DREAM_Z)
		{
			player.setGraphic(-1);
		}
		returnWeapon(player);
		restMap.remove(player);
	}

	public void removeWeapon(Player player)
	{
		PlayerComposition comp = player.getPlayerComposition();
		int[] kits = comp.getEquipmentIds();
		int weaponId = comp.getEquipmentId(KitType.WEAPON);
		weaponMap.put(player, weaponId);

		int idlePoseId = player.getIdlePoseAnimation();
		idlePoseMap.put(player, idlePoseId);

		if (weaponId != 0)
		{
			kits[KitType.WEAPON.getIndex()] = 0;
			player.getPlayerComposition().setHash();
		}
	}

	public void returnWeapon(Player player)
	{
		PlayerComposition comp = player.getPlayerComposition();
		int[] kits = comp.getEquipmentIds();
		kits[KitType.WEAPON.getIndex()] = weaponMap.get(player) + 512;
		player.getPlayerComposition().setHash();
		weaponMap.remove(player);

		int idlePoseId = idlePoseMap.get(player);
		player.setIdlePoseAnimation(idlePoseId);
		idlePoseMap.remove(player);
	}

	public boolean angledToFire(Tile targetTile, WorldPoint worldPoint, int orientation)
	{
		return ((orientation <= 256 && orientation >= 0) || (orientation <= 2047 && orientation >= 1792)) && targetTile.getWorldLocation().dy(1).distanceTo(worldPoint) == 0
				|| ((orientation >= 256 && orientation <= 768) && targetTile.getWorldLocation().dx(1).distanceTo(worldPoint) == 0)
				|| ((orientation >= 768 && orientation <= 1280) && targetTile.getWorldLocation().dy(-1).distanceTo(worldPoint) == 0)
				|| ((orientation >= 1280 && orientation <= 1792) && targetTile.getWorldLocation().dx(-1).distanceTo(worldPoint) == 0)
				|| ((orientation >= 0 && orientation <= 512) && targetTile.getWorldLocation().dx(1).dy(1).distanceTo(worldPoint) == 0)
				|| ((orientation >= 512 && orientation <= 1024) && targetTile.getWorldLocation().dx(1).dy(-1).distanceTo(worldPoint) == 0)
				|| ((orientation >= 1024 && orientation <= 1536) && targetTile.getWorldLocation().dx(-1).dy(-1).distanceTo(worldPoint) == 0)
				|| (((orientation >= 1536 && orientation <= 2047) || (orientation == 0)) && targetTile.getWorldLocation().dx(-1).dy(1).distanceTo(worldPoint) == 0);
	}

	private boolean hasMoved(Player player)
	{
		WorldPoint currentLocation = player.getWorldLocation();
		return !restMap.get(player).equals(currentLocation);
	}

	private void sendChatMessage(String chatMessage)
	{
		final String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(chatMessage).build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build());
	}

	@Provides
	RestingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RestingConfig.class);
	}
}



