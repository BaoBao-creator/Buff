package bao.buff.client.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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
    private static final int BUDGET_SMOOTHING_NUM = 7;
    private static final int BUDGET_SMOOTHING_DEN = 8;
    private static final double LOW_FRAME_TIME_MS = 33.0D;
    private static final double MID_FRAME_TIME_MS = 22.0D;
    private static final int ENTITY_VISIBLE_CACHE_FRAMES = 3;
    private static final int ENTITY_HIDDEN_CACHE_FRAMES = 1;
    private static final int ITEM_VISIBLE_CACHE_FRAMES = 6;
    private static final int ITEM_HIDDEN_CACHE_FRAMES = 2;
    private static final int BLOCK_ENTITY_VISIBLE_CACHE_FRAMES = 4;
    private static final int BLOCK_ENTITY_HIDDEN_CACHE_FRAMES = 2;
    private static final int MAX_CACHE_ENTRIES = 4096;
    private static final int MAX_SPATIAL_BUCKETS = 512;
    private static final int LOS_VISIBLE_BUCKET_TTL = 5;
    private static final int LOS_HIDDEN_BUCKET_TTL = 2;
    private static final double CAMERA_REUSE_SHIFT_SQ = 0.04D;

    private static final Int2ObjectLinkedOpenHashMap<CacheEntry> entityCache = new Int2ObjectLinkedOpenHashMap<>();
    private static final Long2ObjectLinkedOpenHashMap<CacheEntry> blockEntityCache = new Long2ObjectLinkedOpenHashMap<>();
    private static final Long2ObjectLinkedOpenHashMap<LosBucketEntry> losBucketCache = new Long2ObjectLinkedOpenHashMap<>(1024);

    private static Frustum activeFrustum;
    private static Vec3 cachedCameraPos;
    private static Vector3fc cachedCameraForward;
    private static Entity cachedCameraEntity;
    private static long cachedCameraCell = Long.MIN_VALUE;
    private static int frameIndex;
    private static int raycastsThisFrame;
    private static int maxRaycastsThisFrame = BASE_MAX_RAYCASTS_PER_FRAME;

    private static long cacheHits;
    private static long cacheMisses;
    private static long cachePrunes;
    private static long cacheClears;
    private static long losBucketHits;
    private static long losBucketMisses;
    private static long losBucketEvictions;
    private static long sampleRequests;
    private static double smoothedFrameTimeMs = MID_FRAME_TIME_MS;

    private VisibilityCuller() {
    }

    public static void setActiveFrustum(Frustum frustum) {
        activeFrustum = frustum;
        frameIndex++;
        raycastsThisFrame = 0;
        maxRaycastsThisFrame = computeRaycastBudget();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera != null) {
            cachedCameraPos = camera.position();
            cachedCameraForward = camera.forwardVector();
            cachedCameraEntity = camera.entity();
            cachedCameraCell = cachedCameraPos != null ? BlockPos.containing(cachedCameraPos).asLong() : Long.MIN_VALUE;
        }
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
        Entity cameraEntity = cachedCameraEntity;
        Vec3 cameraPos = cachedCameraPos;
        Vector3fc cameraForward = cachedCameraForward;
        if (cameraEntity == null || cameraPos == null || cameraForward == null) {
            return true;
        }

        if (entity == cameraEntity || entity.isPassengerOfSameVehicle(cameraEntity)) {
            return true;
        }

        boolean item = entity instanceof ItemEntity;

        AABB box = entity.getBoundingBox();
        double offset = item ? 0.05D : 0.15D;
        double minX = box.minX - offset;
        double minY = box.minY - offset;
        double minZ = box.minZ - offset;
        double maxX = box.maxX + offset;
        double maxY = box.maxY + offset;
        double maxZ = box.maxZ + offset;

        if (!frustum.isVisible(minX, minY, minZ, maxX, maxY, maxZ)) {
            remember(entityCache, entity.getId(), false, 1, Long.MIN_VALUE, Long.MIN_VALUE);
            return false;
        }

        double midX = mid(minX, maxX);
        double midY = mid(minY, maxY);
        double midZ = mid(minZ, maxZ);
        long objectCell = cellKey(midX, midY, midZ);
        long cameraCell = cachedCameraCell;
        
        CacheEntry cached = entityCache.get(entity.getId());
        if (cached != null && cached.matches(frameIndex, objectCell, cameraCell)) {
            cacheHits++;
            return cached.visible;
        }
        cacheMisses++;

        double nearSkipDistanceSq = item ? ITEM_NEAR_SKIP_DISTANCE_SQ : NEAR_SKIP_DISTANCE_SQ;
        int sampleCount = resolveSampleCount(box.getXsize(), box.getYsize(), box.getZsize(), distanceToSqr(cameraPos, midX, midY, midZ), item);
        boolean visible = isBoxVisible(entity.level(), cameraPos, cameraForward, minX, minY, minZ, maxX, maxY, maxZ, null, cameraEntity,
                nearSkipDistanceSq, sampleCount, objectCell, cameraCell);
        
        int ttl = item ? (visible ? ITEM_VISIBLE_CACHE_FRAMES : ITEM_HIDDEN_CACHE_FRAMES)
                : (visible ? ENTITY_VISIBLE_CACHE_FRAMES : ENTITY_HIDDEN_CACHE_FRAMES);
        remember(entityCache, entity.getId(), visible, ttl, objectCell, cameraCell);
        return visible;
    }

    public static boolean shouldRenderBlockEntity(Level level, BlockPos pos, Vec3 cameraPos) {
        if (cameraPos == null) {
            return true;
        }

        double offset = 0.05D;
        double minX = pos.getX() - offset;
        double minY = pos.getY() - offset;
        double minZ = pos.getZ() - offset;
        double maxX = pos.getX() + 1.0D + offset;
        double maxY = pos.getY() + 1.0D + offset;
        double maxZ = pos.getZ() + 1.0D + offset;

        Frustum frustum = activeFrustum;
        if (frustum != null && !frustum.isVisible(minX, minY, minZ, maxX, maxY, maxZ)) {
            remember(blockEntityCache, pos.asLong(), false, 1, Long.MIN_VALUE, Long.MIN_VALUE);
            return false;
        }

        long objectCell = pos.asLong();
        long cameraCell = cachedCameraCell;
        CacheEntry cached = blockEntityCache.get(objectCell);
        if (cached != null && cached.matches(frameIndex, objectCell, cameraCell)) {
            cacheHits++;
            return cached.visible;
        }
        cacheMisses++;

        Entity cameraEntity = cachedCameraEntity;
        Vector3fc cameraForward = cachedCameraForward;
        if (cameraEntity == null || cameraForward == null || cachedCameraPos == null) {
            return true;
        }
        double midX = mid(minX, maxX);
        double midY = mid(minY, maxY);
        double midZ = mid(minZ, maxZ);
        
        int sampleCount = resolveSampleCount(1.0D, 1.0D, 1.0D, distanceToSqr(cameraPos, midX, midY, midZ), false);
        boolean visible = isBoxVisible(level, cameraPos, cameraForward, minX, minY, minZ, maxX, maxY, maxZ, pos, cameraEntity,
                NEAR_SKIP_DISTANCE_SQ, sampleCount, objectCell, cameraCell);
        
        int ttl = visible ? BLOCK_ENTITY_VISIBLE_CACHE_FRAMES : BLOCK_ENTITY_HIDDEN_CACHE_FRAMES;
        remember(blockEntityCache, objectCell, visible, ttl, objectCell, cameraCell);
        return visible;
    }

    private static boolean isBoxVisible(Level level, Vec3 cameraPos, Vector3fc forward, double minX, double minY, double minZ, double maxX,
                                        double maxY, double maxZ, BlockPos targetBlock, Entity cameraEntity, double nearSkipDistanceSq, int sampleCount, long objectCell,
                                        long cameraCell) {
        double midX = mid(minX, maxX);
        double midY = mid(minY, maxY);
        double midZ = mid(minZ, maxZ);

        if (distanceToSqr(cameraPos, midX, midY, midZ) <= nearSkipDistanceSq) {
            return true;
        }
        if (!isInFrontOfCamera(cameraPos, forward, minX, minY, minZ, maxX, maxY, maxZ, midX, midY, midZ, sampleCount)) {
            return false;
        }
        if (raycastsThisFrame >= maxRaycastsThisFrame) {
            return true;
        }
        if (hasVisibleSample(level, cameraPos, midX, midY, midZ, targetBlock, cameraEntity, objectCell, cameraCell)) {
            return true;
        }
        if (sampleCount <= 1) {
            return false;
        }
        if (hasVisibleSample(level, cameraPos, midX, maxY, midZ, targetBlock, cameraEntity, objectCell, cameraCell)
                || hasVisibleSample(level, cameraPos, midX, minY, midZ, targetBlock, cameraEntity, objectCell, cameraCell)) {
            return true;
        }
        if (sampleCount <= FAST_SAMPLE_COUNT) {
            return false;
        }
        return hasVisibleSample(level, cameraPos, minX, midY, minZ, targetBlock, cameraEntity, objectCell, cameraCell)
                || hasVisibleSample(level, cameraPos, minX, midY, maxZ, targetBlock, cameraEntity, objectCell, cameraCell)
                || hasVisibleSample(level, cameraPos, maxX, midY, minZ, targetBlock, cameraEntity, objectCell, cameraCell)
                || hasVisibleSample(level, cameraPos, maxX, midY, maxZ, targetBlock, cameraEntity, objectCell, cameraCell);
    }

    private static boolean isInFrontOfCamera(Vec3 cameraPos, Vector3fc forward, double minX, double minY, double minZ, double maxX, double maxY,
                                             double maxZ, double midX, double midY, double midZ, int sampleCount) {
        if (isSampleInFront(cameraPos, forward, midX, midY, midZ)) {
            return true;
        }
        if (sampleCount <= 1) {
            return false;
        }
        if (isSampleInFront(cameraPos, forward, midX, maxY, midZ) || isSampleInFront(cameraPos, forward, midX, minY, midZ)) {
            return true;
        }
        if (sampleCount <= FAST_SAMPLE_COUNT) {
            return false;
        }
        return isSampleInFront(cameraPos, forward, minX, midY, minZ)
                || isSampleInFront(cameraPos, forward, minX, midY, maxZ)
                || isSampleInFront(cameraPos, forward, maxX, midY, minZ)
                || isSampleInFront(cameraPos, forward, maxX, midY, maxZ);
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
        return dot * dot < 0.0025D * lengthSq;
    }

    private static boolean hasVisibleSample(Level level, Vec3 from, double sampleX, double sampleY, double sampleZ, BlockPos targetBlock,
                                            Entity cameraEntity, long objectCell, long cameraCell) {
        if (raycastsThisFrame >= maxRaycastsThisFrame) {
            return true;
        }

        sampleRequests++;
        long bucket = bucketKey(objectCell, cameraCell);
        LosBucketEntry cached = losBucketCache.getAndMoveToFirst(bucket);
        
        if (cached != null && cached.expiresAtFrame >= frameIndex) {
            double dx = from.x - cached.cameraX;
            if (dx * dx <= CAMERA_REUSE_SHIFT_SQ) {
                double dy = from.y - cached.cameraY;
                if (dy * dy <= CAMERA_REUSE_SHIFT_SQ) {
                    double dz = from.z - cached.cameraZ;
                    if (dz * dz <= CAMERA_REUSE_SHIFT_SQ && dx * dx + dy * dy + dz * dz <= CAMERA_REUSE_SHIFT_SQ) {
                        losBucketHits++;
                        return cached.visible;
                    }
                }
            }
        }

        losBucketMisses++;
        raycastsThisFrame++;
        boolean visible = hasLineOfSight(level, from, sampleX, sampleY, sampleZ, targetBlock, cameraEntity);
        int ttl = visible ? LOS_VISIBLE_BUCKET_TTL : LOS_HIDDEN_BUCKET_TTL;

        if (cached != null) {
            cached.update(visible, frameIndex + ttl, from.x, from.y, from.z);
            losBucketCache.putAndMoveToFirst(bucket, cached);
        } else {
            losBucketCache.putAndMoveToFirst(bucket, new LosBucketEntry(visible, frameIndex + ttl, from.x, from.y, from.z));
        }

        while (losBucketCache.size() > MAX_SPATIAL_BUCKETS) {
            losBucketCache.removeLast();
            losBucketEvictions++;
        }
        return visible;
    }

    private static boolean hasLineOfSight(Level level, Vec3 from, double sampleX, double sampleY, double sampleZ, BlockPos targetBlock,
                                          Entity cameraEntity) {
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

    private static void remember(Int2ObjectLinkedOpenHashMap<CacheEntry> cache, int key, boolean visible, int ttl, long objectCell, long cameraCell) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.update(visible, frameIndex + ttl, objectCell, cameraCell);
            cache.putAndMoveToFirst(key, entry);
        } else {
            enforceCacheBudget(cache);
            cache.putAndMoveToFirst(key, new CacheEntry(visible, frameIndex + ttl, objectCell, cameraCell));
        }
    }

    private static void remember(Long2ObjectLinkedOpenHashMap<CacheEntry> cache, long key, boolean visible, int ttl, long objectCell, long cameraCell) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.update(visible, frameIndex + ttl, objectCell, cameraCell);
            cache.putAndMoveToFirst(key, entry);
        } else {
            enforceCacheBudget(cache);
            cache.putAndMoveToFirst(key, new CacheEntry(visible, frameIndex + ttl, objectCell, cameraCell));
        }
    }

    private static void enforceCacheBudget(Int2ObjectLinkedOpenHashMap<CacheEntry> cache) {
        while (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.removeLast();
            cacheClears++;
        }
    }

    private static void enforceCacheBudget(Long2ObjectLinkedOpenHashMap<CacheEntry> cache) {
        while (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.removeLast();
            cacheClears++;
        }
    }

    private static int computeRaycastBudget() {
        Minecraft minecraft = Minecraft.getInstance();
        int fps = minecraft.getFps();
        if (fps > 0) {
            double frameTime = 1000.0D / fps;
            smoothedFrameTimeMs = (smoothedFrameTimeMs * BUDGET_SMOOTHING_NUM + frameTime) / BUDGET_SMOOTHING_DEN;
        }

        if (smoothedFrameTimeMs >= LOW_FRAME_TIME_MS) {
            return MIN_RAYCASTS_PER_FRAME;
        }
        if (smoothedFrameTimeMs <= MID_FRAME_TIME_MS) {
            return BASE_MAX_RAYCASTS_PER_FRAME;
        }

        double alpha = (LOW_FRAME_TIME_MS - smoothedFrameTimeMs) / (LOW_FRAME_TIME_MS - MID_FRAME_TIME_MS);
        return (int) Math.round(MIN_RAYCASTS_PER_FRAME + alpha * (BASE_MAX_RAYCASTS_PER_FRAME - MIN_RAYCASTS_PER_FRAME));
    }

    private static int resolveSampleCount(double sizeX, double sizeY, double sizeZ, double distanceSq, boolean item) {
        if (item) {
            return FAST_SAMPLE_COUNT;
        }
        double extentSq = sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ;
        if (distanceSq > 1024.0D && extentSq < 4.0D) {
            return 1;
        }
        if (distanceSq > 256.0D) {
            return FAST_SAMPLE_COUNT;
        }
        return FULL_SAMPLE_COUNT;
    }

    private static long bucketKey(long objectCell, long cameraCell) {
        return objectCell * 31L + cameraCell;
    }

    private static void pruneCache(Int2ObjectMap<CacheEntry> cache) {
        cachePrunes++;
        cache.int2ObjectEntrySet().removeIf(entry -> entry.getValue().expiresAtFrame < frameIndex);
    }

    private static void pruneCache(Long2ObjectMap<CacheEntry> cache) {
        cachePrunes++;
        cache.long2ObjectEntrySet().removeIf(entry -> entry.getValue().expiresAtFrame < frameIndex);
    }

    private static long cellKey(double midX, double midY, double midZ) {
        return BlockPos.containing(midX, midY, midZ).asLong();
    }

    private static double distanceToSqr(Vec3 pos, double x, double y, double z) {
        return lengthSquared(pos.x - x, pos.y - y, pos.z - z);
    }

    private static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    private static double mid(double min, double max) {
        return (min + max) * 0.5D;
    }

    public static String debugTelemetry() {
        return "VisibilityCuller{hits=" + cacheHits + ", misses=" + cacheMisses + ", prunes=" + cachePrunes + ", clears=" + cacheClears
                + ", raycasts=" + raycastsThisFrame + "/" + maxRaycastsThisFrame + ", samples=" + sampleRequests
                + ", losHits=" + losBucketHits + ", losMisses=" + losBucketMisses + ", losEvicts=" + losBucketEvictions
                + ", frameMs=" + String.format("%.2f", smoothedFrameTimeMs) + "}";
    }

    private static final class CacheEntry {
        boolean visible;
        int expiresAtFrame;
        long objectCell;
        long cameraCell;

        CacheEntry(boolean visible, int expiresAtFrame, long objectCell, long cameraCell) {
            update(visible, expiresAtFrame, objectCell, cameraCell);
        }

        void update(boolean visible, int expiresAtFrame, long objectCell, long cameraCell) {
            this.visible = visible;
            this.expiresAtFrame = expiresAtFrame;
            this.objectCell = objectCell;
            this.cameraCell = cameraCell;
        }

        boolean matches(int frame, long currentObjectCell, long currentCameraCell) {
            return expiresAtFrame >= frame && objectCell == currentObjectCell && cameraCell == currentCameraCell;
        }
    }

    private static final class LosBucketEntry {
        boolean visible;
        int expiresAtFrame;
        double cameraX;
        double cameraY;
        double cameraZ;

        LosBucketEntry(boolean visible, int expiresAtFrame, double cameraX, double cameraY, double cameraZ) {
            update(visible, expiresAtFrame, cameraX, cameraY, cameraZ);
        }

        void update(boolean visible, int expiresAtFrame, double cameraX, double cameraY, double cameraZ) {
            this.visible = visible;
            this.expiresAtFrame = expiresAtFrame;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
        }
    }
}
