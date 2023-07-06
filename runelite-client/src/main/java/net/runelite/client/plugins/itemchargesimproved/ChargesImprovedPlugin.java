package net.runelite.client.plugins.itemchargesimproved;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsAhrimsHood;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsAhrimsRobeskirt;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsAhrimsRobetop;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsAhrimsStaff;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsDharoksGreataxe;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsDharoksHelm;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsDharoksPlatebody;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsDharoksPlatelegs;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsGuthansChainskirt;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsGuthansHelm;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsGuthansPlatebody;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsGuthansWarspear;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsKarilsCoif;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsKarilsCrossbow;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsKarilsLeatherskirt;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsKarilsLeathertop;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsToragsHammers;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsToragsHelm;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsToragsPlatebody;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsToragsPlatelegs;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsVeracsBrassard;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsVeracsFlail;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsVeracsHelm;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.BarrowsVeracsPlateskirt;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.C_Coffin;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.H_CircletOfWater;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_BraceletOfClay;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_BraceletOfExpeditious;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_BraceletOfFlamtaer;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_BraceletOfSlaughter;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_Camulet;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_CelestialRing;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_RingOfRecoil;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_RingOfSuffering;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_SlayerRing;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.J_XericsTalisman;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.S_Chronicle;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.S_CrystalShield;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.S_DragonfireShield;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.S_FaladorShield;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.S_KharedstMemoirs;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_AshSanctifier;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_BoneCrusher;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_BottomlessCompostBucket;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_FishBarrel;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_GricollersCan;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_OgreBellows;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_SoulBearer;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_TeleportCrystal;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.U_Waterskin;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_Arclight;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_BryophytasStaff;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_IbansStaff;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_PharaohsSceptre;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_SanguinestiStaff;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_SkullSceptre;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_ToxicStaffOfTheDead;
import net.runelite.client.plugins.itemchargesimproved.infoboxes.W_TridentOfTheSeas;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
	name = "Item Charges Improved",
	description = "Show charges of various items",
	tags = {
		"charges",
		"barrows",
		"bracelet",
		"ring",
		"xeric",
		"talisman",
		"book",
		"chronicle",
		"shield",
		"ash",
		"bone",
		"bottomless",
		"bucket",
		"fish",
		"gricoller",
		"can",
		"soul",
		"arclight",
		"bryophyta",
		"staff",
		"iban",
		"pharaoh",
		"sceptre",
		"skull",
		"sanguinesti",
		"trident",
		"dragonfire",
		"circlet",
		"camulet"
	}
)
public class ChargesImprovedPlugin extends Plugin {
	private final int VARBIT_MINUTES = 8354;

	public static final int CHARGES_UNKNOWN = -1;
	public static final int CHARGES_UNLIMITED = -2;

	@Inject
	private Client client;

	@Inject
	private ClientThread client_thread;

	@Inject
	private ItemManager items;

	@Inject
	private ConfigManager configs;

	@Inject
	private InfoBoxManager infoboxes;

	@Inject
	private OverlayManager overlays;

	@Inject
	private ChargesImprovedConfig config;

	@Inject
	private ChatMessageManager chat_messages;

	@Inject
	private Notifier notifier;

