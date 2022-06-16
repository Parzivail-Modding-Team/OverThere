package com.parzivail.overthere;

import com.parzivail.overthere.item.LaserPointerItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OverThere implements ModInitializer
{
	public static final Logger LOGGER = LoggerFactory.getLogger("Over There");
	public static final String MODID = "overthere";

	public static final Identifier PACKET_C2S_PING = id("ping");
	public static final Identifier PACKET_S2C_PONG = id("pong");
	public static final Identifier PACKET_S2C_POINT = id("point");

	@Override
	public void onInitialize()
	{
		ServerPlayNetworking.registerGlobalReceiver(PACKET_C2S_PING, OverThere::handlePingPacket);

		ServerTickEvents.START_WORLD_TICK.register(OverThere::onStartWorldTick);

		for (var dyeColor : DyeColor.values())
			Registry.register(Registry.ITEM, id(dyeColor.getName() + "_laser_pointer"), new LaserPointerItem(dyeColor, new FabricItemSettings().maxCount(1).group(ItemGroup.TOOLS)));
	}

	private static void onStartWorldTick(ServerWorld serverWorld)
	{
		for (var player : serverWorld.getPlayers())
		{
			if (player.getActiveItem().getItem() instanceof LaserPointerItem && player.getItemUseTime() > 0 && player.age % 10 == 0)
				onPointingState(player, serverWorld, true);
		}
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
			var rayEnd = rayStart.add(player.getRotationVector().multiply(64));
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

	public static void onPointingState(PlayerEntity player, World world, boolean isUsing)
	{
		var passedData = new PacketByteBuf(Unpooled.buffer());

		passedData.writeUuid(player.getUuid());
		passedData.writeBoolean(isUsing);

		for (var trackingPlayer : PlayerLookup.tracking((ServerWorld)player.world, player.getBlockPos()))
			ServerPlayNetworking.send(trackingPlayer, PACKET_S2C_POINT, passedData);
	}

	public static Identifier id(String path)
	{
		return new Identifier(MODID, path);
	}
}
