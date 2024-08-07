/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.api.widgets;

/**
 * Represents a group-child {@link Widget} relationship.
 * <p>
 * For getting a specific widget from the client, see {@link net.runelite.api.Client#getWidget(WidgetInfo)}.
 */
@Deprecated
public enum WidgetInfo
{
	FAIRY_RING_TELEPORT_BUTTON(ComponentID.FAIRY_RING_TELEPORT_BUTTON),

	WORLD_SWITCHER_BUTTON(ComponentID.LOGOUT_PANEL_WORLD_SWITCHER_BUTTON),
	LOGOUT_BUTTON(ComponentID.LOGOUT_PANEL_LOGOUT_BUTTON),

	INVENTORY(WidgetID.INVENTORY_GROUP_ID, 0),
	FRIENDS_LIST(WidgetID.FRIENDS_LIST_GROUP_ID, 0),
	IGNORE_LIST(WidgetID.IGNORE_LIST_GROUP_ID, 0),
	FRIENDS_CHAT(WidgetID.FRIENDS_CHAT_GROUP_ID, 0),
	RAIDING_PARTY(WidgetID.RAIDING_PARTY_GROUP_ID, 0),

	WORLD_MAP_VIEW(ComponentID.WORLD_MAP_MAPVIEW),
	WORLD_MAP_OVERVIEW_MAP(ComponentID.WORLD_MAP_OVERVIEW_MAP),
	WORLD_MAP_BOTTOM_BAR(ComponentID.WORLD_MAP_BOTTOM_BAR),
	WORLD_MAP_SEARCH(ComponentID.WORLD_MAP_SEARCH),
	WORLD_MAP_SURFACE_SELECTOR(ComponentID.WORLD_MAP_SURFACE_SELECTOR),
	WORLD_MAP_TOOLTIP(ComponentID.WORLD_MAP_TOOLTIP),

	CLUE_SCROLL_TEXT(ComponentID.CLUESCROLL_TEXT),
	CLUE_SCROLL_REWARD_ITEM_CONTAINER(ComponentID.CLUESCROLL_REWARD_ITEM_CONTAINER),

	EQUIPMENT(WidgetID.EQUIPMENT_GROUP_ID, 0),
	EQUIPMENT_INVENTORY_ITEMS_CONTAINER(ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER),

	EMOTE_WINDOW(ComponentID.EMOTES_WINDOW),
	EMOTE_SCROLL_CONTAINER(ComponentID.EMOTES_EMOTE_SCROLL_CONTAINER),
	EMOTE_CONTAINER(ComponentID.EMOTES_EMOTE_CONTAINER),
	EMOTE_SCROLLBAR(ComponentID.EMOTES_EMOTE_SCROLLBAR),

	MUSIC_WINDOW(ComponentID.MUSIC_CONTAINER),
	MUSIC_TRACK_LIST(ComponentID.MUSIC_LIST),
	MUSIC_TRACK_SCROLL_CONTAINER(ComponentID.MUSIC_SCROLL_CONTAINER),
	MUSIC_TRACK_SCROLLBAR(ComponentID.MUSIC_SCROLLBAR),

	DIARY_QUEST_WIDGET_TITLE(ComponentID.DIARY_TITLE),
	DIARY_QUEST_WIDGET_TEXT(ComponentID.DIARY_TEXT),

	ACHIEVEMENT_DIARY_SCROLL_TITLE(ComponentID.ACHIEVEMENT_DIARY_SCROLL_TITLE),
	ACHIEVEMENT_DIARY_SCROLL_TEXT(ComponentID.ACHIEVEMENT_DIARY_SCROLL_TEXT),

	PEST_CONTROL_BOAT_INFO(WidgetID.PEST_CONTROL_BOAT_GROUP_ID, 2),
	PEST_CONTROL_KNIGHT_INFO_CONTAINER(ComponentID.PEST_CONTROL_KNIGHT_INFO_CONTAINER),
	PEST_CONTROL_ACTIVITY_SHIELD_INFO_CONTAINER(ComponentID.PEST_CONTROL_ACTIVITY_SHIELD_CONTAINER),
	PEST_CONTROL_PURPLE_SHIELD(ComponentID.PEST_CONTROL_PURPLE_SHIELD),
	PEST_CONTROL_BLUE_SHIELD(ComponentID.PEST_CONTROL_BLUE_SHIELD),
	PEST_CONTROL_YELLOW_SHIELD(ComponentID.PEST_CONTROL_YELLOW_SHIELD),
	PEST_CONTROL_RED_SHIELD(ComponentID.PEST_CONTROL_RED_SHIELD),
	PEST_CONTROL_PURPLE_HEALTH(ComponentID.PEST_CONTROL_PURPLE_HEALTH),
	PEST_CONTROL_BLUE_HEALTH(ComponentID.PEST_CONTROL_BLUE_HEALTH),
	PEST_CONTROL_YELLOW_HEALTH(ComponentID.PEST_CONTROL_YELLOW_HEALTH),
	PEST_CONTROL_RED_HEALTH(ComponentID.PEST_CONTROL_RED_HEALTH),
	PEST_CONTROL_PURPLE_ICON(ComponentID.PEST_CONTROL_PURPLE_ICON),
	PEST_CONTROL_BLUE_ICON(ComponentID.PEST_CONTROL_BLUE_ICON),
	PEST_CONTROL_YELLOW_ICON(ComponentID.PEST_CONTROL_YELLOW_ICON),
	PEST_CONTROL_RED_ICON(ComponentID.PEST_CONTROL_RED_ICON),
	PEST_CONTROL_ACTIVITY_BAR(ComponentID.PEST_CONTROL_ACTIVITY_BAR),
	PEST_CONTROL_ACTIVITY_PROGRESS(ComponentID.PEST_CONTROL_ACTIVITY_PROGRESS),

