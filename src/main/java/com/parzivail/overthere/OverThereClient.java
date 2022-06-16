package com.parzivail.overthere;

import com.parzivail.overthere.client.PingInstance;
import com.parzivail.overthere.client.PingRenderer;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class OverThereClient implements ClientModInitializer
{
	private static final String KEY_CATEGORY = "key.category.overthere";

	public static final KeyBinding KEY_PING = new KeyBinding("key.overthere.ping", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY);

	@Override
	public void onInitializeClient()
	{
		KeyBindingHelper.registerKeyBinding(KEY_PING);

		ClientTickEvents.START_CLIENT_TICK.register(OverThereClient::startTick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(PingRenderer::renderAfterTranslucent);
		WorldRenderEvents.END.register(PingRenderer::renderEnd);

		ClientPlayNetworking.registerGlobalReceiver(OverThere.PACKET_S2C_PONG, OverThereClient::handlePongPacket);
	}

	private static void handlePongPacket(MinecraftClient mc, ClientPlayNetworkHandler networkHandler, PacketByteBuf buf, PacketSender sender)
	{
		if (mc.world == null)
			return;

		var type = PingType.ID_TO_TYPE.get(buf.readInt());

		var sourceId = buf.readUuid();

		var x = buf.readFloat();
		var y = buf.readFloat();
		var z = buf.readFloat();

		var direction = Direction.byId(buf.readInt());

		var sourcePlayer = mc.world.getPlayerByUuid(sourceId);

		if (sourcePlayer == null)
			return;

		PingRenderer.addPing(new PingInstance(new Vec3d(x, y, z), direction, type, sourcePlayer.getDisplayName(), 100));
	}

	private static void startTick(MinecraftClient mc)
	{
		if (mc.player == null)
			return;

		if (KEY_PING.wasPressed())
		{
			while (KEY_PING.wasPressed())
				;
			sendPing(PingType.DOWN_CHEVRON);
		}

		PingRenderer.tick();
	}

	private static void sendPing(PingType pingType)
	{
		var passedData = new PacketByteBuf(Unpooled.buffer());
		passedData.writeInt(pingType.getId());
		ClientPlayNetworking.send(OverThere.PACKET_C2S_PING, passedData);
	}
}
