package com.parzivail.overthere;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OverThere implements ModInitializer
{
	public static final Logger LOGGER = LoggerFactory.getLogger("Over There");
	public static final String MODID = "overthere";

	public static final Identifier PACKET_C2S_PING = id("ping");
	public static final Identifier PACKET_S2C_PONG = id("pong");

	@Override
	public void onInitialize()
	{
		ServerPlayNetworking.registerGlobalReceiver(PACKET_C2S_PING, OverThere::handlePingPacket);
	}

	private static void handlePingPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler networkHandler, PacketByteBuf buf, PacketSender sender)
	{
		var pingTypeId = buf.readInt();
		var pingType = PingType.ID_TO_TYPE.get(pingTypeId);
		if (pingType == null)
		{
			LOGGER.warn("Player {} attempted to ping with unknown ID {}", player.toString(), pingTypeId);
			return;
		}

		server.execute(() -> {
			var rayStart = player.getEyePos();
			var rayEnd = rayStart.add(player.getRotationVector().multiply(16));
			var result = player.world.raycast(new RaycastContext(rayStart, rayEnd, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, player));

			var pos = result.getPos();
			if (pos == null)
			{
				LOGGER.warn("Player {} generated HitResult with null pos", player);
				return;
			}

			var norm = result.getSide();

			var passedData = new PacketByteBuf(Unpooled.buffer());
			passedData.writeInt(PingType.DOWN_CHEVRON.getId());

			passedData.writeUuid(player.getUuid());

			passedData.writeFloat((float)pos.x);
			passedData.writeFloat((float)pos.y);
			passedData.writeFloat((float)pos.z);

			passedData.writeInt(norm.getId());

			for (var trackingPlayer : PlayerLookup.tracking((ServerWorld)player.world, result.getBlockPos()))
				ServerPlayNetworking.send(trackingPlayer, PACKET_S2C_PONG, passedData);
		});
	}

	public static Identifier id(String path)
	{
		return new Identifier(MODID, path);
	}
}