	VOLCANIC_MINE_TIME_LEFT(ComponentID.VOLCANIC_MINE_TIME_LEFT),
	VOLCANIC_MINE_POINTS(ComponentID.VOLCANIC_MINE_POINTS),
	VOLCANIC_MINE_STABILITY(ComponentID.VOLCANIC_MINE_STABILITY),
	VOLCANIC_MINE_PLAYER_COUNT(ComponentID.VOLCANIC_MINE_PLAYER_COUNT),
	VOLCANIC_MINE_VENTS_INFOBOX_GROUP(ComponentID.VOLCANIC_MINE_VENTS_INFOBOX_CONTAINER),
	VOLCANIC_MINE_STABILITY_INFOBOX_GROUP(ComponentID.VOLCANIC_MINE_STABILITY_INFOBOX_CONTAINER),
	VOLCANIC_MINE_VENT_A_PERCENTAGE(ComponentID.VOLCANIC_MINE_VENT_A_PERCENTAGE),
	VOLCANIC_MINE_VENT_B_PERCENTAGE(ComponentID.VOLCANIC_MINE_VENT_B_PERCENTAGE),
	VOLCANIC_MINE_VENT_C_PERCENTAGE(ComponentID.VOLCANIC_MINE_VENT_C_PERCENTAGE),
	VOLCANIC_MINE_VENT_A_STATUS(ComponentID.VOLCANIC_MINE_VENT_A_STATUS),
	VOLCANIC_MINE_VENT_B_STATUS(ComponentID.VOLCANIC_MINE_VENT_B_STATUS),
	VOLCANIC_MINE_VENT_C_STATUS(ComponentID.VOLCANIC_MINE_VENT_C_STATUS),

	FRIEND_CHAT_TITLE(ComponentID.FRIEND_LIST_TITLE),
	FRIEND_LIST_FULL_CONTAINER(ComponentID.FRIEND_LIST_FULL_CONTAINER),
	FRIEND_LIST_SORT_BY_NAME_BUTTON(ComponentID.FRIEND_LIST_SORT_BY_NAME_BUTTON),
	FRIEND_LIST_SORT_BY_LAST_WORLD_CHANGE_BUTTON(ComponentID.FRIEND_LIST_SORT_BY_LAST_WORLD_CHANGE_BUTTON),
	FRIEND_LIST_SORT_BY_WORLD_BUTTON(ComponentID.FRIEND_LIST_SORT_BY_WORLD_BUTTON),
	FRIEND_LIST_LEGACY_SORT_BUTTON(ComponentID.FRIEND_LIST_LEGACY_SORT_BUTTON),
	FRIEND_LIST_NAMES_CONTAINER(ComponentID.FRIEND_LIST_NAMES_CONTAINER),
	FRIEND_LIST_SCROLL_BAR(ComponentID.FRIEND_LIST_SCROLL_BAR),
	FRIEND_LIST_LOADING_TEXT(ComponentID.FRIEND_LIST_LOADING_TEXT),
	FRIEND_LIST_PREVIOUS_NAME_HOLDER(ComponentID.FRIEND_LIST_PREVIOUS_NAME_HOLDER),

	IGNORE_TITLE(ComponentID.IGNORE_LIST_TITLE),
	IGNORE_FULL_CONTAINER(ComponentID.IGNORE_LIST_FULL_CONTAINER),
	IGNORE_SORT_BY_NAME_BUTTON(ComponentID.IGNORE_LIST_SORT_BY_NAME_BUTTON),
	IGNORE_LEGACY_SORT_BUTTON(ComponentID.IGNORE_LIST_LEGACY_SORT_BUTTON),
	IGNORE_NAMES_CONTAINER(ComponentID.IGNORE_LIST_NAMES_CONTAINER),
	IGNORE_SCROLL_BAR(ComponentID.IGNORE_LIST_SCROLL_BAR),
	IGNORE_LOADING_TEXT(ComponentID.IGNORE_LIST_LOADING_TEXT),
	IGNORE_PREVIOUS_NAME_HOLDER(ComponentID.IGNORE_LIST_PREVIOUS_NAME_HOLDER),

	EXPLORERS_RING_ALCH_INVENTORY(ComponentID.EXPLORERS_RING_INVENTORY),

	FRIENDS_CHAT_ROOT(ComponentID.FRIENDS_CHAT_ROOT),
	FRIENDS_CHAT_TITLE(ComponentID.FRIENDS_CHAT_TITLE),
	FRIENDS_CHAT_OWNER(ComponentID.FRIENDS_CHAT_OWNER),
	FRIENDS_CHAT_LIST(ComponentID.FRIENDS_CHAT_LIST),

	BANK_CONTAINER(ComponentID.BANK_CONTAINER),
	BANK_SEARCH_BUTTON_BACKGROUND(ComponentID.BANK_SEARCH_BUTTON_BACKGROUND),
	BANK_ITEM_CONTAINER(ComponentID.BANK_ITEM_CONTAINER),
	BANK_INVENTORY_ITEMS_CONTAINER(ComponentID.BANK_INVENTORY_ITEM_CONTAINER),
	BANK_EQUIPMENT_INVENTORY_ITEMS_CONTAINER(ComponentID.BANK_INVENTORY_EQUIPMENT_ITEM_CONTAINER),
	BANK_TITLE_BAR(ComponentID.BANK_TITLE_BAR),
	BANK_INCINERATOR(ComponentID.BANK_INCINERATOR),
	BANK_INCINERATOR_CONFIRM(ComponentID.BANK_INCINERATOR_CONFIRM),
	BANK_CONTENT_CONTAINER(ComponentID.BANK_CONTENT_CONTAINER),
	BANK_DEPOSIT_EQUIPMENT(ComponentID.BANK_DEPOSIT_EQUIPMENT),
	BANK_DEPOSIT_INVENTORY(ComponentID.BANK_DEPOSIT_INVENTORY),
	BANK_TAB_CONTAINER(ComponentID.BANK_TAB_CONTAINER),
	BANK_EQUIPMENT_CONTAINER(ComponentID.BANK_EQUIPMENT_PARENT),
	BANK_EQUIPMENT_BUTTON(ComponentID.BANK_EQUIPMENT_BUTTON),
	BANK_POPUP(ComponentID.BANK_POPUP),
	BANK_ITEM_COUNT_TOP(ComponentID.BANK_ITEM_COUNT_TOP),
	BANK_ITEM_COUNT_BAR(ComponentID.BANK_ITEM_COUNT_BAR),
	BANK_ITEM_COUNT_BOTTOM(ComponentID.BANK_ITEM_COUNT_BOTTOM),
	BANK_GROUP_STORAGE_BUTTON(ComponentID.BANK_GROUP_STORAGE_BUTTON),
	BANK_SCROLLBAR(ComponentID.BANK_SCROLLBAR),
	BANK_PIN_CONTAINER(ComponentID.BANK_PIN_CONTAINER),
	BANK_SETTINGS_BUTTON(ComponentID.BANK_SETTINGS_BUTTON),
	BANK_TUTORIAL_BUTTON(ComponentID.BANK_TUTORIAL_BUTTON),

	GROUP_STORAGE_UI(ComponentID.GROUP_STORAGE_UI),
	GROUP_STORAGE_ITEM_CONTAINER(ComponentID.GROUP_STORAGE_ITEM_CONTAINER),

