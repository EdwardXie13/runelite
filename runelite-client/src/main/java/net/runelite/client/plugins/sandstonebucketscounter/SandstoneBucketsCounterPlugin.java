package net.runelite.client.plugins.sandstonebucketscounter;

import com.google.inject.Provides;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import static net.runelite.api.ItemID.SANDSTONE_10KG;
import static net.runelite.api.ItemID.SANDSTONE_1KG;
import static net.runelite.api.ItemID.SANDSTONE_2KG;
import static net.runelite.api.ItemID.SANDSTONE_5KG;
import net.runelite.api.MessageNode;
import net.runelite.api.NpcID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModelType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(name = "Sandstone Buckets Counter", description = "Show how many buckets of sand in sandstones player has in their inventory/grinder.", tags = {"sandstone", "desert quarry", "quarry", "bucket of sand"})
@Slf4j
public class SandstoneBucketsCounterPlugin extends Plugin
{
	private static final int DESERT_QUARRY_REGION = 12589;

	private static final Pattern GRINDER_NPC_DEPOSIT_BUCKET_PATTERN = Pattern.compile("The grinder is now holding enough sandstone equivalent to (?<filledBucketCount>[\\d,]+) buckets of sand.");

	private static final Pattern GRINDER_DEPOSIT_PATTERN = Pattern.compile("The grinder is holding enough sandstone for (?<filledBucketCount>[\\d,]+) buckets of sand.");

	private static final Pattern GRINDER_NPC_CHECK_BUCKET_PATTERN = Pattern.compile("I have (?<emptyBucketCount>[\\d,]+) of your buckets and you've ground enough sandstone for (?<filledBucketCount>[\\d,]+) buckets of sand.");

	private static final String CONFIG_GRINDER_STORAGE_KEY = "numStoredInGrinder";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SandstoneBucketsCounterOverlay overlay;

	@Getter(AccessLevel.PACKAGE)
	private int inventoryCount;

	private int grinderCount;

	@Getter(AccessLevel.PACKAGE)
	private boolean isInDesertQuarry;

	@Override
	protected void startUp() throws Exception
	{
		reset();
		overlayManager.add(overlay);
		clientThread.invokeLater(() -> {
			final ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);

			if (container != null)
			{
				calculateInventory(container.getItems());
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		reset();
	}

	private void reset()
	{
		inventoryCount = 0;
	}

	private boolean isInDesertQuarryRegion()
	{
		if (client.getLocalPlayer() != null)
		{
			return client.getLocalPlayer().getWorldLocation().getRegionID() == DESERT_QUARRY_REGION;
		}

		return false;
	}

	public int getGrinderCount()
	{
		Integer configGrinderCount = configManager.getRSProfileConfiguration(SandstoneBucketsCounterConfig.CONFIG_GROUP_NAME, CONFIG_GRINDER_STORAGE_KEY, int.class);
		return configGrinderCount != null ? configGrinderCount : grinderCount;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (!isInDesertQuarryRegion())
		{
			if (isInDesertQuarry)
			{
				log.debug("Left Desert Quarry region");
				reset();
			}

			isInDesertQuarry = false;

			return;
		}

		if (!isInDesertQuarry)
		{
			reset();
			log.debug("Entered Desert Quarry region");
		}

		isInDesertQuarry = true;

		checkChatBox();
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (!isInDesertQuarry)
		{
			return;
		}

		ChatMessageType chatMessageType = chatMessage.getType();
		if (chatMessageType != ChatMessageType.GAMEMESSAGE && chatMessageType != ChatMessageType.SPAM)
		{
			return;
		}

		MessageNode messageNode = chatMessage.getMessageNode();
		String normalizedMessageValue = messageNode.getValue().replace("one bucket", "1 buckets");
		Matcher textMatcher = GRINDER_DEPOSIT_PATTERN.matcher(normalizedMessageValue);
		if (!textMatcher.find())
		{
			return;
		}

		updateGrinderCount(textMatcher.group("filledBucketCount"));
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		final ItemContainer container = event.getItemContainer();

		if (!isInDesertQuarry || container != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		calculateInventory(container.getItems());
	}

	@Provides
	SandstoneBucketsCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SandstoneBucketsCounterConfig.class);
	}

	private void checkChatBox()
	{
		Widget npcDialog = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
		if (npcDialog == null)
		{
			return;
		}

		Widget name = client.getWidget(ComponentID.DIALOG_NPC_NAME);
		Widget head = client.getWidget(ComponentID.DIALOG_NPC_HEAD_MODEL);
		if (name == null || head == null || head.getModelType() != WidgetModelType.NPC_CHATHEAD)
		{
			return;
		}

		final int npcId = head.getModelId();
		if (npcId != NpcID.DREW)
		{
			return;
		}

		String npcText = Text.sanitizeMultilineText(npcDialog.getText());
		Matcher textMatcher = GRINDER_NPC_DEPOSIT_BUCKET_PATTERN.matcher(npcText);
		if (!textMatcher.find())
		{
			textMatcher = GRINDER_NPC_CHECK_BUCKET_PATTERN.matcher(npcText);
			if (!textMatcher.find())
			{
				return;
			}
		}

		updateGrinderCount(textMatcher.group("filledBucketCount"));
	}

	private void calculateInventory(Item[] inv)
	{
		inventoryCount = 0;

		for (Item item : inv)
		{
			inventoryCount += getBucketsPotentialCount(item.getId());
		}
	}

	private void updateGrinderCount(String candidate)
	{
		grinderCount = Integer.parseInt(candidate.replace(",", ""));
		configManager.setRSProfileConfiguration(SandstoneBucketsCounterConfig.CONFIG_GROUP_NAME, CONFIG_GRINDER_STORAGE_KEY, grinderCount);
	}

	private static int getBucketsPotentialCount(int id)
	{
		switch (id)
		{
			case SANDSTONE_1KG:
				return 1;
			case SANDSTONE_2KG:
				return 2;
			case SANDSTONE_5KG:
				return 4;
			case SANDSTONE_10KG:
				return 8;
			default:
				return 0;
		}
	}
}
