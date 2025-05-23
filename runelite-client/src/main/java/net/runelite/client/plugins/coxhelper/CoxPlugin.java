package net.runelite.client.plugins.coxhelper;

import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.coxhelper.Util.Text;
import net.runelite.client.ui.overlay.OverlayManager;

import static net.runelite.api.GraphicID.GRAPHICS_OBJECT_ROCKFALL;

@PluginDescriptor(
	name = "CoX Helper",
	enabledByDefault = false,
	description = "All-in-one plugin for Chambers of Xeric",
	tags = {"CoX", "chamber", "xeric", "helper"}
)
@Slf4j
@Getter(AccessLevel.PACKAGE)
public class CoxPlugin extends Plugin
{
	private static final int ANIMATION_ID_G1 = 430;
	private static final Pattern TP_REGEX = Pattern.compile("You have been paired with <col=ff0000>(.*)</col>! The magical power will enact soon...");
	private final Map<NPC, NPCContainer> npcContainers = new HashMap<>();
	@Inject
	@Getter(AccessLevel.NONE)
	private Client client;
	@Inject
	@Getter(AccessLevel.NONE)
	private ChatMessageManager chatMessageManager;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxOverlay coxOverlay;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxInfoBox coxInfoBox;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxDebugBox coxDebugBox;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxConfig config;
	@Inject
	@Getter(AccessLevel.NONE)
	private OverlayManager overlayManager;
	@Inject
	@Getter(AccessLevel.NONE)
	private EventBus eventBus;
	@Inject
	private Olm olm;
	//other

	int ticks;
	private int vanguards;
	private boolean tektonActive;
	private int tektonAttackTicks;

	private List<LocalPoint> olmTileLocalPoints = new ArrayList<>();

	public static final int TEKTON_ANVIL = 7475;
	public static final int TEKTON_AUTO1 = 7482;
	public static final int TEKTON_AUTO2 = 7483;
	public static final int TEKTON_AUTO3 = 7484;
	public static final int TEKTON_FAST_AUTO1 = 7478;
	public static final int TEKTON_FAST_AUTO2 = 7488;
	public static final int TEKTON_ENRAGE_AUTO1 = 7492;
	public static final int TEKTON_ENRAGE_AUTO2 = 7493;
	public static final int TEKTON_ENRAGE_AUTO3 = 7494;

