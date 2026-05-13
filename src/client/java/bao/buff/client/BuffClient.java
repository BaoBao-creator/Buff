package bao.buff.client;

import bao.buff.Buff;
import net.fabricmc.api.ClientModInitializer;

public class BuffClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Buff.LOGGER.info("Buff is active: particles disabled, mob animations frozen, texture and block animations frozen, fullbright forced.");
	}
}
