package bao.buff.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class CullingManager {
    private static final Int2ObjectOpenHashMap<VisibilityCache> VISIBILITY_CACHE = new Int2ObjectOpenHashMap<>();
    private static final double IMMEDIATE_RENDER_DISTANCE_SQR = 4.0;
    private static final double FULL_SAMPLE_DISTANCE_SQR = 16.0 * 16.0;
    private static final int NEAR_REFRESH_TICKS = 2;
    private static final int FAR_REFRESH_TICKS = 5;
    private static final int CACHE_TTL_TICKS = 40;
    private static final double SAMPLE_INSET = 0.15;

    private static Level cachedLevel;
    private static long lastCleanupTick = Long.MIN_VALUE;

    private CullingManager() {
    }

    public static void onClientTick(Minecraft client) {
        if (client.level == null) {
            cachedLevel = null;
            resetCache();
            return;
        }

        if (client.level != cachedLevel) {
            cachedLevel = client.level;
            resetCache();
        }

        long gameTime = client.level.getGameTime();
        if (gameTime - lastCleanupTick < 20L) {
            return;
        }

        lastCleanupTick = gameTime;
        VISIBILITY_CACHE.int2ObjectEntrySet().removeIf(entry -> gameTime - entry.getValue().lastCheckTick > CACHE_TTL_TICKS);
    }

    public static void onEntityUnload(Entity entity, ClientLevel world) {
        VISIBILITY_CACHE.remove(entity.getId());
    }

    public static void resetCache() {
        VISIBILITY_CACHE.clear();
        lastCleanupTick = Long.MIN_VALUE;
    }

    public static boolean shouldRenderPlayer(Player target, Vec3 cameraPosition) {
        if (!Config.playerCulling) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            cachedLevel = null;
            resetCache();
            return true;
        }

        if (client.level != cachedLevel) {
            cachedLevel = client.level;
            resetCache();
        }

        if (target == client.player || target == client.getCameraEntity()) {
            return true;
        }

        if (target.isCurrentlyGlowing()) {
            return true;
        }

        AABB bounds = target.getBoundingBox();
        if (bounds.contains(cameraPosition)) {
            return true;
        }

        double dx = target.getX() - cameraPosition.x;
        double dy = target.getEyeY() - cameraPosition.y;
        double dz = target.getZ() - cameraPosition.z;
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        if (distanceSqr <= IMMEDIATE_RENDER_DISTANCE_SQR) {
            return true;
        }

        long gameTime = client.level.getGameTime();
        int refreshInterval = distanceSqr <= FULL_SAMPLE_DISTANCE_SQR ? NEAR_REFRESH_TICKS : FAR_REFRESH_TICKS;
        int cameraBlockX = floorToInt(cameraPosition.x);
        int cameraBlockY = floorToInt(cameraPosition.y);
        int cameraBlockZ = floorToInt(cameraPosition.z);
        int targetBlockX = target.getBlockX();
        int targetBlockY = target.getBlockY();
        int targetBlockZ = target.getBlockZ();

        VisibilityCache cache = VISIBILITY_CACHE.get(target.getId());
        if (cache != null && cache.isValid(gameTime, refreshInterval, cameraBlockX, cameraBlockY, cameraBlockZ, targetBlockX, targetBlockY, targetBlockZ)) {
            return cache.visible;
        }

        boolean visible = distanceSqr <= FULL_SAMPLE_DISTANCE_SQR
            ? hasVisibleSamples(target, cameraPosition, bounds)
            : isPathClear(target, cameraPosition, target.getX(), target.getEyeY(), target.getZ());

        if (cache == null) {
            cache = new VisibilityCache();
            VISIBILITY_CACHE.put(target.getId(), cache);
        }

        cache.update(gameTime, cameraBlockX, cameraBlockY, cameraBlockZ, targetBlockX, targetBlockY, targetBlockZ, visible);
        return visible;
    }

    public static boolean shouldRenderItem(ItemEntity target, Vec3 cameraPosition) {
        if (!Config.itemCulling) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            cachedLevel = null;
            resetCache();
            return true;
        }

        if (client.level != cachedLevel) {
            cachedLevel = client.level;
            resetCache();
        }

        if (target == client.getCameraEntity()) {
            return true;
        }

        AABB bounds = target.getBoundingBox();
        if (bounds.contains(cameraPosition)) {
            return true;
        }

        double dx = target.getX() - cameraPosition.x;
        double dy = target.getY(0.5) - cameraPosition.y;
        double dz = target.getZ() - cameraPosition.z;
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        if (distanceSqr <= IMMEDIATE_RENDER_DISTANCE_SQR) {
            return true;
        }

        long gameTime = client.level.getGameTime();
        int refreshInterval = distanceSqr <= FULL_SAMPLE_DISTANCE_SQR ? NEAR_REFRESH_TICKS : FAR_REFRESH_TICKS;
        int cameraBlockX = floorToInt(cameraPosition.x);
        int cameraBlockY = floorToInt(cameraPosition.y);
        int cameraBlockZ = floorToInt(cameraPosition.z);
        int targetBlockX = target.getBlockX();
        int targetBlockY = target.getBlockY();
        int targetBlockZ = target.getBlockZ();

        VisibilityCache cache = VISIBILITY_CACHE.get(target.getId());
        if (cache != null && cache.isValid(gameTime, refreshInterval, cameraBlockX, cameraBlockY, cameraBlockZ, targetBlockX, targetBlockY, targetBlockZ)) {
            return cache.visible;
        }

        boolean visible = distanceSqr <= FULL_SAMPLE_DISTANCE_SQR
            ? hasVisibleItemSamples(target, cameraPosition, bounds)
            : isPathClear(target, cameraPosition, target.getX(), target.getY(0.5), target.getZ());

        if (cache == null) {
            cache = new VisibilityCache();
            VISIBILITY_CACHE.put(target.getId(), cache);
        }

        cache.update(gameTime, cameraBlockX, cameraBlockY, cameraBlockZ, targetBlockX, targetBlockY, targetBlockZ, visible);
        return visible;
    }

    private static boolean hasVisibleSamples(Player target, Vec3 cameraPosition, AABB bounds) {
        double midX = target.getX();
        double midZ = target.getZ();
        double lowerY = bounds.minY + bounds.getYsize() * 0.2;
        double midY = bounds.minY + bounds.getYsize() * 0.55;
        double eyeY = target.getEyeY();

        double xInset = Math.max(0.0, Math.min(bounds.getXsize() * 0.5 - 0.01, SAMPLE_INSET));
        double zInset = Math.max(0.0, Math.min(bounds.getZsize() * 0.5 - 0.01, SAMPLE_INSET));
        double leftX = bounds.minX + xInset;
        double rightX = bounds.maxX - xInset;
        double frontZ = bounds.minZ + zInset;
        double backZ = bounds.maxZ - zInset;

        return isPathClear(target, cameraPosition, midX, lowerY, midZ)
            || isPathClear(target, cameraPosition, midX, midY, midZ)
            || isPathClear(target, cameraPosition, midX, eyeY, midZ)
            || isPathClear(target, cameraPosition, leftX, midY, midZ)
            || isPathClear(target, cameraPosition, rightX, midY, midZ)
            || isPathClear(target, cameraPosition, midX, midY, frontZ)
            || isPathClear(target, cameraPosition, midX, midY, backZ);
    }

    private static boolean isPathClear(Player target, Vec3 cameraPosition, double sampleX, double sampleY, double sampleZ) {
        return target.level()
            .clip(
                new ClipContext(
                    cameraPosition,
                    new Vec3(sampleX, sampleY, sampleZ),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                )
            )
            .getType() == HitResult.Type.MISS;
    }

    private static boolean hasVisibleItemSamples(ItemEntity target, Vec3 cameraPosition, AABB bounds) {
        double centerX = target.getX();
        double centerY = target.getY(0.5);
        double centerZ = target.getZ();
        double halfWidth = Math.max(0.01, bounds.getXsize() * 0.25);
        double halfHeight = Math.max(0.01, bounds.getYsize() * 0.25);
        double halfDepth = Math.max(0.01, bounds.getZsize() * 0.25);

        return isPathClear(target, cameraPosition, centerX, centerY, centerZ)
            || isPathClear(target, cameraPosition, centerX + halfWidth, centerY, centerZ)
            || isPathClear(target, cameraPosition, centerX - halfWidth, centerY, centerZ)
            || isPathClear(target, cameraPosition, centerX, centerY + halfHeight, centerZ)
            || isPathClear(target, cameraPosition, centerX, centerY - halfHeight, centerZ)
            || isPathClear(target, cameraPosition, centerX, centerY, centerZ + halfDepth)
            || isPathClear(target, cameraPosition, centerX, centerY, centerZ - halfDepth);
    }

    private static boolean isPathClear(ItemEntity target, Vec3 cameraPosition, double sampleX, double sampleY, double sampleZ) {
        return target.level()
            .clip(
                new ClipContext(
                    cameraPosition,
                    new Vec3(sampleX, sampleY, sampleZ),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                )
            )
            .getType() == HitResult.Type.MISS;
    }

    private static int floorToInt(double value) {
        return (int)Math.floor(value);
    }

    private static final class VisibilityCache {
        private long lastCheckTick = Long.MIN_VALUE;
        private int cameraBlockX;
        private int cameraBlockY;
        private int cameraBlockZ;
        private int targetBlockX;
        private int targetBlockY;
        private int targetBlockZ;
        private boolean visible;

        private boolean isValid(
            long gameTime,
            int refreshInterval,
            int currentCameraBlockX,
            int currentCameraBlockY,
            int currentCameraBlockZ,
            int currentTargetBlockX,
            int currentTargetBlockY,
            int currentTargetBlockZ
        ) {
            return gameTime - this.lastCheckTick < refreshInterval
                && this.cameraBlockX == currentCameraBlockX
                && this.cameraBlockY == currentCameraBlockY
                && this.cameraBlockZ == currentCameraBlockZ
                && this.targetBlockX == currentTargetBlockX
                && this.targetBlockY == currentTargetBlockY
                && this.targetBlockZ == currentTargetBlockZ;
        }

        private void update(
            long gameTime,
            int currentCameraBlockX,
            int currentCameraBlockY,
            int currentCameraBlockZ,
            int currentTargetBlockX,
            int currentTargetBlockY,
            int currentTargetBlockZ,
            boolean currentVisible
        ) {
            this.lastCheckTick = gameTime;
            this.cameraBlockX = currentCameraBlockX;
            this.cameraBlockY = currentCameraBlockY;
            this.cameraBlockZ = currentCameraBlockZ;
            this.targetBlockX = currentTargetBlockX;
            this.targetBlockY = currentTargetBlockY;
            this.targetBlockZ = currentTargetBlockZ;
            this.visible = currentVisible;
        }
    }
}
