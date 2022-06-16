package com.parzivail.overthere;

import java.util.EnumSet;
import java.util.HashMap;

public enum PingType
{
	DOWN_CHEVRON(1),
	DOWN_POINTER(2),
	DIAMOND(3),
	TARGET(4),
	DOWN_ARROW(5),
	CHECK(6),
	CROSS(7),
	FRAME(8),
	CROSSHAIR(9),
	SPEECH_TAIL(10),
	DOWN_CONE(11),
	EYE(12),
	QUESTION(13),
	EXCLAMATION(14),
	PIN(15),
	;

	public static final HashMap<Integer, PingType> ID_TO_TYPE = new HashMap<>();

	static
	{
		for (var p : EnumSet.allOf(PingType.class))
			ID_TO_TYPE.put(p.getId(), p);
	}

	private final int id;

	PingType(int id)
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
