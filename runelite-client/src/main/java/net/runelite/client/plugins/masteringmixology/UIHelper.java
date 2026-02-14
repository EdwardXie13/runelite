package net.runelite.client.plugins.masteringmixology;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.masteringmixology.constants.MixologyIDs;

import java.util.Optional;

@Singleton
public class UIHelper
{
	@Inject
	private Client client;

	public GameObject digweedNE;
	public GameObject digweedSE;
	public GameObject digweedSW;
	public GameObject digweedNW;

	public boolean isAgitatorSpeedupObjectPresent() {
		return isGraphicsObjectPresent(MixologyIDs.AGITATOR_SPEEDUP_OBJECT_ID);
	}

	public boolean isAlembicSpeedupObjectPresent() {
		return isGraphicsObjectPresent(MixologyIDs.ALEMBIC_SPEEDUP_OBJECT_ID);
	}

	public boolean isMatureDigweedPresent() {
		return getMatureDigweedObjectOrNull() != null;
	}

	public GameObject getMatureDigweedObjectOrNull() {
		Optional.ofNullable(digweedNE).map(GameObject::getId).orElse(null);
		var compositionNE = Optional.ofNullable(digweedNE).map(GameObject::getId).map(this::getObjectComposition).orElse(null);
		var compositionSE = Optional.ofNullable(digweedSE).map(GameObject::getId).map(this::getObjectComposition).orElse(null);
		var compositionSW = Optional.ofNullable(digweedSW).map(GameObject::getId).map(this::getObjectComposition).orElse(null);
		var compositionNW = Optional.ofNullable(digweedNW).map(GameObject::getId).map(this::getObjectComposition).orElse(null);

		if (compositionNE != null && compositionNE.getId() == MixologyIDs.MATURE_DIGWEED_COMPOSITION_ID) {
			return digweedNE;
		}
		if (compositionSE != null && compositionSE.getId() == MixologyIDs.MATURE_DIGWEED_COMPOSITION_ID) {
			return digweedSE;
		}
		if (compositionSW != null && compositionSW.getId() == MixologyIDs.MATURE_DIGWEED_COMPOSITION_ID) {
			return digweedSW;
		}
		if (compositionNW != null && compositionNW.getId() == MixologyIDs.MATURE_DIGWEED_COMPOSITION_ID) {
			return digweedNW;
		}

		return null;
	}

	private boolean isGraphicsObjectPresent(int graphicsObjectId) {
		for (GraphicsObject graphicsObject : client.getGraphicsObjects()) {
			if (graphicsObject.getId() == graphicsObjectId) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private ObjectComposition getObjectComposition(int id) {
		ObjectComposition objectComposition = client.getObjectDefinition(id);
		return objectComposition.getImpostorIds() == null ? objectComposition : objectComposition.getImpostor();
	}
}
