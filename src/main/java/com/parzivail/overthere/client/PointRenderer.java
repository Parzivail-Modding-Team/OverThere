package com.parzivail.overthere.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parzivail.overthere.OverThere;
import com.parzivail.overthere.PointType;
import com.parzivail.overthere.item.LaserPointerItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;

public class PointRenderer
{
	private static final Identifier POINT_SHEET = OverThere.id("textures/ping/points.png");
	private static final float SHEET_SIZE = 128 / 11f;

	private static final HashMap<DyeColor, Integer> POINT_COLORS = Util.make(new HashMap<>(), h -> {
		h.put(DyeColor.WHITE, 0xFFFFFFFF);
		h.put(DyeColor.ORANGE, 0xFFFF8000);
		h.put(DyeColor.MAGENTA, 0xFFFF00FF);
		h.put(DyeColor.LIGHT_BLUE, 0xFF8080FF);
		h.put(DyeColor.YELLOW, 0xFFFFFF00);
		h.put(DyeColor.LIME, 0xFFFFFF00);
		h.put(DyeColor.PINK, 0xFFFF80FF);
		h.put(DyeColor.GRAY, 0xFF808080);
		h.put(DyeColor.LIGHT_GRAY, 0xFFC0C0C0);
		h.put(DyeColor.CYAN, 0xFF00FFFF);
		h.put(DyeColor.PURPLE, 0xFF800080);
		h.put(DyeColor.BLUE, 0xFF0000FF);
		h.put(DyeColor.BROWN, 0xFF804000);
		h.put(DyeColor.GREEN, 0xFF00FF00);
		h.put(DyeColor.RED, 0xFFFF0000);
		h.put(DyeColor.BLACK, 0xFF000000);
	});

	private static final HashMap<PlayerEntity, PointInstance> POINT_INSTANCES = new HashMap<>();

	public static void tick()
	{
		POINT_INSTANCES.values().forEach(PointInstance::tick);
		POINT_INSTANCES.values().removeIf(PointInstance::isExpired);
	}

	public static void heartbeat(PlayerEntity source, boolean isUsing)
	{
		if (!isUsing)
		{
			POINT_INSTANCES.remove(source);
			return;
		}

		var sourceInstance = POINT_INSTANCES.get(source);
		if (sourceInstance == null)
			addPoint(source);
		else
			sourceInstance.heartbeat();
	}

	private static void addPoint(PlayerEntity source)
	{
		POINT_INSTANCES.put(source, new PointInstance(null, Direction.UP));
	}

	private static void render(WorldRenderContext c)
	{
		var client = MinecraftClient.getInstance();
		if (client == null)
			return;

		var player = client.player;
		var world = client.world;
		if (player == null || world == null)
			return;

		var f = client.textRenderer;
		if (f == null)
			return;

		var pos = c.camera().getPos();
		var matrices = c.matrixStack();

		matrices.push();

		var frustum = new Frustum(matrices.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix());
		frustum.setPosition(pos.x, pos.y, pos.z);
		matrices.translate(-pos.x, -pos.y, -pos.z);

		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

		var faceQuat = new Quaternion(Vec3f.POSITIVE_X, 90, true);

		var pingBox = new Box(new Vec3d(-0.25, 0.25, 0), new Vec3d(0.25, 0.25, 1));

		for (var pingEntry : POINT_INSTANCES.entrySet())
		{
			var ping = pingEntry.getValue();
			var source = pingEntry.getKey();

			if (!(source.getActiveItem().getItem() instanceof LaserPointerItem lpi))
				continue;

			var hit = getPlayerLookHit(source, c.tickDelta());
			ping.setPosition(hit.getPos());
			ping.setFacing(hit.getSide());

			if (!frustum.isVisible(pingBox.offset(ping.getPosition())))
				continue;

			var facing = ping.getFacing();

			matrices.push();

			var pingPos = ping.getPosition();
			matrices.translate(pingPos.x, pingPos.y, pingPos.z);

			matrices.multiply(facing.getRotationQuaternion());
			matrices.multiply(faceQuat);
			matrices.translate(0, -5 / 32f, -0.001f);
			bufferPoint(matrices.peek().getPositionMatrix(), immediate, new Vec3d(facing.getUnitVector()), PointType.POINT, lpi.getColor());

			matrices.pop();
		}

		matrices.pop();

		RenderSystem.enablePolygonOffset();
		RenderSystem.polygonOffset(-1, -2);

		immediate.draw();

		RenderSystem.disablePolygonOffset();
	}

	private static void bufferPoint(Matrix4f mat, VertexConsumerProvider vertexConsumerProvider, Vec3d normal, PointType type, DyeColor dyeColor)
	{
		Vec3f[] vec3fs = new Vec3f[] {
				new Vec3f(-1.0f, -1.0f, 0.0f),
				new Vec3f(-1.0f, 1.0f, 0.0f),
				new Vec3f(1.0f, 1.0f, 0.0f),
				new Vec3f(1.0f, -1.0f, 0.0f)
		};
		for (int i = 0; i < 4; ++i)
		{
			Vec3f vec = vec3fs[i];
			vec.scale(5 / 32f);
			vec.add(0, 5 / 32f, 0);
		}

		var vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getTextSeeThrough(POINT_SHEET));

		var color = POINT_COLORS.get(dyeColor);

		var id = type.getTextureSlot();

		var dT = 1f / SHEET_SIZE;
		var u = dT * (id % 11);
		var v = dT * (id / 11);

		vertexConsumer
				.vertex(mat, vec3fs[0].getX(), vec3fs[0].getY(), vec3fs[0].getZ())
				.color(color)
				.texture(u + dT, v + dT)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[1].getX(), vec3fs[1].getY(), vec3fs[1].getZ())
				.color(color)
				.texture(u + dT, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[2].getX(), vec3fs[2].getY(), vec3fs[2].getZ())
				.color(color)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[3].getX(), vec3fs[3].getY(), vec3fs[3].getZ())
				.color(color)
				.texture(u, v + dT)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
	}

	public static void renderAfterTranslucent(WorldRenderContext worldRenderContext)
	{
		if (worldRenderContext.advancedTranslucency())
			render(worldRenderContext);
	}

	public static void renderEnd(WorldRenderContext worldRenderContext)
	{
		if (!worldRenderContext.advancedTranslucency())
			render(worldRenderContext);
	}

	private static BlockHitResult getPlayerLookHit(PlayerEntity source, float tickDelta)
	{
		var rayStart = source.getCameraPosVec(tickDelta);
		var rayEnd = rayStart.add(source.getRotationVector().multiply(64));
		return source.world.raycast(new RaycastContext(rayStart, rayEnd, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, source));
	}
}
