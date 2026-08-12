package com.example.addon.modules;

import com.example.addon.SomethingRandom;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class FreecamFlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTeleport = settings.createGroup("Teleport Settings");

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed while in freecam flight.")
        .onChanged(aDouble -> speedValue = aDouble)
        .defaultValue(1.0)
        .min(0.0)
        .build()
    );

    private final Setting<Double> speedScrollSensitivity = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-scroll-sensitivity")
        .description("Allows you to change speed value using scroll wheel. 0 to disable.")
        .defaultValue(0)
        .min(0)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> autoTP = sgTeleport.add(new BoolSetting.Builder()
        .name("auto-teleport")
        .description("Automatically teleports your player body to follow the camera when entering air spaces.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minTpDistance = sgTeleport.add(new DoubleSetting.Builder()
        .name("min-tp-distance")
        .description("Minimum distance between camera and player required to trigger a teleport.")
        .defaultValue(1.5)
        .min(0.5)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Boolean> staySneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-sneaking")
        .description("If you are sneaking when you enter freecam, whether your player should remain sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-damage")
        .description("Disables freecam when you take damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-death")
        .description("Disables freecam when you die.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnLog = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables freecam when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("reload-chunks")
        .description("Disables cave culling.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
        .name("show-hands")
        .description("Whether or not to render your hands in freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the block or entity you are looking at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> staticView = sgGeneral.add(new BoolSetting.Builder()
        .name("static")
        .description("Disables settings that move the view.")
        .defaultValue(true)
        .build()
    );

    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();

    private CameraType perspective;
    private double speedValue;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double fovScale;
    private boolean bobView;
    private boolean isSneaking;

    public FreecamFlight() {
        super(SomethingRandom.CATEGORY, "freecam-flight", "Freecam flight that teleports your player entity along with your view into open spaces.");
    }

    @Override
    public void onActivate() {
        fovScale = mc.options.fovEffectScale().get();
        bobView = mc.options.bobView().get();

        if (staticView.get()) {
            mc.options.fovEffectScale().set(0.0);
            mc.options.bobView().set(false);
        }

        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();

        perspective = mc.options.getCameraType();
        speedValue = speed.get();

        Utils.set(pos, mc.gameRenderer.getMainCamera().position());
        Utils.set(prevPos, mc.gameRenderer.getMainCamera().position());

        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.keyShift.isDown();

        if (reloadChunks.get() && mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public void onDeactivate() {
        if (reloadChunks.get() && mc.levelRenderer != null) {
            mc.execute(mc.levelRenderer::allChanged);
        }

        mc.options.setCameraType(perspective);

        if (staticView.get()) {
            mc.options.fovEffectScale().set(fovScale);
            mc.options.bobView().set(bobView);
        }

        isSneaking = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        prevPos.set(pos);
        lastYaw = yaw;
        lastPitch = pitch;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.getCameraEntity().isInWall()) mc.getCameraEntity().noPhysics = true;
        if (!perspective.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);

        // Check GUIMove rules before sampling key bindings
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        boolean canMoveInGui = mc.screen == null || (guiMove.isActive() && !guiMove.skip());

        boolean moveForward = canMoveInGui && mc.options.keyUp.isDown();
        boolean moveBackward = canMoveInGui && mc.options.keyDown.isDown();
        boolean moveRight = canMoveInGui && mc.options.keyRight.isDown();
        boolean moveLeft = canMoveInGui && mc.options.keyLeft.isDown();
        boolean moveUp = canMoveInGui && mc.options.keyJump.isDown();
        boolean moveDown = canMoveInGui && mc.options.keyShift.isDown();
        boolean isSprinting = canMoveInGui && mc.options.keySprint.isDown();

        Vec3 forwardVec = Vec3.directionFromRotation(0, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90);

        double velX = 0;
        double velY = 0;
        double velZ = 0;

        if (rotate.get()) {
            if (mc.hitResult instanceof EntityHitResult ehr) {
                BlockPos crossHairPos = ehr.getEntity().blockPosition();
                Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
            } else if (mc.hitResult instanceof BlockHitResult bhr) {
                Vec3 crossHairPosition = bhr.getLocation();
                BlockPos crossHairPos = bhr.getBlockPos();

                if (!mc.level.getBlockState(crossHairPos).isAir()) {
                    Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
                }
            }
        }

        double moveSpeed = speedValue * (isSprinting ? 2.0 : 1.0);

        boolean isMovingHorizontally = false;
        if (moveForward) {
            velX += forwardVec.x * moveSpeed;
            velZ += forwardVec.z * moveSpeed;
            isMovingHorizontally = true;
        }
        if (moveBackward) {
            velX -= forwardVec.x * moveSpeed;
            velZ -= forwardVec.z * moveSpeed;
            isMovingHorizontally = true;
        }

        boolean isMovingSideways = false;
        if (moveRight) {
            velX += rightVec.x * moveSpeed;
            velZ += rightVec.z * moveSpeed;
            isMovingSideways = true;
        }
        if (moveLeft) {
            velX -= rightVec.x * moveSpeed;
            velZ -= rightVec.z * moveSpeed;
            isMovingSideways = true;
        }

        if (isMovingHorizontally && isMovingSideways) {
            double diagonal = 1.0 / Math.sqrt(2.0);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (moveUp) velY += moveSpeed;
        if (moveDown) velY -= moveSpeed;

        prevPos.set(pos);
        pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);

        if (autoTP.get()) {
            checkAndTeleportPlayer();
        }
    }

    private void checkAndTeleportPlayer() {
        BlockPos feetPos = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos headPos = feetPos.above();

        // Ensure both feet and head blocks have no collisions (open space)
        boolean isFeetClear = mc.level.getBlockState(feetPos).getCollisionShape(mc.level, feetPos).isEmpty();
        boolean isHeadClear = mc.level.getBlockState(headPos).getCollisionShape(mc.level, headPos).isEmpty();

        if (isFeetClear && isHeadClear) {
            double distSq = mc.player.distanceToSqr(pos.x, pos.y, pos.z);
            double minDistSq = minTpDistance.get() * minTpDistance.get();

            if (distSq >= minDistSq) {
                // Teleport player body to freecam position
                mc.player.setPos(pos.x, pos.y, pos.z);
                mc.player.setDeltaMovement(0, 0, 0);

                // Notify server of new position
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        pos.x, pos.y, pos.z,
                        mc.player.getYRot(), mc.player.getXRot(),
                        mc.player.onGround(),
                        mc.player.horizontalCollision
                    ));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (speedScrollSensitivity.get() > 0 && mc.screen == null) {
            speedValue += event.value * 0.25 * (speedScrollSensitivity.get() * speedValue);
            if (speedValue < 0.1) speedValue = 0.1;

            event.cancel();
        }
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get()) toggle();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
            Entity entity = mc.level.getEntity(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        } else if (event.packet instanceof ClientboundSetHealthPacket packet) {
            if (mc.player.getHealth() - packet.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                info("Toggled off because you took damage.");
            }
        } else if (event.packet instanceof ClientboundRespawnPacket) {
            if (isActive()) {
                toggle();
                info("Toggled off because you changed dimensions.");
            }
        }
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = Mth.clamp(pitch, -90.0f, 90.0f);
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }

    public boolean staySneaking() {
        return isActive() && !mc.player.getAbilities().flying && staySneaking.get() && isSneaking;
    }

    public double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.x, pos.x);
    }

    public double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.y, pos.y);
    }

    public double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.z, pos.z);
    }

    public double getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }

    public double getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }
}
