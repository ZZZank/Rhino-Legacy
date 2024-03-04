package dev.latvian.mods.rhino.util.unit;

import java.util.Objects;
import java.util.function.Supplier;

public class FunctionFactory {
	
	private final String name;
	private final int minArgs;
	private final int maxArgs;
	private final FuncSupplier supplier;

	public FunctionFactory(String name, int minArgs, int maxArgs, FuncSupplier supplier) {
		this.name = name;
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.supplier = supplier;
	}
	public final String name() {
		return this.name;
	}
	public final int minArgs() {
		return this.minArgs;
	}
	public final int maxArgs() {
		return this.maxArgs;
	}
	public final FuncSupplier supplier() {
		return this.supplier;
	}
	@Override
	public String toString() {
		return String.format("MemberDef {%s, %s, %s, %s}", name, minArgs, maxArgs, supplier);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof FunctionFactory) {
			FunctionFactory other = (FunctionFactory) obj;
			return this.name == other.name &&
				this.minArgs == other.minArgs &&
				this.maxArgs == other.maxArgs &&
				this.supplier == other.supplier;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return Objects.hash(name, minArgs, maxArgs, supplier);
	}

	public static final class Arg0 implements FuncSupplier {
		private final Supplier<Unit> unit;
		private Unit cachedUnit;

		public Arg0(Supplier<Unit> unit) {
			this.unit = unit;
		}

		@Override
		public Unit create(Unit[] args) {
			if (cachedUnit == null) {
				cachedUnit = unit.get();
			}

			return cachedUnit;
		}
	}

	public static FunctionFactory of(String name, int minArgs, int maxArgs, FuncSupplier supplier) {
		return new FunctionFactory(name, minArgs, maxArgs, supplier);
	}

	public static FunctionFactory of(String name, int args, FuncSupplier supplier) {
		return of(name, args, args, supplier);
	}

	public static FunctionFactory of0(String name, Supplier<Unit> supplier) {
		return of(name, 0, new Arg0(supplier));
	}

	public static FunctionFactory of1(String name, FuncSupplier.Func1 supplier) {
		return of(name, 1, supplier);
	}

	public static FunctionFactory of2(String name, FuncSupplier.Func2 supplier) {
		return of(name, 2, supplier);
	}

	public static FunctionFactory of3(String name, FuncSupplier.Func3 supplier) {
		return of(name, 3, supplier);
	}

	public Unit create(Unit[] args) {
		if (args.length < minArgs || args.length > maxArgs) {
			throw new UnitInterpretException("Invalid number of arguments for function '" + name + "'. Expected " + (minArgs == maxArgs ? String.valueOf(minArgs) : (minArgs + "-" + maxArgs)) + " but got " + args.length);
		}

		return supplier.create(args);
	}
}