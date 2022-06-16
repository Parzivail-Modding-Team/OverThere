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
	private static final float SHEET_SIZE = 11 + 7 / 11f;

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

		var q = new Quaternion(Vec3f.POSITIVE_X, 90, true);

		for (var ping : PING_INSTANCES)
		{
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

			var camDir = pingPos.subtract(client.player.getCameraPosVec(c.tickDelta())).withAxis(ping.getFacing().getAxis(), 0).normalize();
			var normal = new Vec3d(ping.getFacing().getUnitVector());

			var lookVec = pingPos.subtract(client.player.getCameraPosVec(c.tickDelta())).normalize();

			matrices.push();
			matrices.multiply(ping.getFacing().getRotationQuaternion());
			matrices.multiply(q);
			matrices.translate(0, -5 / 32f, -0.001f);
			bufferPing(matrices.peek().getPositionMatrix(), immediate, normal, PingType.TARGET);
			matrices.pop();

			matrices.multiply(QuatUtil.billboardAxis(camDir, normal));

			var textOffset = -client.textRenderer.fontHeight - 20;

			if (Math.abs(client.player.getRotationVecClient().dotProduct(lookVec)) > 0.995)
			{
				matrices.push();
				var s = 1 / 37f;
				matrices.scale(-s, -s, -s);
				float plaqueOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
				int plaqueColor = (int)(plaqueOpacity * 255.0F) << 24;
				float strWidth = (float)(-client.textRenderer.getWidth(ping.getText()) / 2);
				client.textRenderer.draw(ping.getText(), strWidth, textOffset, 0x20ffffff, false, matrices.peek().getPositionMatrix(), immediate, true, plaqueColor, 0xF000F0);
				client.textRenderer.draw(ping.getText(), strWidth, textOffset, 0xffffffff, false, matrices.peek().getPositionMatrix(), immediate, false, 0, 0xF000F0);
				matrices.pop();
			}

			matrices.translate(0.0, MathHelper.sin((age + c.tickDelta()) / 10.0F) * 0.1F + 0.1F, 0.0);

			bufferPing(matrices.peek().getPositionMatrix(), immediate, normal, ping.getType());

			matrices.pop();
		}

		matrices.pop();

		RenderSystem.enablePolygonOffset();
		RenderSystem.polygonOffset(-1, -2);

		immediate.draw();

		RenderSystem.disablePolygonOffset();
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

		var vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(PING_SHEET));

		var r = 1f;
		var g = 1f;
		var b = 1f;
		var a = 1f;

		var id = type.getId() - 1;

		var dT = 1f / SHEET_SIZE;
		var u = dT * (id % 11);
		var v = dT * (int)(id / 11);

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
