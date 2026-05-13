package bao.buff.client;

import bao.buff.Buff;
import net.fabricmc.api.ClientModInitializer;

public class BuffClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Buff.LOGGER.info("Buff is active: particles, sky, clouds, weather, mob animations, texture animations, block animations, and dropped-item spin disabled; dropped items use smaller flat 2D rendering; hidden entity/block-entity visibility culling is enabled; first-person held items are reduced; fullbright forced.");
	}
}
