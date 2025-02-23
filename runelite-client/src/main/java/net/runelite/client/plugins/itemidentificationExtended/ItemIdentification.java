/*
 * Copyright (c) 2019, Hydrox6 <ikada@protonmail.ch>
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
package net.runelite.client.plugins.itemidentificationExtended;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.runelite.api.ItemID;

enum ItemIdentification
{
	//Herbs
	GRIMY_GUAM("(g)G", ItemID.GRIMY_GUAM_LEAF),
	GRIMY_MARRENTILL("(g)M", ItemID.GRIMY_MARRENTILL),
	GRIMY_TARROMIN("(g)TAR", ItemID.GRIMY_TARROMIN),
	GRIMY_HARRALANDER("(g)H", ItemID.GRIMY_HARRALANDER),
	GRIMY_RANARR("(g)R", ItemID.GRIMY_RANARR_WEED),
	GRIMY_TOADFLAX("(g)TOA", ItemID.GRIMY_TOADFLAX),
	GRIMY_IRIT("(g)I", ItemID.GRIMY_IRIT_LEAF),
	GRIMY_AVANTOE("(g)A", ItemID.GRIMY_AVANTOE),
	GRIMY_KWUARM("(g)K", ItemID.GRIMY_KWUARM),
	GRIMY_SNAPDRAGON("(g)S", ItemID.GRIMY_SNAPDRAGON),
	GRIMY_CADANTINE("(g)C", ItemID.GRIMY_CADANTINE),
	GRIMY_LANTADYME("(g)L", ItemID.GRIMY_LANTADYME),
	GRIMY_DWARF_WEED("(g)D", ItemID.GRIMY_DWARF_WEED),
	GRIMY_TORSTOL("(g)TOR", ItemID.GRIMY_TORSTOL),

	CLEAN_GUAM("G", ItemID.GUAM_LEAF),
	CLEAN_MARRENTILL("M", ItemID.MARRENTILL),
	CLEAN_TARROMIN("TAR", ItemID.TARROMIN),
	CLEAN_HARRALANDER("H", ItemID.HARRALANDER),
	CLEAN_RANARR("R", ItemID.RANARR_WEED),
	CLEAN_TOADFLAX( "TOA", ItemID.TOADFLAX),
	CLEAN_IRIT("I", ItemID.IRIT_LEAF),
	CLEAN_AVANTOE("A", ItemID.AVANTOE),
	CLEAN_KWUARM("K", ItemID.KWUARM),
	CLEAN_SNAPDRAGON("S", ItemID.SNAPDRAGON),
	CLEAN_CADANTINE("C", ItemID.CADANTINE),
	CLEAN_LANTADYME("L", ItemID.LANTADYME),
	CLEAN_DWARF_WEED( "D", ItemID.DWARF_WEED),
	CLEAN_TORSTOL("TOR", ItemID.TORSTOL),

	//LEO Random event
	COFFIN_CRAFT("Craft", ItemID.COFFIN),
	COFFIN_MINE("Mine", ItemID.COFFIN_7588),
	COFFIN_CHEF("Chef", ItemID.COFFIN_7589),
	COFFIN_FARM("Farm", ItemID.COFFIN_7590),
	COFFIN_WC("WC", ItemID.COFFIN_7591);

	final String name;
	final int[] itemIDs;

	ItemIdentification(String name, int... ids)
	{
		this.name = name;
		this.itemIDs = ids;
	}

	private static final Map<Integer, ItemIdentification> itemIdentifications;

	static
	{
		ImmutableMap.Builder<Integer, ItemIdentification> builder = new ImmutableMap.Builder<>();

		for (ItemIdentification i : values())
		{
			for (int id : i.itemIDs)
			{
				builder.put(id, i);
			}
		}

		itemIdentifications = builder.build();
	}

	static ItemIdentification get(int id)
	{
		return itemIdentifications.get(id);
	}
}
