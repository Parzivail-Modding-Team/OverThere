package com.parzivail.overthere;

import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

public class QuatUtil
{
	// http://answers.unity3d.com/questions/467614/what-is-the-source-code-of-quaternionlookrotation.html
	public static Quaternion lookRotation(Vec3d up, Vec3d forwardVector)
	{
		forwardVector = forwardVector.normalize();
		var right = up.crossProduct(forwardVector).normalize();
		up = forwardVector.crossProduct(right);

		var m00 = right.x;
		var m01 = right.y;
		var m02 = right.z;
		var m10 = up.x;
		var m11 = up.y;
		var m12 = up.z;
		var m20 = forwardVector.x;
		var m21 = forwardVector.y;
		var m22 = forwardVector.z;

		var num8 = (m00 + m11) + m22;
		if (num8 > 0f)
		{
			var num = (float)Math.sqrt(num8 + 1f);
			var w = num * 0.5f;
			num = 0.5f / num;
			var x = (m12 - m21) * num;
			var y = (m20 - m02) * num;
			var z = (m01 - m10) * num;
			return new Quaternion((float)x, (float)y, (float)z, (float)w);
		}

		if ((m00 >= m11) && (m00 >= m22))
		{
			var num7 = (float)Math.sqrt(((1f + m00) - m11) - m22);
			var num4 = 0.5f / num7;
			var x = 0.5f * num7;
			var y = (m01 + m10) * num4;
			var z = (m02 + m20) * num4;
			var w = (m12 - m21) * num4;
			return new Quaternion((float)x, (float)y, (float)z, (float)w);
		}

		if (m11 > m22)
		{
			var num6 = (float)Math.sqrt(((1f + m11) - m00) - m22);
			var num3 = 0.5f / num6;
			var x = (m10 + m01) * num3;
			var y = 0.5f * num6;
			var z = (m21 + m12) * num3;
			var w = (m20 - m02) * num3;
			return new Quaternion((float)x, (float)y, (float)z, (float)w);
		}

		var num5 = (float)Math.sqrt(((1f + m22) - m00) - m11);
		var num2 = 0.5f / num5;
		var x = (m20 + m02) * num2;
		var y = (m21 + m12) * num2;
		var z = 0.5f * num5;
		var w = (m01 - m10) * num2;
		return new Quaternion((float)x, (float)y, (float)z, (float)w);
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

		return lookRotation(vUp, lookDir);
	}

	public static Vec3d rotate(Vec3d self, Quaternion q)
	{
		var u = new Vec3d(q.getX(), q.getY(), q.getZ());
		var s = q.getW();
		return u.multiply(2.0f * u.dotProduct(self))
		        .add(self.multiply(s * s - u.dotProduct(u)))
		        .add(u.crossProduct(self).multiply(2.0f * s));
	}
}
