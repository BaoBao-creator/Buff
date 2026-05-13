package bao.buff.client;

import bao.buff.Buff;
import net.fabricmc.api.ClientModInitializer;

public class BuffClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Buff.LOGGER.info("Buff is active: particles, sky, clouds, weather, mob animations, texture animations, and block animations disabled; fullbright forced.");
	}
}
