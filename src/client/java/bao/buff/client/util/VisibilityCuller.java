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
        Vec3 faceCenter = new Vec3(
                blockPos.getX() + 0.5D + faceDirection.getStepX() * 0.5D,
                blockPos.getY() + 0.5D + faceDirection.getStepY() * 0.5D,
                blockPos.getZ() + 0.5D + faceDirection.getStepZ() * 0.5D);

        if (cameraPos.distanceToSqr(faceCenter) <= NEAR_SKIP_DISTANCE_SQ) {
            return true;
        }

        double viewDot = (cameraPos.x - faceCenter.x) * faceDirection.getStepX()
                + (cameraPos.y - faceCenter.y) * faceDirection.getStepY()
                + (cameraPos.z - faceCenter.z) * faceDirection.getStepZ();
        if (viewDot <= 0.01D) {
            return false;
        }

        Frustum frustum = activeFrustum;
        if (frustum != null && !frustum.isVisible(faceBounds(blockPos, faceDirection))) {
            return false;
        }

        return hasBlockFaceLineOfSight(minecraft.level, cameraPos, faceCenter, blockPos, camera.entity());
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
        if (cameraPos.distanceToSqr(center(box)) <= NEAR_SKIP_DISTANCE_SQ) {
            return true;
        }

        if (!isInFrontOfCamera(cameraPos, forward, box)) {
            return false;
        }

        for (Vec3 sample : samplePoints(box)) {
            if (hasLineOfSight(level, cameraPos, sample, targetBlock, cameraEntity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInFrontOfCamera(Vec3 cameraPos, Vector3fc forward, AABB box) {
        for (Vec3 sample : samplePoints(box)) {
            Vec3 toSample = sample.subtract(cameraPos);
            double length = toSample.length();
            if (length <= 0.0001D) {
                return true;
            }

            double dot = (toSample.x * forward.x() + toSample.y * forward.y() + toSample.z * forward.z()) / length;
            if (dot > -0.05D) {
                return true;
            }
        }
        return false;
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
            maxX = minX + 0.001D;
        } else if (direction.getStepX() > 0) {
            minX = maxX - 0.001D;
        } else if (direction.getStepY() < 0) {
            maxY = minY + 0.001D;
        } else if (direction.getStepY() > 0) {
            minY = maxY - 0.001D;
        } else if (direction.getStepZ() < 0) {
            maxZ = minZ + 0.001D;
        } else if (direction.getStepZ() > 0) {
            minZ = maxZ - 0.001D;
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Vec3 center(AABB box) {
        return new Vec3((box.minX + box.maxX) * 0.5D, (box.minY + box.maxY) * 0.5D, (box.minZ + box.maxZ) * 0.5D);
    }

    private static Vec3[] samplePoints(AABB box) {
        double midX = (box.minX + box.maxX) * 0.5D;
        double midY = (box.minY + box.maxY) * 0.5D;
        double midZ = (box.minZ + box.maxZ) * 0.5D;
        return new Vec3[] {
                new Vec3(midX, midY, midZ),
                new Vec3(midX, box.maxY, midZ),
                new Vec3(midX, box.minY, midZ),
                new Vec3(box.minX, midY, box.minZ),
                new Vec3(box.minX, midY, box.maxZ),
                new Vec3(box.maxX, midY, box.minZ),
                new Vec3(box.maxX, midY, box.maxZ)
        };
    }
}
