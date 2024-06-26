package net.runelite.client.plugins.itemchargesimproved;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerAnimation;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerChatMessage;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerGraphic;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerHitsplat;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItemContainer;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerMenuOption;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerReset;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChargedItemInfoBox extends InfoBox {
    public final ChargesItem infobox_id;
    public int item_id;
    protected final Client client;
    protected final ClientThread client_thread;
    protected final ItemManager items;
    protected final InfoBoxManager infoboxes;
    protected final ConfigManager configs;
    protected final ChatMessageManager chat_messages;
    protected final Notifier notifier;
    protected final ChargesImprovedConfig config;

    @Nullable public ItemContainer inventory;
    @Nullable protected ItemContainer equipment;
    @Nullable private Item[] inventory_items;

    @Nullable protected String config_key;
    @Nullable protected String[] extra_config_keys;
    @Nullable protected TriggerChatMessage[] triggers_chat_messages;
    @Nullable protected TriggerAnimation[] triggers_animations;
    @Nullable protected TriggerGraphic[] triggers_graphics;
    @Nullable protected TriggerHitsplat[] triggers_hitsplats;
    @Nullable protected TriggerItem[] triggers_items;
    @Nullable protected TriggerWidget[] triggers_widgets;
    @Nullable protected TriggerReset[] triggers_resets;
    @Nullable protected TriggerItemContainer[] triggers_item_containers;
    @Nullable protected TriggerMenuOption[] triggers_menu_options;

    protected boolean needs_to_be_equipped_for_infobox;
    private boolean in_equipment;
    private boolean in_inventory;
    private final List<String[]> menu_entries = new ArrayList<>();
    private int animation = -1;
    private int graphic = -1;
    protected int charges = ChargesImprovedPlugin.CHARGES_UNKNOWN;

    private String tooltip;
    private boolean render = false;
    public boolean zero_charges_is_positive = false;
    private int gametick = 0;
    private int gametick_before = 0;

    public ChargedItemInfoBox(
        final ChargesItem infobox_id,
        final int item_id,
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Plugin plugin
    ) {
        super(items.getImage(item_id), plugin);
        this.infobox_id = infobox_id;
        this.item_id = item_id;
        this.client = client;
        this.client_thread = client_thread;
        this.configs = configs;
        this.items = items;
        this.infoboxes = infoboxes;
        this.chat_messages = chat_messages;
        this.notifier = notifier;
        this.config = config;

        client_thread.invokeLater(() -> {
            loadChargesFromConfig();
            updateTooltip();
            onChargesUpdated();
        });
    }

    @Override
    public String getName() {
        return super.getName() + "_" + item_id;
    }

    @Override
    public String getText() {
        return ChargesImprovedPlugin.getChargesMinified(needs_to_be_equipped_for_infobox && !in_equipment ? 0 : charges);
    }

    @Override
    public String getTooltip() {
        return tooltip;
    }

    @Override
    public Color getTextColor() {
        return getText().equals("?") ? config.getColorUnknown() : getText().equals("0") && !zero_charges_is_positive ? config.getColorEmpty() : config.getColorDefault();
    }

    private boolean isAllowed() {
        return !config.getHiddenInfoboxes().contains(infobox_id);
    }

    @Override
    public boolean render() {
        return config.showInfoboxes() && isAllowed() && render;
    }

    public int getCharges() {
        return charges;
    }

    public void onItemContainersChanged(final ItemContainerChanged event) {
        if (event.getItemContainer() == null) return;

        // Find items difference before items are overridden.
        int items_difference = 0;
        if (event.getItemContainer().getId() == InventoryID.INVENTORY.getId() && inventory_items != null) {
            items_difference = itemsDifference(inventory_items, event.getItemContainer().getItems());
        }

        // Update inventory reference.
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            inventory = event.getItemContainer();
            inventory_items = inventory.getItems();
        }

        if (triggers_item_containers != null) {
            for (final TriggerItemContainer trigger_item_container : triggers_item_containers) {
                // Item container is wrong.
                if (trigger_item_container.inventory_id != event.getContainerId()) continue;

                // Menu target check.
                if (
                    trigger_item_container.menu_target != null &&
                    menu_entries.stream().noneMatch(entry -> entry[0].equals(trigger_item_container.menu_target))
                ) continue;

                // Menu option check.
                if (
                    trigger_item_container.menu_option != null &&
                    menu_entries.stream().noneMatch(entry -> entry[1].equals(trigger_item_container.menu_option))
                ) continue;

                // Fixed charges.
                if (trigger_item_container.fixed_charges != null) {
                    setCharges(trigger_item_container.fixed_charges);
                    break;
                }

                // Increase by difference of amount of items.
                if (trigger_item_container.increase_by_difference) {
                    increaseCharges(items_difference);
                    break;
                }

                // Charges dynamically based on the items of the item container.
                if (event.getItemContainer() != null) {
                    setCharges(event.getItemContainer().count());
                    break;
                }
            }
        }

        // Save inventory and equipment item containers for other uses.
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            inventory = event.getItemContainer();
        } else if (event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
            equipment = event.getItemContainer();
        }

        // No trigger items to detect charges.
        if (triggers_items == null) return;

        boolean in_equipment = false;
        boolean in_inventory = false;
        boolean render = false;
        Integer charges = null;

        for (final TriggerItem trigger_item : triggers_items) {
            // Find out if item is equipped.
            final boolean in_equipment_item = equipment != null && equipment.contains(trigger_item.item_id);
            final boolean in_inventory_item = inventory != null && inventory.contains(trigger_item.item_id);

            // Find out if infobox should be rendered.
            if (in_inventory_item || in_equipment_item) {
                render = true;

                // Update infobox item picture and tooltip dynamically based on the items if use has different variant of it.
                if (trigger_item.item_id != item_id) {
                    updateInfobox(trigger_item.item_id);
                }

                if (in_equipment_item) in_equipment = true;
                if (in_inventory_item) in_inventory = true;
            }

            // Item not found, don't calculate charges.
            if (!in_equipment_item && !in_inventory_item) continue;

            // Find out charges for the item.
            if (trigger_item.fixed_charges != null) {
                if (charges == null) charges = 0;
                charges += inventory != null ? inventory.count(trigger_item.item_id) * trigger_item.fixed_charges : 0;
                charges += equipment != null ? equipment.count(trigger_item.item_id) * trigger_item.fixed_charges : 0;
            }
        }

        // Update infobox variables for other triggers.
        this.in_equipment = in_equipment;
        this.in_inventory = in_inventory;
        this.render = render;
        if (charges != null) this.charges = charges;
    }

    public void onChatMessage(final ChatMessage event) {
        if (
            // No chat messages triggers.
            triggers_chat_messages == null ||
            // Message type that we are not interested in.
            event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM ||
            // No config to save charges to.
            config_key == null ||
            // Not in inventory nor in equipment.
            (!in_inventory && !in_equipment)
        ) return;

        final String message = event.getMessage().replaceAll("</?col.*?>", "").replaceAll("<br>", " ");

        for (final TriggerChatMessage chat_message : triggers_chat_messages) {
            final Pattern regex = chat_message.message;
            final Matcher matcher = regex.matcher(message);

            // Message does not match the pattern.
            if (!matcher.find()) continue;

            // Menu target check.
            if (
                chat_message.menu_target &&
                menu_entries.stream().noneMatch(entry -> entry[0].equals(items.getItemComposition(item_id).getName()))
            ) continue;

            // Item needs to be equipped but isn't.
            if (chat_message.equipped && !in_equipment) continue;

            // Increase charges by fixed amount.
            if (chat_message.increase_charges != null) {
                increaseCharges(chat_message.increase_charges);

            // Decrease charges by fixed amount.
            } else if (chat_message.decrease_charges != null) {
                decreaseCharges(chat_message.decrease_charges);

            // Set charges to fixed amount.
            } else if (chat_message.fixed_charges != null) {
                setCharges(chat_message.fixed_charges);

            // Set charges from multiple amounts.
            } else if (chat_message.multiple_charges) {
                int charges = 0;

                final Matcher matcher_multiple = Pattern.compile(".*?(\\d+)").matcher(message);
                while (matcher_multiple.find()) {
                    charges += Integer.parseInt(matcher_multiple.group(1));
                }

                setCharges(charges);

            // Custom consumer.
            } else if (chat_message.consumer != null) {
                chat_message.consumer.accept(message);

            // Set charges dynamically from the chat message.
            } else if (matcher.group("charges") != null) {
                setCharges(Integer.parseInt(matcher.group("charges").replaceAll(",", "").replaceAll("\\.", "")));

            // No trigger found.
            } else {
                continue;
            }

            // Check extra matches groups.
            if (extra_config_keys != null) {
                for (final String extra_group : extra_config_keys) {
                    final String extra = matcher.group(extra_group);
                    if (extra != null) {
                        setConfiguration(config_key + "_" + extra_group, extra.replaceAll(",", ""));
                    }
                }
            }

            // Notifications.
            if (chat_message.notification) {
                notifier.notify(chat_message.notification_message != null ? chat_message.notification_message : message);
            }

            // Chat message used, no need to check other messages.
            return;
        }
    }
    public void onWidgetLoaded(final WidgetLoaded event) {
        if (triggers_widgets == null || config_key == null) return;

        client_thread.invokeLater(() -> {
            for (final TriggerWidget trigger_widget : triggers_widgets) {
                if (event.getGroupId() != trigger_widget.group_id) continue;

                Widget widget = client.getWidget(trigger_widget.group_id, trigger_widget.child_id);
                if (trigger_widget.sub_child_id != null && widget != null) widget = widget.getChild(trigger_widget.sub_child_id);
                if (widget == null) continue;

                final String message = widget.getText().replaceAll("</?col.*?>", "").replaceAll("<br>", " ");
                final Pattern regex = Pattern.compile(trigger_widget.message);
                final Matcher matcher = regex.matcher(message);
                if (!matcher.find()) continue;

                // Charges amount is fixed.
                if (trigger_widget.charges != null) {
                    setCharges(trigger_widget.charges);
                // Charges amount has custom logic.
                } else if (trigger_widget.consumer != null) {
                    trigger_widget.consumer.accept(message);
                // Charges amount is dynamic.
                } else if (matcher.group("charges") != null) {
                    final int charges = Integer.parseInt(matcher.group("charges").replaceAll(",", ""));

                    // Charges increased dynamically.
                    if (trigger_widget.increase_dynamically) {
                        increaseCharges(charges);
                    } else {
                        setCharges(charges);
                    }
                }

                // Check extra matches groups.
                if (extra_config_keys != null) {
                    for (final String extra_group : extra_config_keys) {
                        final String extra = matcher.group(extra_group);
                        if (extra != null) setConfiguration(config_key + "_" + extra_group, extra);
                    }
                }
            }
        });
    }

    public void onConfigChanged(final ConfigChanged event) {
        if (event.getGroup().equals(ChargesImprovedConfig.group) && event.getKey().equals(config_key)) {
            charges = Integer.parseInt(event.getNewValue());
        }
    }

    public void onAnimationChanged(final AnimationChanged event) {
        // Player check.
        if (event.getActor() != client.getLocalPlayer()) return;

        // Save animation ID for others to use.
        animation = event.getActor().getAnimation();

        // No animations to check.
        if (inventory == null || triggers_animations == null || charges == ChargesImprovedPlugin.CHARGES_UNKNOWN || triggers_items == null) return;

        // Check all animation triggers.
        for (final TriggerAnimation trigger_animation : triggers_animations) {
            // Valid animation id check.
            if (trigger_animation.animation_id != event.getActor().getAnimation()) continue;

            // Unallowed items check.
            if (trigger_animation.unallowed_items != null) {
                boolean unallowed_items = false;
                for (final int item_id : trigger_animation.unallowed_items) {
                    if (inventory.contains(item_id) || equipment != null && equipment.contains(item_id)) {
                        unallowed_items = true;
                        break;
                    }
                }
                if (unallowed_items) continue;
            }

            // Equipped check.
            if (trigger_animation.equipped) {
                boolean equipped = false;
                for (final TriggerItem trigger_item : triggers_items) {
                    if (equipment != null && equipment.contains(trigger_item.item_id)) {
                        equipped = true;
                        break;
                    }
                }
                if (!equipped) continue;
            }

            // Menu target check.
            if (
                trigger_animation.menu_target &&
                menu_entries.stream().noneMatch(entry -> entry[0].equals(items.getItemComposition(item_id).getName()))
            ) continue;

            // Menu option check.
            if (
                trigger_animation.menu_option != null &&
                menu_entries.stream().noneMatch(entry -> entry[1].equals(trigger_animation.menu_option))
            ) continue;

            // Valid trigger, modify charges.
            if (trigger_animation.decrease_charges) {
                decreaseCharges(trigger_animation.charges);
            } else {
                increaseCharges(trigger_animation.charges);
            }
        }
    }

    public void onGraphicChanged(final GraphicChanged event) {
        // Player check.
        if (event.getActor() != client.getLocalPlayer()) return;

        // Save animation ID for others to use.
        graphic = event.getActor().getGraphic();

        // No animations to check.
        if (inventory == null || triggers_graphics == null || charges == ChargesImprovedPlugin.CHARGES_UNKNOWN || triggers_items == null) return;

        // Check all animation triggers.
        for (final TriggerGraphic trigger_graphic : triggers_graphics) {
            // Valid animation id check.
            if (trigger_graphic.graphic_id != event.getActor().getGraphic()) continue;

            // Unallowed items check.
            if (trigger_graphic.unallowed_items != null) {
                boolean unallowed_items = false;
                for (final int item_id : trigger_graphic.unallowed_items) {
                    if (inventory.contains(item_id) || equipment != null && equipment.contains(item_id)) {
                        unallowed_items = true;
                        break;
                    }
                }
                if (unallowed_items) continue;
            }

            // Equipped check.
            if (trigger_graphic.equipped) {
                boolean equipped = false;
                for (final TriggerItem trigger_item : triggers_items) {
                    if (equipment != null && equipment.contains(trigger_item.item_id)) {
                        equipped = true;
                        break;
                    }
                }
                if (!equipped) continue;
            }

            // Menu option check.
            if (trigger_graphic.menu_option != null && (menu_entries.stream().noneMatch(entry -> entry[1].equals(trigger_graphic.menu_option)))) continue;

            // Valid trigger, modify charges.
            if (trigger_graphic.decrease_charges) {
                decreaseCharges(trigger_graphic.charges);
            } else {
                increaseCharges(trigger_graphic.charges);
            }
        }
    }

    public void onHitsplatApplied(final HitsplatApplied event) {
        if (triggers_hitsplats == null) return;

        // Check all hitsplat triggers.
        for (final TriggerHitsplat trigger_hitsplat : triggers_hitsplats) {
            // Player check.
            if (trigger_hitsplat.self && event.getActor() != client.getLocalPlayer()) continue;

            // Enemy check.
            if (!trigger_hitsplat.self && (event.getActor() == client.getLocalPlayer() || event.getHitsplat().isOthers())) continue;

            // Hitsplat type check.
            if (trigger_hitsplat.hitsplat_id != event.getHitsplat().getHitsplatType()) continue;

            // Equipped check.
            if (trigger_hitsplat.equipped && triggers_items != null && equipment != null) {
                boolean equipped = false;
                for (final TriggerItem trigger_item : triggers_items) {
                    if (equipment.contains(trigger_item.item_id)) {
                        equipped = true;
                        break;
                    }
                }
                if (!equipped) continue;
            }

            // Animation check.
            if (trigger_hitsplat.animations != null) {
                boolean valid = false;
                for (final int animation : trigger_hitsplat.animations) {
                    if (animation == this.animation) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) continue;
            }

            // Valid hitsplat, modify charges.
            decreaseCharges(trigger_hitsplat.discharges);
        }
    }


    public void onMenuOptionClicked(final MenuOptionClicked event) {
        final String menu_target = event.getMenuTarget().replaceAll("</?col.*?>", "");
        final String menu_option = event.getMenuOption();

        if (
            // Not menu.
            menu_target.length() == 0 ||
            // Item not in inventory nor equipment.
            !in_inventory && !in_equipment ||
            // Menu option not found.
            menu_option == null || menu_option.length() == 0
        ) {
            return;
        }

        // Gametick changed, clear previous menu entries since they are no longer valid.
        if (gametick > gametick_before + 1) {
            gametick = 0; gametick_before = 0;
            menu_entries.clear();
        }

        // Save menu option and target for other triggers to use.
        menu_entries.add(new String[]{menu_target, menu_option});

        // No menu option triggers.
        if (triggers_menu_options == null) return;

        for (final TriggerMenuOption trigger_menu_option : triggers_menu_options) {
            if (!trigger_menu_option.option.equals(menu_option)) continue;

            // Fixed charges.
            if (trigger_menu_option.fixed_charges != null) {
                setCharges(trigger_menu_option.fixed_charges);
                break;
            }
        }
    }

    public void resetCharges() {
        if (triggers_resets == null) return;

        // Send message about item charges being reset if player has it on them.
        if (in_equipment || in_inventory) {
            client_thread.invokeLater(() -> {
                chat_messages.queue(QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage("<colHIGHLIGHT>" + items.getItemComposition(item_id).getName() + " daily charges have been reset.")
                    .build()
                );
            });
        }

        // Check for item resets.
        for (final TriggerReset trigger_reset : triggers_resets) {
            // Same item variations have different amount of charges.
            if (trigger_reset.item_id != null) {
                if (item_id == trigger_reset.item_id) {
                    setCharges(trigger_reset.charges);
                }

            // All variants of the item reset to the same amount of charges.
            } else {
                setCharges(trigger_reset.charges);
            }
        }
    }

    private void loadChargesFromConfig() {
        if (config_key == null) return;
        try {
            charges = Integer.parseInt(configs.getConfiguration(ChargesImprovedConfig.group, config_key));
        } catch (final Exception ignored) {}

    }

    public void setCharges(final int charges) {
        this.charges = charges;
        onChargesUpdated();

        if (config_key != null) {
            setConfiguration(config_key, charges);
        }
    }

    private void decreaseCharges(final int charges) {
        if (this.charges - charges < 0) return;
        setCharges(this.charges - charges);
    }

    public void increaseCharges(final int charges) {
        if (this.charges < 0) return;
        setCharges(this.charges + charges);
    }

    private void setConfiguration(final String key, @Nonnull final String value) {
        configs.setConfiguration(ChargesImprovedConfig.group, key, value);
    }

    private void setConfiguration(final String key, final int value) {
        configs.setConfiguration(ChargesImprovedConfig.group, key, value);
    }

    private void updateInfobox(final int item_id) {
        // Item id.
        this.item_id = item_id;

        // Tooltip.
        updateTooltip();

        // Image.
        setImage(items.getImage(item_id));
        infoboxes.updateInfoBoxImage(this);
    }

    private void updateTooltip() {
        tooltip = items.getItemComposition(item_id).getName() + (needs_to_be_equipped_for_infobox && !in_equipment ? " - Needs to be equipped" : "");
    }

    protected void onChargesUpdated() {}

    public void onGameTick(final GameTick ignored) {
        gametick++;
    }

    private int itemsDifference(final Item[] items_before, final Item[] items_after) {
        final int items_before_count = (int) Arrays.stream(items_before).filter(item -> item.getId() != -1).count();
        final int items_after_count = (int) Arrays.stream(items_after).filter(item -> item.getId() != -1).count();

        return Math.abs(items_before_count - items_after_count);

    }
}


