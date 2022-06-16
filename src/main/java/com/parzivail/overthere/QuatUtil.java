package com.parzivail.overthere;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class QuatUtil
{
	private static final Vec3d FORWARD = new Vec3d(0, 0, 1);

	public static Quaternion lookAt(Vec3d up, Vec3d forwardVector)
	{
		var dot = FORWARD.dotProduct(forwardVector);

		if (Math.abs(dot - (-1.0f)) < 0.000001f)
			return new Quaternion(new Vec3f((float)up.x, (float)up.y, (float)up.z), MathHelper.PI, false);
		if (Math.abs(dot - (1.0f)) < 0.000001f)
			return new Quaternion(Quaternion.IDENTITY);

		var rotAngle = Math.acos(dot);
		var rotAxis = FORWARD.crossProduct(forwardVector);
		rotAxis = rotAxis.normalize();

		return new Quaternion(new Vec3f(rotAxis), (float)rotAngle, false);
	}

	public static Quaternion billboardAxis(Vec3d lookDir, Vec3d rotateAxis)
	{
		lookDir = lookDir.normalize();
		rotateAxis = rotateAxis.normalize();

		var visible = Math.abs(lookDir.dotProduct(rotateAxis));
		if (visible >= 1)
			return new Quaternion(Quaternion.IDENTITY);

		var vRight = rotateAxis.crossProduct(lookDir);
		lookDir = vRight.crossProduct(rotateAxis);
		var vUp = lookDir.crossProduct(vRight);

		return lookAt(vUp, lookDir);
	}
}
