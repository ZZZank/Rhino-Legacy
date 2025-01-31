package dev.latvian.mods.rhino.util.unit;

public class Atan2Unit extends Func2Unit {
	public Atan2Unit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getFuncName() {
		return "atan2";
	}

	@Override
	public float get() {
		return (float) Math.atan2(unit.get(), with.get());
	}
}
