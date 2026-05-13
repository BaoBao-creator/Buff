package bao.buff.client.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class VisibilityCuller {
    private static final double NEAR_SKIP_DISTANCE_SQ = 16.0D;
    private static final double ITEM_NEAR_SKIP_DISTANCE_SQ = 64.0D;
    private static final double HIT_EPSILON_SQ = 0.09D;
    private static final int FULL_SAMPLE_COUNT = 7;
    private static final int FAST_SAMPLE_COUNT = 3;
    private static final int BASE_MAX_RAYCASTS_PER_FRAME = 512;
    private static final int MIN_RAYCASTS_PER_FRAME = 160;
    private static final int LOW_FPS_THRESHOLD = 35;
    private static final int MID_FPS_THRESHOLD = 50;
    private static final double DISTANT_ENTITY_SAMPLE_DISTANCE_SQ = 40.0D * 40.0D;
    private static final int ENTITY_VISIBLE_CACHE_FRAMES = 3;
    private static final int ENTITY_HIDDEN_CACHE_FRAMES = 1;
    private static final int ITEM_VISIBLE_CACHE_FRAMES = 6;
    private static final int ITEM_HIDDEN_CACHE_FRAMES = 2;
    private static final int BLOCK_ENTITY_VISIBLE_CACHE_FRAMES = 4;
    private static final int BLOCK_ENTITY_HIDDEN_CACHE_FRAMES = 2;
    private static final int MAX_CACHE_ENTRIES = 4096;

    private static final Map<Integer, CacheEntry> entityCache = new HashMap<>();
    private static final Map<Long, CacheEntry> blockEntityCache = new HashMap<>();
    private static Frustum activeFrustum;
    private static int frameIndex;
    private static int raycastsThisFrame;
    private static int maxRaycastsThisFrame = BASE_MAX_RAYCASTS_PER_FRAME;
    private static boolean lowBudgetFrame;

    private static long cacheHits;
    private static long cacheMisses;
    private static long cachePrunes;
    private static long cacheClears;

    private VisibilityCuller() {
    }

    public static void setActiveFrustum(Frustum frustum) {
        activeFrustum = frustum;
        frameIndex++;
        raycastsThisFrame = 0;
        maxRaycastsThisFrame = computeRaycastBudget();
        lowBudgetFrame = maxRaycastsThisFrame <= ((BASE_MAX_RAYCASTS_PER_FRAME + MIN_RAYCASTS_PER_FRAME) / 2);
        if ((frameIndex & 31) == 0) {
            pruneCache(entityCache);
            pruneCache(blockEntityCache);
        }
    }

    public static void clearCaches() {
        entityCache.clear();
        blockEntityCache.clear();
        cacheClears += 2;
        raycastsThisFrame = 0;
    }

    public static boolean shouldRenderEntity(Entity entity, Frustum frustum) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.entity();
        if (entity == cameraEntity || entity.isPassengerOfSameVehicle(cameraEntity)) {
            return true;
        }

        boolean item = entity instanceof ItemEntity;
        AABB box = item ? entity.getBoundingBox().inflate(0.05D) : entity.getBoundingBox().inflate(0.15D);
        if (!frustum.isVisible(box)) {
            remember(entityCache, entity.getId(), false, 1);
            return false;
        }

        Vec3 cameraPos = camera.position();
        long objectCell = cellKey(box);
        long cameraCell = BlockPos.containing(cameraPos).asLong();
        CacheEntry cached = entityCache.get(entity.getId());
        if (cached != null && cached.matches(frameIndex, objectCell, cameraCell)) {
            cacheHits++;
            return cached.visible;
        }
        cacheMisses++;

        double nearSkipDistanceSq = item ? ITEM_NEAR_SKIP_DISTANCE_SQ : NEAR_SKIP_DISTANCE_SQ;
        int sampleCount = item ? FAST_SAMPLE_COUNT : FULL_SAMPLE_COUNT;
        if (lowBudgetFrame || (!item && distanceToSqr(cameraPos, box) >= DISTANT_ENTITY_SAMPLE_DISTANCE_SQ)) {
            sampleCount = FAST_SAMPLE_COUNT;
        }
        boolean visible = isBoxVisible(entity.level(), cameraPos, camera.forwardVector(), box, null, cameraEntity, nearSkipDistanceSq, sampleCount);
        int ttl = item ? (visible ? ITEM_VISIBLE_CACHE_FRAMES : ITEM_HIDDEN_CACHE_FRAMES)
                : (visible ? ENTITY_VISIBLE_CACHE_FRAMES : ENTITY_HIDDEN_CACHE_FRAMES);
        remember(entityCache, entity.getId(), visible, ttl, objectCell, cameraCell);
        return visible;
    }

    public static boolean shouldRenderBlockEntity(Level level, BlockPos pos, Vec3 cameraPos) {
        if (cameraPos == null) {
            return true;
        }

        AABB box = new AABB(pos).inflate(0.05D);
        Frustum frustum = activeFrustum;
        if (frustum != null && !frustum.isVisible(box)) {
            remember(blockEntityCache, pos.asLong(), false, 1);
            return false;
        }

        long objectCell = pos.asLong();
        long cameraCell = BlockPos.containing(cameraPos).asLong();
        CacheEntry cached = blockEntityCache.get(objectCell);
        if (cached != null && cached.matches(frameIndex, objectCell, cameraCell)) {
            cacheHits++;
            return cached.visible;
        }
        cacheMisses++;

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.entity();
        boolean visible = isBoxVisible(level, cameraPos, camera.forwardVector(), box, pos, cameraEntity, NEAR_SKIP_DISTANCE_SQ, FAST_SAMPLE_COUNT);
        int ttl = visible ? BLOCK_ENTITY_VISIBLE_CACHE_FRAMES : BLOCK_ENTITY_HIDDEN_CACHE_FRAMES;
        remember(blockEntityCache, objectCell, visible, ttl, objectCell, cameraCell);
        return visible;
    }

    private static boolean isBoxVisible(Level level, Vec3 cameraPos, Vector3fc forward, AABB box, BlockPos targetBlock, Entity cameraEntity,
            double nearSkipDistanceSq, int sampleCount) {
        double midX = mid(box.minX, box.maxX);
        double midY = mid(box.minY, box.maxY);
        double midZ = mid(box.minZ, box.maxZ);

        if (distanceToSqr(cameraPos, midX, midY, midZ) <= nearSkipDistanceSq) {
            return true;
        }

        if (!isInFrontOfCamera(cameraPos, forward, box, midX, midY, midZ, sampleCount)) {
            return false;
        }

        if (raycastsThisFrame >= maxRaycastsThisFrame) {
            return true;
        }

        if (hasVisibleSample(level, cameraPos, midX, midY, midZ, targetBlock, cameraEntity)) {
            return true;
        }

        if (sampleCount <= 1) {
            return false;
        }

        if (hasVisibleSample(level, cameraPos, midX, box.maxY, midZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, midX, box.minY, midZ, targetBlock, cameraEntity)) {
            return true;
        }

        if (sampleCount <= FAST_SAMPLE_COUNT) {
            return false;
        }

        return hasVisibleSample(level, cameraPos, box.minX, midY, box.minZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.minX, midY, box.maxZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.maxX, midY, box.minZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.maxX, midY, box.maxZ, targetBlock, cameraEntity);
    }

    private static boolean isInFrontOfCamera(Vec3 cameraPos, Vector3fc forward, AABB box, double midX, double midY, double midZ, int sampleCount) {
        if (isSampleInFront(cameraPos, forward, midX, midY, midZ)) {
            return true;
        }

        if (sampleCount <= 1) {
            return false;
        }

        if (isSampleInFront(cameraPos, forward, midX, box.maxY, midZ) || isSampleInFront(cameraPos, forward, midX, box.minY, midZ)) {
            return true;
        }

        if (sampleCount <= FAST_SAMPLE_COUNT) {
            return false;
        }

        return isSampleInFront(cameraPos, forward, box.minX, midY, box.minZ)
                || isSampleInFront(cameraPos, forward, box.minX, midY, box.maxZ)
                || isSampleInFront(cameraPos, forward, box.maxX, midY, box.minZ)
                || isSampleInFront(cameraPos, forward, box.maxX, midY, box.maxZ);
    }

    private static boolean isSampleInFront(Vec3 cameraPos, Vector3fc forward, double sampleX, double sampleY, double sampleZ) {
        double x = sampleX - cameraPos.x;
        double y = sampleY - cameraPos.y;
        double z = sampleZ - cameraPos.z;
        double lengthSq = lengthSquared(x, y, z);
        if (lengthSq <= 1.0E-8D) {
            return true;
        }

        double dot = x * forward.x() + y * forward.y() + z * forward.z();
        if (dot >= 0.0D) {
            return true;
        }

        // Equivalent to (dot / sqrt(lengthSq)) > -0.05D but avoids sqrt/division in the hot path.
        return dot * dot < 0.0025D * lengthSq;
    }

    private static boolean hasVisibleSample(Level level, Vec3 from, double sampleX, double sampleY, double sampleZ, BlockPos targetBlock, Entity cameraEntity) {
        if (raycastsThisFrame >= maxRaycastsThisFrame) {
            return true;
        }

        raycastsThisFrame++;
        return hasLineOfSight(level, from, sampleX, sampleY, sampleZ, targetBlock, cameraEntity);
    }

    private static boolean hasLineOfSight(Level level, Vec3 from, double sampleX, double sampleY, double sampleZ, BlockPos targetBlock, Entity cameraEntity) {
        Vec3 to = new Vec3(sampleX, sampleY, sampleZ);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cameraEntity));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (targetBlock != null && hit.getBlockPos().equals(targetBlock)) {
            return true;
        }

        return hit.getLocation().distanceToSqr(from) + HIT_EPSILON_SQ >= distanceToSqr(from, sampleX, sampleY, sampleZ);
    }

    private static void remember(Map<Integer, CacheEntry> cache, int key, boolean visible, int ttl) {
        enforceCacheBudget(cache);
        cache.put(key, new CacheEntry(visible, frameIndex + ttl, Long.MIN_VALUE, Long.MIN_VALUE));
    }

    private static void remember(Map<Integer, CacheEntry> cache, int key, boolean visible, int ttl, long objectCell, long cameraCell) {
        enforceCacheBudget(cache);
        cache.put(key, new CacheEntry(visible, frameIndex + ttl, objectCell, cameraCell));
    }

    private static void remember(Map<Long, CacheEntry> cache, long key, boolean visible, int ttl) {
        enforceCacheBudget(cache);
        cache.put(key, new CacheEntry(visible, frameIndex + ttl, Long.MIN_VALUE, Long.MIN_VALUE));
    }

    private static void remember(Map<Long, CacheEntry> cache, long key, boolean visible, int ttl, long objectCell, long cameraCell) {
        enforceCacheBudget(cache);
        cache.put(key, new CacheEntry(visible, frameIndex + ttl, objectCell, cameraCell));
    }

    private static void enforceCacheBudget(Map<?, CacheEntry> cache) {
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }

        pruneCache(cache);
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
            cacheClears++;
        }
    }

    private static int computeRaycastBudget() {
        Minecraft minecraft = Minecraft.getInstance();
        int fps = minecraft.getFps();
        if (fps <= 0) {
            return BASE_MAX_RAYCASTS_PER_FRAME;
        }

        if (fps < LOW_FPS_THRESHOLD) {
            return MIN_RAYCASTS_PER_FRAME;
        }

        if (fps < MID_FPS_THRESHOLD) {
            return (BASE_MAX_RAYCASTS_PER_FRAME + MIN_RAYCASTS_PER_FRAME) / 2;
        }

        return BASE_MAX_RAYCASTS_PER_FRAME;
    }

    public static String debugTelemetry() {
        return "VisibilityCuller{hits=" + cacheHits + ", misses=" + cacheMisses + ", prunes=" + cachePrunes + ", clears=" + cacheClears
                + ", raycasts=" + raycastsThisFrame + "/" + maxRaycastsThisFrame + "}";
    }

    private static void pruneCache(Map<?, CacheEntry> cache) {
        cachePrunes++;
        Iterator<? extends Map.Entry<?, CacheEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAtFrame < frameIndex) {
                iterator.remove();
            }
        }
    }

    private static long cellKey(AABB box) {
        return BlockPos.containing(mid(box.minX, box.maxX), mid(box.minY, box.maxY), mid(box.minZ, box.maxZ)).asLong();
    }

    private static double distanceToSqr(Vec3 pos, double x, double y, double z) {
        return lengthSquared(pos.x - x, pos.y - y, pos.z - z);
    }

    private static double distanceToSqr(Vec3 pos, AABB box) {
        return distanceToSqr(pos, mid(box.minX, box.maxX), mid(box.minY, box.maxY), mid(box.minZ, box.maxZ));
    }

    private static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    private static double mid(double min, double max) {
        return (min + max) * 0.5D;
    }

    private record CacheEntry(boolean visible, int expiresAtFrame, long objectCell, long cameraCell) {
        private boolean matches(int frame, long currentObjectCell, long currentCameraCell) {
            return expiresAtFrame >= frame && objectCell == currentObjectCell && cameraCell == currentCameraCell;
        }
    }
}