	GRAND_EXCHANGE_WINDOW_CONTAINER(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER),
	GRAND_EXCHANGE_OFFER_CONTAINER(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER),
	GRAND_EXCHANGE_OFFER_TEXT(ComponentID.GRAND_EXCHANGE_OFFER_DESCRIPTION),

	GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER(ComponentID.GRAND_EXCHANGE_INVENTORY_INVENTORY_ITEM_CONTAINER),

	DEPOSIT_BOX_INVENTORY_ITEMS_CONTAINER(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER),

	SHOP_INVENTORY_ITEMS_CONTAINER(ComponentID.SHOP_INVENTORY_ITEM_CONTAINER),

	SMITHING_INVENTORY_ITEMS_CONTAINER(ComponentID.SMITHING_INVENTORY_ITEM_CONTAINER),

	GUIDE_PRICES_ITEMS_CONTAINER(ComponentID.GUIDE_PRICES_ITEM_CONTAINER),
	GUIDE_PRICES_INVENTORY_ITEMS_CONTAINER(ComponentID.GUIDE_PRICES_INVENTORY_ITEM_CONTAINER),

	RUNE_POUCH_ITEM_CONTAINER(WidgetID.RUNE_POUCH_GROUP_ID, 0),

	MINIMAP_ORBS(WidgetID.MINIMAP_GROUP_ID, 0),
	MINIMAP_XP_ORB(ComponentID.MINIMAP_XP_ORB),
	MINIMAP_PRAYER_ORB(ComponentID.MINIMAP_PRAYER_ORB),
	MINIMAP_QUICK_PRAYER_ORB(ComponentID.MINIMAP_QUICK_PRAYER_ORB),
	MINIMAP_PRAYER_ORB_TEXT(ComponentID.MINIMAP_PRAYER_ORB_TEXT),
	MINIMAP_RUN_ORB(ComponentID.MINIMAP_RUN_ORB),
	MINIMAP_TOGGLE_RUN_ORB(ComponentID.MINIMAP_TOGGLE_RUN_ORB),
	MINIMAP_RUN_ORB_TEXT(ComponentID.MINIMAP_RUN_ORB_TEXT),
	MINIMAP_HEALTH_ORB(ComponentID.MINIMAP_HEALTH_ORB),
	MINIMAP_SPEC_ORB(ComponentID.MINIMAP_SPEC_ORB),
	MINIMAP_WORLDMAP_ORB(ComponentID.MINIMAP_WORLDMAP_ORB),
	MINIMAP_WIKI_BANNER_PARENT(ComponentID.MINIMAP_WIKI_BANNER_PARENT),
	MINIMAP_WIKI_BANNER(ComponentID.MINIMAP_WIKI_BANNER),
	MINIMAP_WORLDMAP_OPTIONS(ComponentID.MINIMAP_WORLDMAP_OPTIONS),

	LMS_INFO(ComponentID.LMS_INFO),
	LMS_KDA(ComponentID.LMS_INGAME_INFO),

	LOGIN_CLICK_TO_PLAY_SCREEN(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, 0),
	LOGIN_CLICK_TO_PLAY_SCREEN_MESSAGE_OF_THE_DAY(ComponentID.LOGIN_CLICK_TO_PLAY_SCREEN_MESSAGE_OF_THE_DAY),

	FIXED_VIEWPORT(ComponentID.FIXED_VIEWPORT_FIXED_VIEWPORT),
	FIXED_VIEWPORT_ROOT_INTERFACE_CONTAINER(ComponentID.FIXED_VIEWPORT_ROOT_INTERFACE_CONTAINER),
	FIXED_VIEWPORT_BANK_CONTAINER(ComponentID.FIXED_VIEWPORT_BANK_CONTAINER),
	FIXED_VIEWPORT_INTERFACE_CONTAINER(ComponentID.FIXED_VIEWPORT_INTERFACE_CONTAINER),
	FIXED_VIEWPORT_INVENTORY_CONTAINER(ComponentID.FIXED_VIEWPORT_INVENTORY_CONTAINER),
	FIXED_VIEWPORT_COMBAT_TAB(ComponentID.FIXED_VIEWPORT_COMBAT_TAB),
	FIXED_VIEWPORT_STATS_TAB(ComponentID.FIXED_VIEWPORT_STATS_TAB),
	FIXED_VIEWPORT_QUESTS_TAB(ComponentID.FIXED_VIEWPORT_QUESTS_TAB),
	FIXED_VIEWPORT_INVENTORY_TAB(ComponentID.FIXED_VIEWPORT_INVENTORY_TAB),
	FIXED_VIEWPORT_EQUIPMENT_TAB(ComponentID.FIXED_VIEWPORT_EQUIPMENT_TAB),
	FIXED_VIEWPORT_PRAYER_TAB(ComponentID.FIXED_VIEWPORT_PRAYER_TAB),
	FIXED_VIEWPORT_MAGIC_TAB(ComponentID.FIXED_VIEWPORT_MAGIC_TAB),
	FIXED_VIEWPORT_FRIENDS_CHAT_TAB(ComponentID.FIXED_VIEWPORT_FRIENDS_CHAT_TAB),
	FIXED_VIEWPORT_FRIENDS_TAB(ComponentID.FIXED_VIEWPORT_FRIENDS_TAB),
	FIXED_VIEWPORT_IGNORES_TAB(ComponentID.FIXED_VIEWPORT_IGNORES_TAB),
	FIXED_VIEWPORT_LOGOUT_TAB(ComponentID.FIXED_VIEWPORT_LOGOUT_TAB),
	FIXED_VIEWPORT_OPTIONS_TAB(ComponentID.FIXED_VIEWPORT_OPTIONS_TAB),
	FIXED_VIEWPORT_EMOTES_TAB(ComponentID.FIXED_VIEWPORT_EMOTES_TAB),
	FIXED_VIEWPORT_MUSIC_TAB(ComponentID.FIXED_VIEWPORT_MUSIC_TAB),
	FIXED_VIEWPORT_COMBAT_ICON(ComponentID.FIXED_VIEWPORT_COMBAT_ICON),
	FIXED_VIEWPORT_STATS_ICON(ComponentID.FIXED_VIEWPORT_STATS_ICON),
	FIXED_VIEWPORT_QUESTS_ICON(ComponentID.FIXED_VIEWPORT_QUESTS_ICON),
	FIXED_VIEWPORT_INVENTORY_ICON(ComponentID.FIXED_VIEWPORT_INVENTORY_ICON),
	FIXED_VIEWPORT_EQUIPMENT_ICON(ComponentID.FIXED_VIEWPORT_EQUIPMENT_ICON),
	FIXED_VIEWPORT_PRAYER_ICON(ComponentID.FIXED_VIEWPORT_PRAYER_ICON),
	FIXED_VIEWPORT_MAGIC_ICON(ComponentID.FIXED_VIEWPORT_MAGIC_ICON),
	FIXED_VIEWPORT_FRIENDS_CHAT_ICON(ComponentID.FIXED_VIEWPORT_FRIENDS_CHAT_ICON),
	FIXED_VIEWPORT_FRIENDS_ICON(ComponentID.FIXED_VIEWPORT_FRIENDS_ICON),
	FIXED_VIEWPORT_IGNORES_ICON(ComponentID.FIXED_VIEWPORT_IGNORES_ICON),
	FIXED_VIEWPORT_LOGOUT_ICON(ComponentID.FIXED_VIEWPORT_LOGOUT_ICON),
	FIXED_VIEWPORT_OPTIONS_ICON(ComponentID.FIXED_VIEWPORT_OPTIONS_ICON),
	FIXED_VIEWPORT_EMOTES_ICON(ComponentID.FIXED_VIEWPORT_EMOTES_ICON),
	FIXED_VIEWPORT_MUSIC_ICON(ComponentID.FIXED_VIEWPORT_MUSIC_ICON),
	FIXED_VIEWPORT_MINIMAP(ComponentID.FIXED_VIEWPORT_MINIMAP),
	FIXED_VIEWPORT_MINIMAP_DRAW_AREA(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA),

