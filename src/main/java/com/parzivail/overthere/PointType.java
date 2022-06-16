package com.parzivail.overthere;

import java.util.EnumSet;
import java.util.HashMap;

public enum PointType
{
	POINT(1),
	;

	public static final HashMap<Integer, PointType> ID_TO_TYPE = new HashMap<>();

	static
	{
		for (var p : EnumSet.allOf(PointType.class))
			ID_TO_TYPE.put(p.getId(), p);
	}

	private final int id;

	PointType(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}

	public int getTextureSlot()
	{
		return id - 1;
	}
}