	@Provides
	CoxConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxConfig.class);
	}

	@Override
	protected void startUp()
	{
		this.overlayManager.add(this.coxOverlay);
		this.overlayManager.add(this.coxInfoBox);
		this.overlayManager.add(this.coxDebugBox);
		this.olm.hardRest();
	}

	@Override
	protected void shutDown()
	{
		this.overlayManager.remove(this.coxOverlay);
		this.overlayManager.remove(this.coxInfoBox);
		this.overlayManager.remove(this.coxDebugBox);
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!this.inRaid())
		{
			return;
		}

		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			final Matcher tpMatcher = TP_REGEX.matcher(event.getMessage());

			if (tpMatcher.matches())
			{
				for (Player player : this.client.getPlayers())
				{
					final String rawPlayerName = player.getName();

					if (rawPlayerName != null)
					{
						final String fixedPlayerName = Text.sanitize(rawPlayerName);

						if (fixedPlayerName.equals(Text.sanitize(tpMatcher.group(1))))
						{
							this.olm.getVictims().add(new Victim(player, Victim.Type.TELEPORT));
						}
					}
				}
			}

			switch (Text.standardize(event.getMessageNode().getValue()))
			{
				case "the great olm rises with the power of acid.":
					olm.setPhaseType(Olm.PhaseType.ACID);
					break;
				case "the great olm rises with the power of crystal.":
					olm.setPhaseType(Olm.PhaseType.CRYSTAL);
					break;
				case "the great olm rises with the power of flame.":
					olm.setPhaseType(Olm.PhaseType.FLAME);
					break;
				case "the great olm fires a sphere of aggression your way. your prayers have been sapped.":
				case "the great olm fires a sphere of aggression your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MELEE);
					break;
				case "the great olm fires a sphere of magical power your way. your prayers have been sapped.":
				case "the great olm fires a sphere of magical power your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MAGIC);
					break;
				case "the great olm fires a sphere of accuracy and dexterity your way. your prayers have been sapped.":
				case "the great olm fires a sphere of accuracy and dexterity your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MISSILES);
					break;
			}
		}
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final Projectile projectile = event.getProjectile();

		switch (projectile.getId())
		{
			// OLM
			case ProjectileID.OLM_MAGE_ATTACK:
				this.olm.setPrayer(Prayer.PROTECT_FROM_MAGIC);
				break;
			case ProjectileID.OLM_RANGE_ATTACK:
				this.olm.setPrayer(Prayer.PROTECT_FROM_MISSILES);
				break;
			case ProjectileID.OLM_ACID_TRAIL:
				Actor actor = projectile.getInteracting();
				if (actor instanceof Player)
				{
					this.olm.getVictims().add(new Victim((Player) actor, Victim.Type.ACID));
				}
				break;
		}
	}

	@Subscribe
	private void onGraphicChanged(GraphicChanged event)
	{
		if (!this.inRaid())
		{
			return;
		}

		if (!(event.getActor() instanceof Player))
		{
			return;
		}

		final Player player = (Player) event.getActor();

		if (player.getGraphic() == GraphicID.OLM_BURN)
		{
			this.olm.getVictims().add(new Victim(player, Victim.Type.BURN));
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.TEKTON:
			case NpcID.TEKTON_7541:
			case NpcID.TEKTON_7542:
			case NpcID.TEKTON_7545:
			case NpcID.TEKTON_ENRAGED:
			case NpcID.TEKTON_ENRAGED_7544:
				this.npcContainers.put(npc, new NPCContainer(npc));
				this.tektonAttackTicks = 27;
				break;
			case NpcID.MUTTADILE:
			case NpcID.MUTTADILE_7562:
			case NpcID.MUTTADILE_7563:
			case NpcID.GUARDIAN:
			case NpcID.GUARDIAN_7570:
				this.npcContainers.put(npc, new NPCContainer(npc));
				break;
			case NpcID.VANGUARD:
			case NpcID.VANGUARD_7526:
			case NpcID.VANGUARD_7527:
			case NpcID.VANGUARD_7528:
			case NpcID.VANGUARD_7529:
				this.vanguards++;
				this.npcContainers.put(npc, new NPCContainer(npc));
				break;
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.TEKTON:
			case NpcID.TEKTON_7541:
			case NpcID.TEKTON_7542:
			case NpcID.TEKTON_7545:
			case NpcID.TEKTON_ENRAGED:
			case NpcID.TEKTON_ENRAGED_7544:
			case NpcID.MUTTADILE:
			case NpcID.MUTTADILE_7562:
			case NpcID.MUTTADILE_7563:
			case NpcID.GUARDIAN:
			case NpcID.GUARDIAN_7570:
			case NpcID.GUARDIAN_7571:
			case NpcID.GUARDIAN_7572:
				if (this.npcContainers.remove(event.getNpc()) != null && !this.npcContainers.isEmpty())
				{
					this.npcContainers.remove(event.getNpc());
				}
				break;
			case NpcID.VANGUARD:
			case NpcID.VANGUARD_7526:
			case NpcID.VANGUARD_7527:
			case NpcID.VANGUARD_7528:
			case NpcID.VANGUARD_7529:
				if (this.npcContainers.remove(event.getNpc()) != null && !this.npcContainers.isEmpty())
				{
					this.npcContainers.remove(event.getNpc());
				}
				this.vanguards--;
				break;
		}
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!this.inRaid())
		{
			this.olm.hardRest();
			return;
		}

		this.handleNpcs();

		if (this.olm.isActive())
		{
			this.olm.update();
			this.handleProjectileMarkers();
		}

		++ticks;
	}

	private void handleProjectileMarkers() {
		System.out.println("ticks: " + ticks);
	}

	private void handleNpcs()
	{
		for (NPCContainer npcs : this.getNpcContainers().values())
		{
			switch (npcs.getNpc().getId())
			{
				case NpcID.TEKTON:
				case NpcID.TEKTON_7541:
				case NpcID.TEKTON_7542:
				case NpcID.TEKTON_7545:
				case NpcID.TEKTON_ENRAGED:
				case NpcID.TEKTON_ENRAGED_7544:
					npcs.setTicksUntilAttack(npcs.getTicksUntilAttack() - 1);
					npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					switch (npcs.getNpc().getAnimation())
					{
						case TEKTON_AUTO1:
						case TEKTON_AUTO2:
						case TEKTON_AUTO3:
						case TEKTON_ENRAGE_AUTO1:
						case TEKTON_ENRAGE_AUTO2:
						case TEKTON_ENRAGE_AUTO3:
							this.tektonActive = true;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(4);
							}
							break;
						case TEKTON_FAST_AUTO1:
						case TEKTON_FAST_AUTO2:
							this.tektonActive = true;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(3);
							}
							break;
						case TEKTON_ANVIL:
							this.tektonActive = false;
							this.tektonAttackTicks = 47;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(15);
							}
					}
					break;
				case NpcID.GUARDIAN:
				case NpcID.GUARDIAN_7570:
				case NpcID.GUARDIAN_7571:
				case NpcID.GUARDIAN_7572:
					npcs.setTicksUntilAttack(npcs.getTicksUntilAttack() - 1);
					npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					if (npcs.getNpc().getAnimation() == ANIMATION_ID_G1 &&
						npcs.getTicksUntilAttack() < 1)
					{
						npcs.setTicksUntilAttack(5);
					}
					break;
				case NpcID.VANGUARD_7529:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.MAGE);
					}
					break;
				case NpcID.VANGUARD_7528:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.RANGE);
					}
					break;
				case NpcID.VANGUARD_7527:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					}
					break;
			}
		}
		if (this.tektonActive && this.tektonAttackTicks > 0)
		{
			this.tektonAttackTicks--;
		}
	}

	boolean inRaid()
	{
		return this.client.getVarbitValue(Varbits.IN_RAID) == 1;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (event.getGameObject() == null)
		{
			return;
		}

		int id = event.getGameObject().getId();
		switch (id)
		{
			case Olm.HEAD_GAMEOBJECT_RISING:
			case Olm.HEAD_GAMEOBJECT_READY:
				if (this.olm.getHead() == null)
				{
					this.olm.startPhase();
				}
				this.olm.setHead(event.getGameObject());
				break;
			case Olm.LEFT_HAND_GAMEOBJECT_RISING:
			case Olm.LEFT_HAND_GAMEOBJECT_READY:
				this.olm.setHand(event.getGameObject());
				break;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (event.getGameObject() == null)
		{
			return;
		}

		int id = event.getGameObject().getId();
		if (id == Olm.HEAD_GAMEOBJECT_READY)
		{
			this.olm.setHead(null);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated graphicsObjectCreated)
	{
		GraphicsObject graphicsObject = graphicsObjectCreated.getGraphicsObject();
		// OLM LIGHTNING
		if (graphicsObject.getId() == GraphicID.OLM_LIGHTNING)
		{
			if (!olmTileLocalPoints.contains(graphicsObject.getLocation()))
				graphicsObject.getLocation();
		}
	}
}