	RESIZABLE_MINIMAP_STONES_WIDGET(ComponentID.RESIZABLE_VIEWPORT_MINIMAP),
	RESIZABLE_MINIMAP_STONES_DRAW_AREA(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA),
	RESIZABLE_MINIMAP_STONES_ORB_HOLDER(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_ORB_HOLDER),
	RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX(ComponentID.RESIZABLE_VIEWPORT_RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX),
	RESIZABLE_VIEWPORT_COMBAT_TAB(ComponentID.RESIZABLE_VIEWPORT_COMBAT_TAB),
	RESIZABLE_VIEWPORT_STATS_TAB(ComponentID.RESIZABLE_VIEWPORT_STATS_TAB),
	RESIZABLE_VIEWPORT_QUESTS_TAB(ComponentID.RESIZABLE_VIEWPORT_QUESTS_TAB),
	RESIZABLE_VIEWPORT_INVENTORY_TAB(ComponentID.RESIZABLE_VIEWPORT_INVENTORY_TAB),
	RESIZABLE_VIEWPORT_EQUIPMENT_TAB(ComponentID.RESIZABLE_VIEWPORT_EQUIPMENT_TAB),
	RESIZABLE_VIEWPORT_PRAYER_TAB(ComponentID.RESIZABLE_VIEWPORT_PRAYER_TAB),
	RESIZABLE_VIEWPORT_MAGIC_TAB(ComponentID.RESIZABLE_VIEWPORT_MAGIC_TAB),
	RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB(ComponentID.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB),
	RESIZABLE_VIEWPORT_FRIENDS_TAB(ComponentID.RESIZABLE_VIEWPORT_FRIENDS_TAB),
	RESIZABLE_VIEWPORT_IGNORES_TAB(ComponentID.RESIZABLE_VIEWPORT_IGNORES_TAB),
	RESIZABLE_VIEWPORT_LOGOUT_TAB(ComponentID.RESIZABLE_VIEWPORT_LOGOUT_TAB),
	RESIZABLE_VIEWPORT_OPTIONS_TAB(ComponentID.RESIZABLE_VIEWPORT_OPTIONS_TAB),
	RESIZABLE_VIEWPORT_EMOTES_TAB(ComponentID.RESIZABLE_VIEWPORT_EMOTES_TAB),
	RESIZABLE_VIEWPORT_MUSIC_TAB(ComponentID.RESIZABLE_VIEWPORT_MUSIC_TAB),
	RESIZABLE_VIEWPORT_COMBAT_ICON(ComponentID.RESIZABLE_VIEWPORT_COMBAT_ICON),
	RESIZABLE_VIEWPORT_STATS_ICON(ComponentID.RESIZABLE_VIEWPORT_STATS_ICON),
	RESIZABLE_VIEWPORT_QUESTS_ICON(ComponentID.RESIZABLE_VIEWPORT_QUESTS_ICON),
	RESIZABLE_VIEWPORT_INVENTORY_ICON(ComponentID.RESIZABLE_VIEWPORT_INVENTORY_ICON),
	RESIZABLE_VIEWPORT_EQUIPMENT_ICON(ComponentID.RESIZABLE_VIEWPORT_EQUIPMENT_ICON),
	RESIZABLE_VIEWPORT_PRAYER_ICON(ComponentID.RESIZABLE_VIEWPORT_PRAYER_ICON),
	RESIZABLE_VIEWPORT_MAGIC_ICON(ComponentID.RESIZABLE_VIEWPORT_MAGIC_ICON),
	RESIZABLE_VIEWPORT_FRIENDS_CHAT_ICON(ComponentID.RESIZABLE_VIEWPORT_FRIENDS_CHAT_ICON),
	RESIZABLE_VIEWPORT_FRIENDS_ICON(ComponentID.RESIZABLE_VIEWPORT_FRIENDS_ICON),
	RESIZABLE_VIEWPORT_IGNORES_ICON(ComponentID.RESIZABLE_VIEWPORT_IGNORES_ICON),
	RESIZABLE_VIEWPORT_LOGOUT_ICON(ComponentID.RESIZABLE_VIEWPORT_LOGOUT_ICON),
	RESIZABLE_VIEWPORT_OPTIONS_ICON(ComponentID.RESIZABLE_VIEWPORT_OPTIONS_ICON),
	RESIZABLE_VIEWPORT_EMOTES_ICON(ComponentID.RESIZABLE_VIEWPORT_EMOTES_ICON),
	RESIZABLE_VIEWPORT_MUSIC_ICON(ComponentID.RESIZABLE_VIEWPORT_MUSIC_ICON),
	RESIZABLE_VIEWPORT_INTERFACE_CONTAINER(ComponentID.RESIZABLE_VIEWPORT_INTERFACE_CONTAINER),
	RESIZABLE_VIEWPORT_INVENTORY_CONTAINER(ComponentID.RESIZABLE_VIEWPORT_INVENTORY_CONTAINER),
	RESIZABLE_VIEWPORT_CHATBOX_PARENT(ComponentID.RESIZABLE_VIEWPORT_CHATBOX_PARENT),
	RESIZABLE_VIEWPORT_INVENTORY_PARENT(ComponentID.RESIZABLE_VIEWPORT_INVENTORY_PARENT),

