package bao.buff.client.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class VisibilityCuller {
    private static final double NEAR_SKIP_DISTANCE_SQ = 16.0D;
    private static final double HIT_EPSILON_SQ = 0.09D;
    private static final double FACE_THICKNESS = 0.001D;
    private static Frustum activeFrustum;

    private VisibilityCuller() {
    }

    public static void setActiveFrustum(Frustum frustum) {
        activeFrustum = frustum;
    }

    public static boolean shouldRenderEntity(Entity entity, Frustum frustum) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.entity();
        if (entity == cameraEntity || entity.isPassengerOfSameVehicle(cameraEntity)) {
            return true;
        }

        AABB box = entity.getBoundingBox().inflate(0.15D);
        if (!frustum.isVisible(box)) {
            return false;
        }

        return isBoxVisible(entity.level(), camera.position(), camera.forwardVector(), box, null, cameraEntity);
    }

    public static boolean shouldRenderBlockFace(Direction faceDirection, BlockPos neighborPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return true;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        BlockPos blockPos = neighborPos.relative(faceDirection.getOpposite());
        double centerX = blockPos.getX() + 0.5D + faceDirection.getStepX() * 0.5D;
        double centerY = blockPos.getY() + 0.5D + faceDirection.getStepY() * 0.5D;
        double centerZ = blockPos.getZ() + 0.5D + faceDirection.getStepZ() * 0.5D;
        double toCameraX = cameraPos.x - centerX;
        double toCameraY = cameraPos.y - centerY;
        double toCameraZ = cameraPos.z - centerZ;

        if (lengthSquared(toCameraX, toCameraY, toCameraZ) <= NEAR_SKIP_DISTANCE_SQ) {
            return true;
        }

        if (toCameraX * faceDirection.getStepX() + toCameraY * faceDirection.getStepY() + toCameraZ * faceDirection.getStepZ() <= 0.01D) {
            return false;
        }

        Frustum frustum = activeFrustum;
        if (frustum != null && !frustum.isVisible(faceBounds(blockPos, faceDirection))) {
            return false;
        }

        return hasBlockFaceLineOfSight(minecraft.level, cameraPos, new Vec3(centerX, centerY, centerZ), blockPos, camera.entity());
    }

    public static boolean shouldRenderBlockEntity(Level level, BlockPos pos, Vec3 cameraPos) {
        if (cameraPos == null) {
            return true;
        }

        AABB box = new AABB(pos).inflate(0.05D);
        Frustum frustum = activeFrustum;
        if (frustum != null && !frustum.isVisible(box)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.entity();
        return isBoxVisible(level, cameraPos, camera.forwardVector(), box, pos, cameraEntity);
    }

    private static boolean isBoxVisible(Level level, Vec3 cameraPos, Vector3fc forward, AABB box, BlockPos targetBlock, Entity cameraEntity) {
        double midX = mid(box.minX, box.maxX);
        double midY = mid(box.minY, box.maxY);
        double midZ = mid(box.minZ, box.maxZ);

        if (distanceToSqr(cameraPos, midX, midY, midZ) <= NEAR_SKIP_DISTANCE_SQ) {
            return true;
        }

        if (!isInFrontOfCamera(cameraPos, forward, box, midX, midY, midZ)) {
            return false;
        }

        return hasVisibleSample(level, cameraPos, midX, midY, midZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, midX, box.maxY, midZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, midX, box.minY, midZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.minX, midY, box.minZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.minX, midY, box.maxZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.maxX, midY, box.minZ, targetBlock, cameraEntity)
                || hasVisibleSample(level, cameraPos, box.maxX, midY, box.maxZ, targetBlock, cameraEntity);
    }

    private static boolean isInFrontOfCamera(Vec3 cameraPos, Vector3fc forward, AABB box, double midX, double midY, double midZ) {
        return isSampleInFront(cameraPos, forward, midX, midY, midZ)
                || isSampleInFront(cameraPos, forward, midX, box.maxY, midZ)
                || isSampleInFront(cameraPos, forward, midX, box.minY, midZ)
                || isSampleInFront(cameraPos, forward, box.minX, midY, box.minZ)
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

        return (x * forward.x() + y * forward.y() + z * forward.z()) / Math.sqrt(lengthSq) > -0.05D;
    }

    private static boolean hasVisibleSample(Level level, Vec3 from, double sampleX, double sampleY, double sampleZ, BlockPos targetBlock, Entity cameraEntity) {
        return hasLineOfSight(level, from, new Vec3(sampleX, sampleY, sampleZ), targetBlock, cameraEntity);
    }

    private static boolean hasBlockFaceLineOfSight(Level level, Vec3 from, Vec3 to, BlockPos targetBlock, Entity cameraEntity) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cameraEntity));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (hit.getBlockPos().equals(targetBlock)) {
            return true;
        }

        return hit.getLocation().distanceToSqr(from) + HIT_EPSILON_SQ >= to.distanceToSqr(from);
    }

    private static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to, BlockPos targetBlock, Entity cameraEntity) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cameraEntity));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (targetBlock != null && hit.getBlockPos().equals(targetBlock)) {
            return true;
        }

        return hit.getLocation().distanceToSqr(from) + HIT_EPSILON_SQ >= to.distanceToSqr(from);
    }

    private static AABB faceBounds(BlockPos blockPos, Direction direction) {
        double minX = blockPos.getX();
        double minY = blockPos.getY();
        double minZ = blockPos.getZ();
        double maxX = minX + 1.0D;
        double maxY = minY + 1.0D;
        double maxZ = minZ + 1.0D;

        if (direction.getStepX() < 0) {
            maxX = minX + FACE_THICKNESS;
        } else if (direction.getStepX() > 0) {
            minX = maxX - FACE_THICKNESS;
        } else if (direction.getStepY() < 0) {
            maxY = minY + FACE_THICKNESS;
        } else if (direction.getStepY() > 0) {
            minY = maxY - FACE_THICKNESS;
        } else if (direction.getStepZ() < 0) {
            maxZ = minZ + FACE_THICKNESS;
        } else if (direction.getStepZ() > 0) {
            minZ = maxZ - FACE_THICKNESS;
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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
}
