package com.parzivail.overthere.client;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PointInstance
{
	private Vec3d position;
	private Direction facing;
	private int life = 20;

	public PointInstance(Vec3d position, Direction facing)
	{
		this.position = position;
		this.facing = facing;
	}

	public Vec3d getPosition()
	{
		return position;
	}

	public void setPosition(Vec3d position)
	{
		this.position = position;
	}

	public Direction getFacing()
	{
		return facing;
	}

	public void setFacing(Direction facing)
	{
		this.facing = facing;
	}

	public void tick()
	{
		this.life--;
	}

	public boolean isExpired()
	{
		return this.life <= 0 || this.position == null;
	}

	public void heartbeat()
	{
		this.life = 20;
	}
}
