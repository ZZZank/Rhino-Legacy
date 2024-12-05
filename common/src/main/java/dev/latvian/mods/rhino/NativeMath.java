/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the Math native object.
 * See ECMA 15.8.
 *
 * @author Norris Boyd
 */

final class NativeMath extends IdScriptableObject {
	private static final long serialVersionUID = -8838847185801131569L;

	private static final Object MATH_TAG = "Math";
	private static final double LOG2E = 1.4426950408889634;
	private static final Double Double32 = 32d;

	static void init(Scriptable scope, boolean sealed) {
		NativeMath obj = new NativeMath();
		obj.activatePrototypeMap(MAX_ID);
		obj.setPrototype(getObjectPrototype(scope));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject();
		}
		defineProperty(scope, "Math", obj, DONTENUM);
	}

	private NativeMath() {
	}

	@Override
	public String getClassName() {
		return "Math";
	}

	@Override
	protected void initPrototypeId(int id) {
		if (id <= LAST_METHOD_ID) {
			String name;
			int arity;
            name = switch (id) {
                case Id_toSource -> {
                    arity = 0;
                    yield "toSource";
                }
                case Id_abs -> {
                    arity = 1;
                    yield "abs";
                }
                case Id_acos -> {
                    arity = 1;
                    yield "acos";
                }
                case Id_acosh -> {
                    arity = 1;
                    yield "acosh";
                }
                case Id_asin -> {
                    arity = 1;
                    yield "asin";
                }
                case Id_asinh -> {
                    arity = 1;
                    yield "asinh";
                }
                case Id_atan -> {
                    arity = 1;
                    yield "atan";
                }
                case Id_atanh -> {
                    arity = 1;
                    yield "atanh";
                }
                case Id_atan2 -> {
                    arity = 2;
                    yield "atan2";
                }
                case Id_cbrt -> {
                    arity = 1;
                    yield "cbrt";
                }
                case Id_ceil -> {
                    arity = 1;
                    yield "ceil";
                }
                case Id_clz32 -> {
                    arity = 1;
                    yield "clz32";
                }
                case Id_cos -> {
                    arity = 1;
                    yield "cos";
                }
                case Id_cosh -> {
                    arity = 1;
                    yield "cosh";
                }
                case Id_exp -> {
                    arity = 1;
                    yield "exp";
                }
                case Id_expm1 -> {
                    arity = 1;
                    yield "expm1";
                }
                case Id_floor -> {
                    arity = 1;
                    yield "floor";
                }
                case Id_fround -> {
                    arity = 1;
                    yield "fround";
                }
                case Id_hypot -> {
                    arity = 2;
                    yield "hypot";
                }
                case Id_imul -> {
                    arity = 2;
                    yield "imul";
                }
                case Id_log -> {
                    arity = 1;
                    yield "log";
                }
                case Id_log1p -> {
                    arity = 1;
                    yield "log1p";
                }
                case Id_log10 -> {
                    arity = 1;
                    yield "log10";
                }
                case Id_log2 -> {
                    arity = 1;
                    yield "log2";
                }
                case Id_max -> {
                    arity = 2;
                    yield "max";
                }
                case Id_min -> {
                    arity = 2;
                    yield "min";
                }
                case Id_pow -> {
                    arity = 2;
                    yield "pow";
                }
                case Id_random -> {
                    arity = 0;
                    yield "random";
                }
                case Id_round -> {
                    arity = 1;
                    yield "round";
                }
                case Id_sign -> {
                    arity = 1;
                    yield "sign";
                }
                case Id_sin -> {
                    arity = 1;
                    yield "sin";
                }
                case Id_sinh -> {
                    arity = 1;
                    yield "sinh";
                }
                case Id_sqrt -> {
                    arity = 1;
                    yield "sqrt";
                }
                case Id_tan -> {
                    arity = 1;
                    yield "tan";
                }
                case Id_tanh -> {
                    arity = 1;
                    yield "tanh";
                }
                case Id_trunc -> {
                    arity = 1;
                    yield "trunc";
                }
                default -> throw new IllegalStateException(String.valueOf(id));
            };
			initPrototypeMethod(MATH_TAG, id, name, arity);
		} else {
			String name;
			double x;
            name = switch (id) {
                case Id_E -> {
                    x = Math.E;
                    yield "E";
                }
                case Id_PI -> {
                    x = Math.PI;
                    yield "PI";
                }
                case Id_LN10 -> {
                    x = 2.302585092994046;
                    yield "LN10";
                }
                case Id_LN2 -> {
                    x = 0.6931471805599453;
                    yield "LN2";
                }
                case Id_LOG2E -> {
                    x = LOG2E;
                    yield "LOG2E";
                }
                case Id_LOG10E -> {
                    x = 0.4342944819032518;
                    yield "LOG10E";
                }
                case Id_SQRT1_2 -> {
                    x = 0.7071067811865476;
                    yield "SQRT1_2";
                }
                case Id_SQRT2 -> {
                    x = 1.4142135623730951;
                    yield "SQRT2";
                }
                default -> throw new IllegalStateException(String.valueOf(id));
            };
			initPrototypeValue(id, name, ScriptRuntime.wrapNumber(x), DONTENUM | READONLY | PERMANENT);
		}
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(MATH_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		double x;
		int methodId = f.methodId();
		switch (methodId) {
			case Id_toSource:
				return "Math";

			case Id_abs:
				x = ScriptRuntime.toNumber(args, 0);
				// abs(-0.0) should be 0.0, but -0.0 < 0.0 == false
				x = (x == 0.0) ? 0.0 : (x < 0.0) ? -x : x;
				break;

			case Id_acos:
			case Id_asin:
				x = ScriptRuntime.toNumber(args, 0);
				if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
					x = (methodId == Id_acos) ? Math.acos(x) : Math.asin(x);
				} else {
					x = Double.NaN;
				}
				break;

			case Id_acosh:
				x = ScriptRuntime.toNumber(args, 0);
				if (!Double.isNaN(x)) {
					return Math.log(x + Math.sqrt(x * x - 1.0));
				}
				return ScriptRuntime.NaNobj;

			case Id_asinh:
				x = ScriptRuntime.toNumber(args, 0);
				if (Double.isInfinite(x)) {
					return x;
				}
				if (!Double.isNaN(x)) {
					if (x == 0) {
						if (1 / x > 0) {
							return ScriptRuntime.zeroObjInt;
						}
						return ScriptRuntime.negativeZeroObj;
					}
					return Math.log(x + Math.sqrt(x * x + 1.0));
				}
				return ScriptRuntime.NaNobj;

			case Id_atan:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.atan(x);
				break;

			case Id_atanh:
				x = ScriptRuntime.toNumber(args, 0);
				if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
					if (x == 0) {
						if (1 / x > 0) {
							return ScriptRuntime.zeroObjInt;
						}
						return ScriptRuntime.negativeZeroObj;
					}
					return 0.5 * Math.log((x + 1.0) / (x - 1.0));
				}
				return ScriptRuntime.NaNobj;

			case Id_atan2:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.atan2(x, ScriptRuntime.toNumber(args, 1));
				break;

			case Id_cbrt:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.cbrt(x);
				break;

			case Id_ceil:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.ceil(x);
				break;

			case Id_clz32:
				x = ScriptRuntime.toNumber(args, 0);
				if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
					return Double32;
				}
				long n = ScriptRuntime.toUint32(x);
				if (n == 0) {
					return Double32;
				}
				return 31 - Math.floor(Math.log(n >>> 0) * LOG2E);

			case Id_cos:
				x = ScriptRuntime.toNumber(args, 0);
				x = Double.isInfinite(x) ? Double.NaN : Math.cos(x);
				break;

			case Id_cosh:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.cosh(x);
				break;

			case Id_hypot:
				x = js_hypot(args);
				break;

			case Id_exp:
				x = ScriptRuntime.toNumber(args, 0);
				x = (x == Double.POSITIVE_INFINITY) ? x : (x == Double.NEGATIVE_INFINITY) ? 0.0 : Math.exp(x);
				break;

			case Id_expm1:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.expm1(x);
				break;

			case Id_floor:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.floor(x);
				break;

			case Id_fround:
				x = ScriptRuntime.toNumber(args, 0);
				// Rely on Java to truncate down to a "float" here"
				x = (float) x;
				break;

			case Id_imul:
				x = js_imul(args);
				break;

			case Id_log:
				x = ScriptRuntime.toNumber(args, 0);
				// Java's log(<0) = -Infinity; we need NaN
				x = (x < 0) ? Double.NaN : Math.log(x);
				break;

			case Id_log1p:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.log1p(x);
				break;

			case Id_log10:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.log10(x);
				break;

			case Id_log2:
				x = ScriptRuntime.toNumber(args, 0);
				// Java's log(<0) = -Infinity; we need NaN
				x = (x < 0) ? Double.NaN : Math.log(x) * LOG2E;
				break;

			case Id_max:
			case Id_min:
				x = (methodId == Id_max) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
				for (int i = 0; i != args.length; ++i) {
					double d = ScriptRuntime.toNumber(args[i]);
					if (Double.isNaN(d)) {
						x = d; // NaN
						break;
					}
					if (methodId == Id_max) {
						// if (x < d) x = d; does not work due to -0.0 >= +0.0
						x = Math.max(x, d);
					} else {
						x = Math.min(x, d);
					}
				}
				break;

			case Id_pow:
				x = ScriptRuntime.toNumber(args, 0);
				x = js_pow(x, ScriptRuntime.toNumber(args, 1));
				break;

			case Id_random:
				x = Math.random();
				break;

			case Id_round:
				x = ScriptRuntime.toNumber(args, 0);
				if (!Double.isNaN(x) && !Double.isInfinite(x)) {
					// Round only finite x
					long l = Math.round(x);
					if (l != 0) {
						x = l;
					} else {
						// We must propagate the sign of d into the result
						if (x < 0.0) {
							x = ScriptRuntime.negativeZero;
						} else if (x != 0.0) {
							x = 0.0;
						}
					}
				}
				break;

			case Id_sign:
				x = ScriptRuntime.toNumber(args, 0);
				if (!Double.isNaN(x)) {
					if (x == 0) {
						if (1 / x > 0) {
							return ScriptRuntime.zeroObjInt;
						}
						return ScriptRuntime.negativeZeroObj;
					}
					return Math.signum(x);
				}
				return ScriptRuntime.NaNobj;

			case Id_sin:
				x = ScriptRuntime.toNumber(args, 0);
				x = Double.isInfinite(x) ? Double.NaN : Math.sin(x);
				break;

			case Id_sinh:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.sinh(x);
				break;

			case Id_sqrt:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.sqrt(x);
				break;

			case Id_tan:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.tan(x);
				break;

			case Id_tanh:
				x = ScriptRuntime.toNumber(args, 0);
				x = Math.tanh(x);
				break;

			case Id_trunc:
				x = ScriptRuntime.toNumber(args, 0);
				x = js_trunc(x);
				break;

			default:
				throw new IllegalStateException(String.valueOf(methodId));
		}
		return ScriptRuntime.wrapNumber(x);
	}

	// See Ecma 15.8.2.13
	private static double js_pow(double x, double y) {
		double result;
		if (Double.isNaN(y)) {
			// y is NaN, result is always NaN
			result = y;
		} else if (y == 0) {
			// Java's pow(NaN, 0) = NaN; we need 1
			result = 1.0;
		} else if (x == 0) {
			// Many differences from Java's Math.pow
			if (1 / x > 0) {
				result = (y > 0) ? 0 : Double.POSITIVE_INFINITY;
			} else {
				// x is -0, need to check if y is an odd integer
				long y_long = (long) y;
				if (y_long == y && (y_long & 0x1) != 0) {
					result = (y > 0) ? -0.0 : Double.NEGATIVE_INFINITY;
				} else {
					result = (y > 0) ? 0.0 : Double.POSITIVE_INFINITY;
				}
			}
		} else {
			result = Math.pow(x, y);
			if (Double.isNaN(result)) {
				// Check for broken Java implementations that gives NaN
				// when they should return something else
				if (y == Double.POSITIVE_INFINITY) {
					if (x < -1.0 || 1.0 < x) {
						result = Double.POSITIVE_INFINITY;
					} else if (-1.0 < x && x < 1.0) {
						result = 0;
					}
				} else if (y == Double.NEGATIVE_INFINITY) {
					if (x < -1.0 || 1.0 < x) {
						result = 0;
					} else if (-1.0 < x && x < 1.0) {
						result = Double.POSITIVE_INFINITY;
					}
				} else if (x == Double.POSITIVE_INFINITY) {
					result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
				} else if (x == Double.NEGATIVE_INFINITY) {
					long y_long = (long) y;
					if (y_long == y && (y_long & 0x1) != 0) {
						// y is odd integer
						result = (y > 0) ? Double.NEGATIVE_INFINITY : -0.0;
					} else {
						result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
					}
				}
			}
		}
		return result;
	}

	// Based on code from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/hypot
	private static double js_hypot(Object[] args) {
		if (args == null) {
			return 0.0;
		}
		double y = 0.0;

		// Spec and tests say that any "Infinity" result takes precedence.
		boolean hasNaN = false;
		boolean hasInfinity = false;

		for (Object o : args) {
			double d = ScriptRuntime.toNumber(o);
			if (Double.isNaN(d)) {
				hasNaN = true;
			} else if (Double.isInfinite(d)) {
				hasInfinity = true;
			} else {
				y += d * d;
			}
		}

		if (hasInfinity) {
			return Double.POSITIVE_INFINITY;
		}
		if (hasNaN) {
			return Double.NaN;
		}
		return Math.sqrt(y);
	}

	private static double js_trunc(double d) {
		return ((d < 0.0) ? Math.ceil(d) : Math.floor(d));
	}

	// From EcmaScript 6 section 20.2.2.19
	private static int js_imul(Object[] args) {
		if (args == null) {
			return 0;
		}

		int x = ScriptRuntime.toInt32(args, 0);
		int y = ScriptRuntime.toInt32(args, 1);
		return x * y;
	}

	// #string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "toSource" -> Id_toSource;
			case "abs" -> Id_abs;
			case "acos" -> Id_acos;
			case "asin" -> Id_asin;
			case "atan" -> Id_atan;
			case "atan2" -> Id_atan2;
			case "ceil" -> Id_ceil;
			case "cos" -> Id_cos;
			case "exp" -> Id_exp;
			case "floor" -> Id_floor;
			case "log" -> Id_log;
			case "max" -> Id_max;
			case "min" -> Id_min;
			case "pow" -> Id_pow;
			case "random" -> Id_random;
			case "round" -> Id_round;
			case "sin" -> Id_sin;
			case "sqrt" -> Id_sqrt;
			case "tan" -> Id_tan;
			case "cbrt" -> Id_cbrt;
			case "cosh" -> Id_cosh;
			case "expm1" -> Id_expm1;
			case "hypot" -> Id_hypot;
			case "log1p" -> Id_log1p;
			case "log10" -> Id_log10;
			case "sinh" -> Id_sinh;
			case "tanh" -> Id_tanh;
			case "imul" -> Id_imul;
			case "trunc" -> Id_trunc;
			case "acosh" -> Id_acosh;
			case "asinh" -> Id_asinh;
			case "atanh" -> Id_atanh;
			case "sign" -> Id_sign;
			case "log2" -> Id_log2;
			case "fround" -> Id_fround;
			case "clz32" -> Id_clz32;
            case "E" -> Id_E;
			case "PI" -> Id_PI;
			case "LN10" -> Id_LN10;
			case "LN2" -> Id_LN2;
			case "LOG2E" -> Id_LOG2E;
			case "LOG10E" -> Id_LOG10E;
			case "SQRT1_2" -> Id_SQRT1_2;
			case "SQRT2" -> Id_SQRT2;
            default -> 0;
        };
	}

	private static final int Id_toSource = 1, Id_abs = 2, Id_acos = 3, Id_asin = 4, Id_atan = 5, Id_atan2 = 6, Id_ceil =
		7, Id_cos = 8, Id_exp = 9, Id_floor = 10, Id_log = 11, Id_max = 12, Id_min = 13, Id_pow = 14, Id_random = 15,
		Id_round = 16, Id_sin = 17, Id_sqrt = 18, Id_tan = 19, Id_cbrt = 20, Id_cosh = 21, Id_expm1 = 22, Id_hypot = 23,
		Id_log1p = 24, Id_log10 = 25, Id_sinh = 26, Id_tanh = 27, Id_imul = 28, Id_trunc = 29, Id_acosh = 30, Id_asinh =
		31, Id_atanh = 32, Id_sign = 33, Id_log2 = 34, Id_fround = 35, Id_clz32 = 36,

	LAST_METHOD_ID = Id_clz32;

/* Missing from ES6:
    clz32
    fround
    log2
 */

	private static final int Id_E = LAST_METHOD_ID + 1, Id_PI = LAST_METHOD_ID + 2, Id_LN10 = LAST_METHOD_ID + 3,
		Id_LN2 = LAST_METHOD_ID + 4, Id_LOG2E = LAST_METHOD_ID + 5, Id_LOG10E = LAST_METHOD_ID + 6, Id_SQRT1_2 =
		LAST_METHOD_ID + 7, Id_SQRT2 = LAST_METHOD_ID + 8,

	MAX_ID = LAST_METHOD_ID + 8;

	// #/string_id_map#
}
