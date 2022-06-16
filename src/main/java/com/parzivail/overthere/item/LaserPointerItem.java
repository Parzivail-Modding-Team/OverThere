package com.parzivail.overthere.item;

import com.parzivail.overthere.OverThere;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LaserPointerItem extends Item
{
	private final DyeColor color;

	public LaserPointerItem(DyeColor color, Settings settings)
	{
		super(settings);
		this.color = color;
	}

	public DyeColor getColor()
	{
		return color;
	}

	@Override
	public String getTranslationKey()
	{
		return Util.createTranslationKey("item", OverThere.id("laser_pointer"));
	}

	@Override
	public void appendTooltip(ItemStack itemStack, @Nullable World world, List<Text> list, TooltipContext tooltipContext)
	{
		list.add(new TranslatableText(Util.createTranslationKey("color", new Identifier(color.getName()))).formatted(Formatting.GRAY));
	}

	@Override
	public int getMaxUseTime(ItemStack itemStack)
	{
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack itemStack)
	{
		return UseAction.BLOCK;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand)
	{
		if (!world.isClient)
			OverThere.onPointingState(playerEntity, world, true);
		return ItemUsage.consumeHeldItem(world, playerEntity, hand);
	}

	@Override
	public void onStoppedUsing(ItemStack itemStack, World world, LivingEntity livingEntity, int i)
	{
		if (!world.isClient && livingEntity instanceof PlayerEntity playerEntity)
			OverThere.onPointingState(playerEntity, world, false);
		super.onStoppedUsing(itemStack, world, livingEntity, i);
	}
}
