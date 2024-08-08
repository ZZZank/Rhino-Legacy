package dev.latvian.mods.rhino.mod.wrapper;

import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public interface DirectionWrapper {
	Direction down = Direction.DOWN;
	Direction up = Direction.UP;
	Direction north = Direction.NORTH;
	Direction south = Direction.SOUTH;
	Direction west = Direction.WEST;
	Direction east = Direction.EAST;
	Direction DOWN = Direction.DOWN;
	Direction UP = Direction.UP;
	Direction NORTH = Direction.NORTH;
	Direction SOUTH = Direction.SOUTH;
	Direction WEST = Direction.WEST;
	Direction EAST = Direction.EAST;
	Map<String, Direction> ALL = Collections
		.unmodifiableMap(Arrays.stream(Direction.values())
		.collect(Collectors.toMap(Direction::getSerializedName, Function.identity())));
}