	@Provides
	ChargesImprovedConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ChargesImprovedConfig.class);
	}

	private ChargedItemsOverlay overlay_charged_items;

	private ChargedItemInfoBox[] infoboxes_charged_items;

	private final ZoneId timezone = ZoneId.of("Europe/London");
	private String date = LocalDateTime.now(timezone).format(DateTimeFormatter.ISO_LOCAL_DATE);

	@Override
	protected void startUp() {
		infoboxes_charged_items = new ChargedItemInfoBox[]{
			// Weapons
			new W_Arclight(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_TridentOfTheSeas(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_SkullSceptre(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_IbansStaff(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_PharaohsSceptre(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_BryophytasStaff(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_SanguinestiStaff(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new W_ToxicStaffOfTheDead(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			// Shields
			new S_KharedstMemoirs(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new S_Chronicle(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new S_CrystalShield(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new S_FaladorShield(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new S_DragonfireShield(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			// Jewellery
			new J_BraceletOfClay(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_BraceletOfExpeditious(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_BraceletOfFlamtaer(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_BraceletOfSlaughter(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_CelestialRing(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_RingOfRecoil(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_RingOfSuffering(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_SlayerRing(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_XericsTalisman(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new J_Camulet(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			// Helms
			new H_CircletOfWater(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			// Capes
			new C_Coffin(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			// Utilities
			new U_AshSanctifier(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_BoneCrusher(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_BottomlessCompostBucket(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_FishBarrel(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_GricollersCan(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_SoulBearer(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_TeleportCrystal(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_Waterskin(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new U_OgreBellows(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsAhrimsHood(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsAhrimsRobetop(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsAhrimsRobeskirt(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsAhrimsStaff(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsDharoksHelm(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsDharoksPlatebody(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsDharoksPlatelegs(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsDharoksGreataxe(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsGuthansHelm(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsGuthansPlatebody(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsGuthansChainskirt(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsGuthansWarspear(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsKarilsCoif(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsKarilsLeathertop(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsKarilsLeatherskirt(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsKarilsCrossbow(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsToragsHelm(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsToragsPlatebody(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsToragsPlatelegs(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsToragsHammers(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),

			new BarrowsVeracsHelm(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsVeracsBrassard(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsVeracsPlateskirt(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
			new BarrowsVeracsFlail(client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, this),
		};
		overlay_charged_items = new ChargedItemsOverlay(config, infoboxes_charged_items);

		overlays.add(overlay_charged_items);
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infoboxes.addInfoBox(infobox));
	}

	@Override
	protected void shutDown() {
		overlays.remove(overlay_charged_items);
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infoboxes.removeInfoBox(infobox));
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event) {
		for (final ChargedItemInfoBox infobox : this.infoboxes_charged_items) {
			infobox.onItemContainersChanged(event);
		}

		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

		if (inventory != null && equipment != null) {
			// We need to know about items to show messages about resetting charges.
			if (!config.getResetDate().equals(date)) {
				resetCharges(date);
			}
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onChatMessage(event));
//		System.out.println("MESSAGE | " +
//			"type: " + event.getType().name() +
//			", message: " + event.getMessage().replaceAll("</?col.*?>", "") +
//			", sender: " + event.getSender()
//		);
	}

	@Subscribe
	public void onAnimationChanged(final AnimationChanged event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onAnimationChanged(event));
//		if (event.getActor() == client.getLocalPlayer()) {
//			System.out.println("ANIMATION | " +
//				"id: " + event.getActor().getAnimation()
//			);
//		}
	}

	@Subscribe
	public void onGraphicChanged(final GraphicChanged event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onGraphicChanged(event));
//		if (event.getActor() == client.getLocalPlayer()) {
//			System.out.println("GRAPHIC | " +
//				"id: " + event.getActor().getGraphic()
//			);
//		}
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onConfigChanged(event));
//		if (event.getGroup().equals(ChargesImprovedConfig.group)) {
//			System.out.println("CONFIG | " +
//				"key: " + event.getKey() +
//				", old value: " + event.getOldValue() +
//				", new value: " + event.getNewValue()
//			);
//		}
	}

	@Subscribe
	public void onHitsplatApplied(final HitsplatApplied event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onHitsplatApplied(event));
		System.out.println("HITSPLAT | " +
			"actor: " + (event.getActor() == client.getLocalPlayer() ? "self" : "enemy") +
			", type: " + event.getHitsplat().getHitsplatType() +
			", amount:" + event.getHitsplat().getAmount() +
			", others = " + event.getHitsplat().isOthers() +
			", mine = " + event.getHitsplat().isMine()
		);
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onWidgetLoaded(event));
//		System.out.println("WIDGET | " +
//			"group: " + event.getGroupId()
//		);
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked event) {
		Arrays.stream(infoboxes_charged_items).forEach(infobox -> infobox.onMenuOptionClicked(event));
//		System.out.println("OPTION | " +
//			"option: " + event.getMenuOption() +
//			", target: " + event.getMenuTarget() +
//			", action name: " + event.getMenuAction().name() +
//			", action id: " + event.getMenuAction().getId()
//		);
	}

	@Subscribe
	public void onVarbitChanged(final VarbitChanged event) {
		// If server minutes are 0, it's a new day!
		if (event.getVarbitId() == VARBIT_MINUTES && client.getGameState() == GameState.LOGGED_IN && event.getValue() == 0) {
			final String date = LocalDateTime.now(timezone).format(DateTimeFormatter.ISO_LOCAL_DATE);
			resetCharges(date);
		}
	}

	@Subscribe
	public void onGameTick(final GameTick gametick) {
		for (final ChargedItemInfoBox infobox : this.infoboxes_charged_items) {
			infobox.onGameTick(gametick);
		}
	}

	private void resetCharges(final String date) {
		configs.setConfiguration(ChargesImprovedConfig.group, ChargesImprovedConfig.date, date);
		Arrays.stream(infoboxes_charged_items).forEach(ChargedItemInfoBox::resetCharges);
	}

	public static String getChargesMinified(final int charges) {
		if (charges == CHARGES_UNLIMITED) return "∞";
		if (charges == CHARGES_UNKNOWN) return "?";
		if (charges < 1000) return String.valueOf(charges);
		if (charges >= 1000000) return charges / 1000000 + "M";

		final int thousands = charges / 1000;
		final int hundreds = Math.min((charges % 1000 + 50) / 100, 9);

		return thousands + (thousands < 10 && hundreds > 0 ? "." + hundreds : "") + "K";
	}
}

