package com.parzivail.overthere.client;

import com.parzivail.overthere.PingType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PingInstance
{
	private final Vec3d position;
	private final Direction facing;
	private final PingType type;
	private final Text text;
	private final int lifespan;

	private int life;

	public PingInstance(Vec3d position, Direction facing, PingType type, Text text, int life)
	{
		this.position = position;
		this.facing = facing;
		this.type = type;
		this.text = text;
		this.life = this.lifespan = life;
	}

	public Vec3d getPosition()
	{
		return position;
	}

	public Direction getFacing()
	{
		return facing;
	}

	public PingType getType()
	{
		return type;
	}

	public Text getText()
	{
		return text;
	}

	public void tick()
	{
		this.life--;
	}

	public int getLife()
	{
		return life;
	}

	public int getLifespan()
	{
		return lifespan;
	}

	public boolean isExpired()
	{
		return this.life <= 0;
	}
}
