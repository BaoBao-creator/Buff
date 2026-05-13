package bao.buff;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buff implements ModInitializer {
	public static final String MOD_ID = "buff";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Buff loaded. Client optimizations are always on when this mod is installed.");
	}
}
