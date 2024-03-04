package dev.latvian.mods.rhino.util.unit;

import java.util.Objects;

public class StringUnitToken implements UnitToken {

    private final String name;
    public StringUnitToken(String name) {
        this.name = name;
    }
    public String name() {
        return this.name;
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StringUnitToken)) {
            return false;
        }
        StringUnitToken other = (StringUnitToken) obj;
        return
            this.name == other.name;
    }
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
	@Override
	public Unit interpret(UnitTokenStream stream) {
		Unit constant = stream.context.constants.get(name);

		if (constant != null) {
			return constant;
		}

		try {
			return FixedUnit.of(Double.parseDouble(name));
		} catch (Exception ex) {
			return VariableUnit.of(name);
		}
	}

	@Override
	public String toString() {
		return name;
	}
}