	RESIZABLE_MINIMAP_WIDGET(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP),
	RESIZABLE_MINIMAP_DRAW_AREA(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA),
	RESIZABLE_MINIMAP_ORB_HOLDER(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_ORB_HOLDER),
	RESIZABLE_MINIMAP_LOGOUT_BUTTON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_LOGOUT_BUTTON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_RESIZABLE_VIEWPORT_BOTTOM_LINE),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_LOGOUT_BUTTON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_LOGOUT_BUTTON_OVERLAY),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_QUESTS_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_QUESTS_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_PRAYER_TAB(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_PRAYER_TAB),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_PRAYER_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_PRAYER_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_EQUIPMENT_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_EQUIP_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_COMBAT_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_CMB_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_STATS_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_SKILLS_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_MAGIC_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MAGIC_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_FC_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_OPTIONS_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_SETTINGS_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_EMOTES_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_EMOTE_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_MUSIC_ICON(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MUSIC_ICON),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_CONTAINER(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_CONTAINER),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_INTERFACE_CONTAINER(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INTERFACE_CONTAINER),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_CHATBOX_PARENT(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_CHATBOX_PARENT),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS1(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS1),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS2(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS2),
	RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_PARENT(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_PARENT),

	COMBAT_LEVEL(ComponentID.COMBAT_LEVEL),
	COMBAT_STYLE_ONE(ComponentID.COMBAT_STYLE_ONE),
	COMBAT_STYLE_TWO(ComponentID.COMBAT_STYLE_TWO),
	COMBAT_STYLE_THREE(ComponentID.COMBAT_STYLE_THREE),
	COMBAT_STYLE_FOUR(ComponentID.COMBAT_STYLE_FOUR),
	COMBAT_SPELLS(ComponentID.COMBAT_SPELLS),
	COMBAT_DEFENSIVE_SPELL_BOX(ComponentID.COMBAT_DEFENSIVE_SPELL_BOX),
	COMBAT_DEFENSIVE_SPELL_ICON(ComponentID.COMBAT_DEFENSIVE_SPELL_ICON),
	COMBAT_DEFENSIVE_SPELL_SHIELD(ComponentID.COMBAT_DEFENSIVE_SPELL_SHIELD),
	COMBAT_DEFENSIVE_SPELL_TEXT(ComponentID.COMBAT_DEFENSIVE_SPELL_TEXT),
	COMBAT_SPELL_BOX(ComponentID.COMBAT_SPELL_BOX),
	COMBAT_SPELL_ICON(ComponentID.COMBAT_SPELL_ICON),
	COMBAT_SPELL_TEXT(ComponentID.COMBAT_SPELL_TEXT),
	COMBAT_AUTO_RETALIATE(ComponentID.COMBAT_AUTO_RETALIATE),

	DIALOG_OPTION(WidgetID.DIALOG_OPTION_GROUP_ID, 0),
	DIALOG_OPTION_OPTIONS(ComponentID.DIALOG_OPTION_OPTIONS),

	DIALOG_SPRITE(WidgetID.DIALOG_SPRITE_GROUP_ID, 0),
	DIALOG_SPRITE_SPRITE(ComponentID.DIALOG_SPRITE_SPRITE),
	DIALOG_SPRITE_TEXT(ComponentID.DIALOG_SPRITE_TEXT),
	DIALOG_DOUBLE_SPRITE_TEXT(ComponentID.DIALOG_DOUBLE_SPRITE_TEXT),
	DIALOG_DOUBLE_SPRITE_SPRITE1(ComponentID.DIALOG_DOUBLE_SPRITE_SPRITE1),
	DIALOG_DOUBLE_SPRITE_SPRITE2(ComponentID.DIALOG_DOUBLE_SPRITE_SPRITE2),

	DIALOG_NPC_NAME(ComponentID.DIALOG_NPC_NAME),
	DIALOG_NPC_TEXT(ComponentID.DIALOG_NPC_TEXT),
	DIALOG_NPC_HEAD_MODEL(ComponentID.DIALOG_NPC_HEAD_MODEL),

	DIALOG_PLAYER(WidgetID.DIALOG_PLAYER_GROUP_ID, 0),
	DIALOG_PLAYER_TEXT(ComponentID.DIALOG_PLAYER_TEXT),

	PRIVATE_CHAT_MESSAGE(WidgetID.PRIVATE_CHAT, 0),

	SLAYER_REWARDS_TOPBAR(ComponentID.SLAYER_REWARDS_TOP_BAR),

	CHATBOX_PARENT(ComponentID.CHATBOX_PARENT),
	CHATBOX(ComponentID.CHATBOX_FRAME),
	CHATBOX_MESSAGES(ComponentID.CHATBOX_MESSAGES),
	CHATBOX_BUTTONS(ComponentID.CHATBOX_BUTTONS),
	CHATBOX_TITLE(ComponentID.CHATBOX_TITLE),
	CHATBOX_FULL_INPUT(ComponentID.CHATBOX_FULL_INPUT),
	CHATBOX_GE_SEARCH_RESULTS(ComponentID.CHATBOX_GE_SEARCH_RESULTS),
	CHATBOX_CONTAINER(ComponentID.CHATBOX_CONTAINER),
	CHATBOX_REPORT_TEXT(ComponentID.CHATBOX_REPORT_TEXT),
	CHATBOX_INPUT(ComponentID.CHATBOX_INPUT),
	CHATBOX_TRANSPARENT_BACKGROUND(ComponentID.CHATBOX_TRANSPARENT_BACKGROUND),
	CHATBOX_TRANSPARENT_LINES(ComponentID.CHATBOX_TRANSPARENT_BACKGROUND_LINES),
	CHATBOX_MESSAGE_LINES(ComponentID.CHATBOX_MESSAGE_LINES),
	CHATBOX_FIRST_MESSAGE(ComponentID.CHATBOX_FIRST_MESSAGE),
	CHATBOX_TAB_ALL(ComponentID.CHATBOX_TAB_ALL),
	CHATBOX_TAB_GAME(ComponentID.CHATBOX_TAB_GAME),
	CHATBOX_TAB_PUBLIC(ComponentID.CHATBOX_TAB_PUBLIC),
	CHATBOX_TAB_PRIVATE(ComponentID.CHATBOX_TAB_PRIVATE),
	CHATBOX_TAB_CHANNEL(ComponentID.CHATBOX_TAB_CHANNEL),
	CHATBOX_TAB_CLAN(ComponentID.CHATBOX_TAB_CLAN),
	CHATBOX_TAB_TRADE(ComponentID.CHATBOX_TAB_TRADE),

	BA_TEAM(ComponentID.BA_TEAM_TEAM),

	BA_HEAL_ROLE_TEXT(ComponentID.BA_HEALER_ROLE),
	BA_HEAL_ROLE_SPRITE(ComponentID.BA_HEALER_ROLE_SPRITE),

	BA_HEAL_TEAMMATES(ComponentID.BA_HEALER_TEAMMATES),
	BA_HEAL_TEAMMATE1(ComponentID.BA_HEALER_TEAMMATE1),
	BA_HEAL_TEAMMATE2(ComponentID.BA_HEALER_TEAMMATE2),
	BA_HEAL_TEAMMATE3(ComponentID.BA_HEALER_TEAMMATE3),
	BA_HEAL_TEAMMATE4(ComponentID.BA_HEALER_TEAMMATE4),

	BA_COLL_ROLE_TEXT(ComponentID.BA_COLLECTOR_ROLE),
	BA_COLL_ROLE_SPRITE(ComponentID.BA_COLLECTOR_ROLE_SPRITE),

	BA_ATK_ROLE_TEXT(ComponentID.BA_ATTACKER_ROLE),
	BA_ATK_ROLE_SPRITE(ComponentID.BA_ATTACKER_ROLE_SPRITE),

	BA_DEF_ROLE_TEXT(ComponentID.BA_DEFENDER_ROLE),
	BA_DEF_ROLE_SPRITE(ComponentID.BA_DEFENDER_ROLE_SPRITE),

	BA_REWARD_TEXT(ComponentID.BA_REWARD_REWARD_TEXT),

	LEVEL_UP(WidgetID.LEVEL_UP_GROUP_ID, 0),
	LEVEL_UP_SKILL(ComponentID.LEVEL_UP_SKILL),
	LEVEL_UP_LEVEL(ComponentID.LEVEL_UP_LEVEL),

	QUEST_COMPLETED(WidgetID.QUEST_COMPLETED_GROUP_ID, 0),
	QUEST_COMPLETED_NAME_TEXT(ComponentID.QUEST_COMPLETED_NAME_TEXT),

	MOTHERLODE_MINE(WidgetID.MOTHERLODE_MINE_GROUP_ID, 0),

	GWD_KC(WidgetID.GWD_KC_GROUP_ID, 5),

	PUZZLE_BOX(ComponentID.PUZZLE_BOX_VISIBLE_BOX),

	LIGHT_BOX(ComponentID.LIGHT_BOX_LIGHT_BOX),
	LIGHT_BOX_CONTENTS(ComponentID.LIGHT_BOX_LIGHT_BULB_CONTAINER),
	LIGHT_BOX_BUTTON_A(ComponentID.LIGHT_BOX_BUTTON_A),
	LIGHT_BOX_BUTTON_B(ComponentID.LIGHT_BOX_BUTTON_B),
	LIGHT_BOX_BUTTON_C(ComponentID.LIGHT_BOX_BUTTON_C),
	LIGHT_BOX_BUTTON_D(ComponentID.LIGHT_BOX_BUTTON_D),
	LIGHT_BOX_BUTTON_E(ComponentID.LIGHT_BOX_BUTTON_E),
	LIGHT_BOX_BUTTON_F(ComponentID.LIGHT_BOX_BUTTON_F),
	LIGHT_BOX_BUTTON_G(ComponentID.LIGHT_BOX_BUTTON_G),
	LIGHT_BOX_BUTTON_H(ComponentID.LIGHT_BOX_BUTTON_H),

	LIGHT_BOX_WINDOW(ComponentID.LIGHT_BOX_LIGHT_BOX_WINDOW),

	NIGHTMARE_ZONE(WidgetID.NIGHTMARE_ZONE_GROUP_ID, 0),

	NIGHTMARE_PILLAR_HEALTH(WidgetID.NIGHTMARE_PILLAR_HEALTH_GROUP_ID, 1),

	RAIDS_POINTS_INFOBOX(ComponentID.RAIDS_POINTS_INFOBOX),

	RAIDS_PRIVATE_STORAGE_ITEM_CONTAINER(ComponentID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_ITEM_CONTAINER),

	TOB_PARTY_INTERFACE(ComponentID.TOB_PARTY_INTERFACE),
	TOB_PARTY_STATS(ComponentID.TOB_PARTY_STATS),
	TOB_HEALTH_BAR(ComponentID.TOB_HEALTHBAR_CONTAINER),

	BLAST_FURNACE_COFFER(WidgetID.BLAST_FURNACE_GROUP_ID, 2),

	PYRAMID_PLUNDER_DATA(WidgetID.PYRAMID_PLUNDER_GROUP_ID, 2),

	EXPERIENCE_TRACKER(WidgetID.EXPERIENCE_TRACKER_GROUP_ID, 0),
	EXPERIENCE_TRACKER_WIDGET(ComponentID.EXPERIENCE_TRACKER_WIDGET),
	EXPERIENCE_TRACKER_BOTTOM_BAR(ComponentID.EXPERIENCE_TRACKER_BOTTOM_BAR),

	FISHING_TRAWLER_CONTRIBUTION(WidgetID.FISHING_TRAWLER_GROUP_ID, 13),
	FISHING_TRAWLER_TIMER(WidgetID.FISHING_TRAWLER_GROUP_ID, 14),

	TITHE_FARM(WidgetID.TITHE_FARM_GROUP_ID, 2),

	BARROWS_BROTHERS(ComponentID.BARROWS_BROTHERS),
	BARROWS_POTENTIAL(ComponentID.BARROWS_POTENTIAL),
	BARROWS_PUZZLE_PARENT(ComponentID.BARROWS_PUZZLE_PARENT),
	BARROWS_PUZZLE_ANSWER1(ComponentID.BARROWS_PUZZLE_ANSWER1),
	BARROWS_PUZZLE_ANSWER1_CONTAINER(ComponentID.BARROWS_PUZZLE_ANSWER1_CONTAINER),
	BARROWS_PUZZLE_ANSWER2(ComponentID.BARROWS_PUZZLE_ANSWER2),
	BARROWS_PUZZLE_ANSWER2_CONTAINER(ComponentID.BARROWS_PUZZLE_ANSWER2_CONTAINER),
	BARROWS_PUZZLE_ANSWER3(ComponentID.BARROWS_PUZZLE_ANSWER3),
	BARROWS_PUZZLE_ANSWER3_CONTAINER(ComponentID.BARROWS_PUZZLE_ANSWER3_CONTAINER),
	BARROWS_FIRST_PUZZLE(ComponentID.BARROWS_PUZZLE_SEQUENCE_1),

	BLAST_MINE(WidgetID.BLAST_MINE_GROUP_ID, 2),

	FAIRY_RING(WidgetID.FAIRY_RING_GROUP_ID, 0),

	FAIRY_RING_HEADER(ComponentID.FAIRY_RING_PANEL_HEADER),
	FAIRY_RING_LIST(ComponentID.FAIRY_RING_PANEL_LIST),
	FAIRY_RING_FAVORITES(ComponentID.FAIRY_RING_PANEL_FAVORITES),
	FAIRY_RING_LIST_SEPARATOR(ComponentID.FAIRY_RING_PANEL_SEPARATOR),
	FAIRY_RING_LIST_SCROLLBAR(ComponentID.FAIRY_RING_PANEL_SCROLLBAR),

	DESTROY_ITEM(WidgetID.DESTROY_ITEM_GROUP_ID, 0),
	DESTROY_ITEM_NAME(ComponentID.DESTROY_ITEM_NAME),
	DESTROY_ITEM_YES(ComponentID.DESTROY_ITEM_YES),
	DESTROY_ITEM_NO(ComponentID.DESTROY_ITEM_NO),

	VARROCK_MUSEUM_QUESTION(ComponentID.VARROCK_MUSEUM_QUESTION),
	VARROCK_MUSEUM_FIRST_ANSWER(ComponentID.VARROCK_MUSEUM_FIRST_ANSWER),
	VARROCK_MUSEUM_SECOND_ANSWER(ComponentID.VARROCK_MUSEUM_SECOND_ANSWER),
	VARROCK_MUSEUM_THIRD_ANSWER(ComponentID.VARROCK_MUSEUM_THIRD_ANSWER),

	KILL_LOG_TITLE(ComponentID.KILL_LOG_TITLE),
	KILL_LOG_MONSTER(ComponentID.KILL_LOG_MONSTER),
	KILL_LOG_KILLS(ComponentID.KILL_LOG_KILLS),
	KILL_LOG_STREAK(ComponentID.KILL_LOG_STREAK),

	ADVENTURE_LOG(ComponentID.ADVENTURE_LOG_CONTAINER),

	COLLECTION_LOG(ComponentID.COLLECTION_LOG_CONTAINER),

	COLLECTION_LOG_TABS(ComponentID.COLLECTION_LOG_TABS),
	COLLECTION_LOG_ENTRY(ComponentID.COLLECTION_LOG_ENTRY),
	COLLECTION_LOG_ENTRY_HEADER(ComponentID.COLLECTION_LOG_ENTRY_HEADER),
	COLLECTION_LOG_ENTRY_ITEMS(ComponentID.COLLECTION_LOG_ENTRY_ITEMS),

	GENERIC_SCROLL_TEXT(ComponentID.GENERIC_SCROLL_TEXT),

	WORLD_SWITCHER_LIST(ComponentID.WORLD_SWITCHER_WORLD_LIST),

	FOSSIL_ISLAND_OXYGENBAR(WidgetID.FOSSIL_ISLAND_OXYGENBAR_ID, 1),

	SPELL_LUMBRIDGE_HOME_TELEPORT(ComponentID.SPELLBOOK_LUMBRIDGE_HOME_TELEPORT),
	SPELL_EDGEVILLE_HOME_TELEPORT(ComponentID.SPELLBOOK_EDGEVILLE_HOME_TELEPORT),
	SPELL_LUNAR_HOME_TELEPORT(ComponentID.SPELLBOOK_LUNAR_HOME_TELEPORT),
	SPELL_ARCEUUS_HOME_TELEPORT(ComponentID.SPELLBOOK_ARCEUUS_HOME_TELEPORT),
	SPELL_KOUREND_HOME_TELEPORT(ComponentID.SPELLBOOK_KOUREND_HOME_TELEPORT),
	SPELL_CATHERBY_HOME_TELEPORT(ComponentID.SPELLBOOK_CATHERBY_HOME_TELEPORT),
	SPELL_LUNAR_FERTILE_SOIL(ComponentID.SPELLBOOK_FERTILE_SOIL),

	PVP_WILDERNESS_SKULL_CONTAINER(ComponentID.PVP_WILDERNESS_SKULL_CONTAINER),
	PVP_SKULL_CONTAINER(ComponentID.PVP_SKULL_CONTAINER),
	PVP_WORLD_SAFE_ZONE(ComponentID.PVP_SAFE_ZONE),

	PVP_WILDERNESS_LEVEL(ComponentID.PVP_WILDERNESS_LEVEL),
	PVP_KILLDEATH_COUNTER(ComponentID.PVP_KILLDEATH_RATIO),

	ZEAH_MESS_HALL_COOKING_DISPLAY(ComponentID.ZEAH_MESS_HALL_COOKING_DISPLAY),

	LOOTING_BAG_CONTAINER(ComponentID.LOOTING_BAG_LOOTING_BAG_INVENTORY),

	SKOTIZO_CONTAINER(ComponentID.SKOTIZO_CONTAINER),

	CHARACTER_SUMMARY_CONTAINER(ComponentID.CHARACTER_SUMMARY_CONTAINER),

	QUESTLIST_BOX(ComponentID.QUEST_LIST_BOX),
	QUESTLIST_CONTAINER(ComponentID.QUEST_LIST_CONTAINER),

	SEED_VAULT_TITLE_CONTAINER(ComponentID.SEED_VAULT_TITLE_CONTAINER),
	SEED_VAULT_ITEM_CONTAINER(ComponentID.SEED_VAULT_ITEM_CONTAINER),
	SEED_VAULT_ITEM_TEXT(ComponentID.SEED_VAULT_ITEM_TEXT),
	SEED_VAULT_SEARCH_BUTTON(ComponentID.SEED_VAULT_SEARCH_BUTTON),
	SEED_VAULT_INVENTORY_ITEMS_CONTAINER(ComponentID.SEED_VAULT_INVENTORY_ITEM_CONTAINER),

	SETTINGS_SIDE_CAMERA_ZOOM_SLIDER_TRACK(ComponentID.SETTINGS_SIDE_CAMERA_ZOOM_SLIDER_TRACK),
	SETTINGS_SIDE_MUSIC_SLIDER(ComponentID.SETTINGS_SIDE_MUSIC_SLIDER),
	SETTINGS_SIDE_MUSIC_SLIDER_STEP_HOLDER(ComponentID.SETTINGS_SIDE_MUSIC_SLIDER_STEP_HOLDER),
	SETTINGS_SIDE_SOUND_EFFECT_SLIDER(ComponentID.SETTINGS_SIDE_SOUND_EFFECT_SLIDER),
	SETTINGS_SIDE_AREA_SOUND_SLIDER(ComponentID.SETTINGS_SIDE_AREA_SOUND_SLIDER),

	SETTINGS_INIT(ComponentID.SETTINGS_INIT),

	ACHIEVEMENT_DIARY_CONTAINER(ComponentID.ACHIEVEMENT_DIARY_CONTAINER),

	SKILLS_CONTAINER(ComponentID.SKILLS_CONTAINER),

	GAUNTLET_TIMER_CONTAINER(ComponentID.GAUNTLET_TIMER_CONTAINER),
	HALLOWED_SEPULCHRE_TIMER_CONTAINER(ComponentID.HALLOWED_SEPULCHRE_TIMER_CONTAINER),

	HEALTH_OVERLAY_BAR(ComponentID.HEALTH_HEALTHBAR_CONTAINER),
	HEALTH_OVERLAY_BAR_TEXT(ComponentID.HEALTH_HEALTHBAR_TEXT),

	TRAILBLAZER_AREA_TELEPORT(ComponentID.TRAILBLAZER_AREAS_TELEPORT),

	MULTICOMBAT_FIXED(ComponentID.FIXED_VIEWPORT_MULTICOMBAT_INDICATOR),
	MULTICOMBAT_RESIZABLE_MODERN(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MULTICOMBAT_INDICATOR),
	MULTICOMBAT_RESIZABLE_CLASSIC(ComponentID.RESIZABLE_VIEWPORT_MULTICOMBAT_INDICATOR),

	TEMPOROSS_STATUS_INDICATOR(ComponentID.TEMPOROSS_STATUS_INDICATOR),
	TEMPOROSS_LOBBY(ComponentID.TEMPOROSS_LOBBY_LOBBY),

	CLAN_LAYER(ComponentID.CLAN_LAYER),
	CLAN_HEADER(ComponentID.CLAN_HEADER),
	CLAN_MEMBER_LIST(ComponentID.CLAN_MEMBERS),

	CLAN_GUEST_LAYER(ComponentID.CLAN_GUEST_LAYER),
	CLAN_GUEST_HEADER(ComponentID.CLAN_GUEST_HEADER),
	CLAN_GUEST_MEMBER_LIST(ComponentID.CLAN_GUEST_MEMBERS),

	POH_TREASURE_CHEST_INVENTORY_CONTAINER(WidgetID.POH_TREASURE_CHEST_INVENTORY_GROUP_ID, 0),

	TRADE_WINDOW_HEADER(ComponentID.TRADE_HEADER),

	TOA_PARTY_LAYER(WidgetID.TOA_PARTY_GROUP_ID, 2),
	TOA_RAID_LAYER(WidgetID.TOA_RAID_GROUP_ID, 3),

	QUICK_PRAYER_PRAYERS(WidgetID.QUICK_PRAYERS_GROUP_ID, 4),

	GOTR_MAIN_DISPLAY(WidgetID.GOTR_GROUP_ID, 2),
	TROUBLE_BREWING_SCORE(ComponentID.TROUBLE_BREWING_SCORE),
	TROUBLE_BREWING_LOBBY(WidgetID.TROUBLE_BREWING_LOBBY_GROUP_ID, 2),
	MORTTON_TEMPLE_STATUS(WidgetID.MORTTON_TEMPLE_GROUP_ID, 2),
	BGR_RANK_DISPLAY_DRAUGHTS(WidgetID.BGR_RANK_DRAUGHTS_GROUP_ID, 2),
	BGR_RANK_DISPLAY_RUNELINK(WidgetID.BGR_RANK_RUNELINK_GROUP_ID, 2),
	BGR_RANK_DISPLAY_RUNESQUARES(WidgetID.BGR_RANK_RUNESQUARES_GROUP_ID, 2),
	BGR_RANK_DISPLAY_RUNEVERSI(WidgetID.BGR_RANK_RUNEVERSI_GROUP_ID, 2),
	AGILITY_ARENA_LIGHT_INDICATOR(WidgetID.AGILITY_ARENA_HUD_GROUP_ID, 2),
	GNOMEBALL_SCORE(WidgetID.GNOMEBALL_SCORE_GROUP_ID, 2),
	MTA_ALCHEMY_POINTS(WidgetID.MTA_ALCHEMY_GROUP_ID, 2),
	MTA_ENCHANT_POINTS(WidgetID.MTA_ENCHANT_GROUP_ID, 2),
	MTA_ENCHANT_BONUS(WidgetID.MTA_ENCHANT_GROUP_ID, 8),
	MTA_GRAVEYARD_POINTS(WidgetID.MTA_GRAVEYARD_GROUP_ID, 2),
	MTA_GRAVEYARD_VALUES(WidgetID.MTA_GRAVEYARD_GROUP_ID, 8),
	MTA_TELEKINETIC_POINTS(WidgetID.MTA_TELEKINETIC_GROUP_ID, 2),
	MTA_TELEKINETIC_SOLVED(WidgetID.MTA_TELEKINETIC_GROUP_ID, 8),

	STRANGLER_INFECTION_OVERLAY(WidgetID.THE_STRANGLER_INFECTION_GROUP_ID, 4),
	SANITY_OVERLAY(WidgetID.SANITY_GROUP_ID, 3),
	;

	private final int id;

	WidgetInfo(int id)
	{
		this.id = id;
	}

	WidgetInfo(int groupId, int childId)
	{
		this.id = (groupId << 16) | childId;
	}

	/**
	 * Gets the ID of the group-child pairing.
	 *
	 * @return the ID
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Gets the group ID of the pair.
	 *
	 * @return the group ID
	 */
	public int getGroupId()
	{
		return id >> 16;
	}

	/**
	 * Gets the ID of the child in the group.
	 *
	 * @return the child ID
	 */
	public int getChildId()
	{
		return id & 0xffff;
	}

	/**
	 * Gets the packed widget ID.
	 *
	 * @return the packed ID
	 */
	public int getPackedId()
	{
		return id;
	}

	/**
	 * Utility method that converts an ID returned by {@link #getId()} back
	 * to its group ID.
	 *
	 * @param id passed group-child ID
	 * @return the group ID
	 */
	public static int TO_GROUP(int id)
	{
		return id >>> 16;
	}

	/**
	 * Utility method that converts an ID returned by {@link #getId()} back
	 * to its child ID.
	 *
	 * @param id passed group-child ID
	 * @return the child ID
	 */
	public static int TO_CHILD(int id)
	{
		return id & 0xFFFF;
	}

	/**
	 * Packs the group and child IDs into a single integer.
	 *
	 * @param groupId the group ID
	 * @param childId the child ID
	 * @return the packed ID
	 */
	public static int PACK(int groupId, int childId)
	{
		return groupId << 16 | childId;
	}

}