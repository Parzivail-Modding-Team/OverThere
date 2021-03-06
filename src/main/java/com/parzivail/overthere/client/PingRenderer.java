package com.parzivail.overthere.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parzivail.overthere.OverThere;
import com.parzivail.overthere.PingType;
import com.parzivail.overthere.QuatUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import java.util.ArrayList;

public class PingRenderer
{
	private static final Identifier PING_SHEET = OverThere.id("textures/ping/pings.png");
	private static final float SHEET_SIZE = 128 / 11f;

	private static final ArrayList<PingInstance> PING_INSTANCES = new ArrayList<>();

	public static void tick()
	{
		PING_INSTANCES.forEach(PingInstance::tick);
		PING_INSTANCES.removeIf(PingInstance::isExpired);
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

		for (var ping : PING_INSTANCES)
		{
			if (!frustum.isVisible(pingBox.offset(ping.getPosition())))
				continue;

			var facing = ping.getFacing();

			matrices.push();

			var pingPos = ping.getPosition();
			matrices.translate(pingPos.x, pingPos.y, pingPos.z);

			var lifespan = ping.getLifespan();
			var age = lifespan - ping.getLife();

			var scale = 1f;
			if (age < 2)
				scale = (age + c.tickDelta()) / 2f;
			else if (age >= lifespan - 2)
				scale = (lifespan - (age + c.tickDelta())) / 2f;

			matrices.scale(scale, scale, scale);

			var camDir = pingPos.subtract(client.player.getCameraPosVec(c.tickDelta())).normalize();
			var normal = new Vec3d(facing.getUnitVector());

			matrices.push();

			var normalQuat = facing.getRotationQuaternion();
			matrices.multiply(normalQuat);
			matrices.multiply(faceQuat);
			matrices.translate(0, -5 / 32f, 0);
			bufferPing(matrices.peek().getPositionMatrix(), immediate, normal, PingType.TARGET);
			matrices.pop();

			var billboard = QuatUtil.billboardAxis(camDir, normal);
			matrices.multiply(billboard);

			var textOffset = -client.textRenderer.fontHeight - 20;

			if (Math.abs(client.player.getRotationVecClient().dotProduct(camDir)) > 0.995)
			{
				matrices.push();
				var s = 1 / 37f;
				matrices.scale(-s, -s, -s);

				float strWidth = (float)(-client.textRenderer.getWidth(ping.getText()) / 2);

				if (facing == Direction.DOWN)
				{
					matrices.scale(-1, -1, 1);
					textOffset = 20;
				}
				else if (facing != Direction.UP)
				{
					matrices.multiply(new Quaternion(Vec3f.POSITIVE_Z, 90, true));
					textOffset = -client.textRenderer.fontHeight / 2;
					matrices.translate(strWidth - 20, 0, 0);

					var up = QuatUtil.rotate(new Vec3d(1, 0, 0), billboard);
					if (up.getY() > 0)
						matrices.scale(-1, -1, 1);
				}

				float plaqueOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
				int plaqueColor = (int)(plaqueOpacity * 255.0F) << 24;
				client.textRenderer.draw(ping.getText(), strWidth, textOffset, 0x20ffffff, false, matrices.peek().getPositionMatrix(), immediate, true, plaqueColor, 0xF000F0);
				client.textRenderer.draw(ping.getText(), strWidth, textOffset, 0xffffffff, false, matrices.peek().getPositionMatrix(), immediate, false, 0, 0xF000F0);
				matrices.pop();
			}

			matrices.translate(0.0, MathHelper.sin((age + c.tickDelta()) / 10.0F) * 0.1F + 0.1F, 0.0);

			bufferPing(matrices.peek().getPositionMatrix(), immediate, normal, ping.getType());

			matrices.pop();
		}

		matrices.pop();

		immediate.draw();
	}

	private static void bufferPing(Matrix4f mat, VertexConsumerProvider vertexConsumerProvider, Vec3d normal, PingType type)
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

		var vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCullZOffset(PING_SHEET));

		var r = 1f;
		var g = 1f;
		var b = 1f;
		var a = 1f;

		var id = type.getTextureSlot();

		var dT = 1f / SHEET_SIZE;
		var u = dT * (id % 11);
		var v = dT * (id / 11);

		vertexConsumer
				.vertex(mat, vec3fs[0].getX(), vec3fs[0].getY(), vec3fs[0].getZ())
				.color(r, g, b, a)
				.texture(u + dT, v + dT)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[1].getX(), vec3fs[1].getY(), vec3fs[1].getZ())
				.color(r, g, b, a)
				.texture(u + dT, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[2].getX(), vec3fs[2].getY(), vec3fs[2].getZ())
				.color(r, g, b, a)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal((float)normal.x, (float)normal.y, (float)normal.z)
				.next();
		vertexConsumer
				.vertex(mat, vec3fs[3].getX(), vec3fs[3].getY(), vec3fs[3].getZ())
				.color(r, g, b, a)
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

	public static void addPing(PingInstance pingInstance)
	{
		PING_INSTANCES.add(pingInstance);
	}
}
