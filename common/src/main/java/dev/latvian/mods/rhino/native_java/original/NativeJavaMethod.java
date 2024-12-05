/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.native_java.original;

import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.native_java.ReflectsKit;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class reflects Java methods into the JavaScript environment and
 * handles overloading of methods.
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */
public class NativeJavaMethod extends BaseFunction {
	private static final long serialVersionUID = -3440381785576412928L;

	public NativeJavaMethod(MemberBox[] methods) {
		this(methods, methods[0].getName());
	}

	public NativeJavaMethod(MemberBox[] methods, String name) {
		this.functionName = name;
		this.methods = methods;
	}

	public NativeJavaMethod(MemberBox method, String name) {
		this(new MemberBox[]{method}, name);
	}

	public NativeJavaMethod(Method method, String name) {
		this(new MemberBox(method), name);
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	public static String scriptSignature(Object[] values) {
		StringBuilder sig = new StringBuilder();
		for (int i = 0; i != values.length; ++i) {
			val value = values[i];

			String s;
			if (value == null) {
				s = "null";
			} else if (value instanceof Boolean) {
				s = "boolean";
			} else if (value instanceof String) {
				s = "string";
			} else if (value instanceof Number) {
				s = "number";
			} else if (value instanceof Scriptable) {
				if (value instanceof Undefined) {
					s = "undefined";
				} else if (value instanceof Wrapper wrapper) {
					s = wrapper.unwrap().getClass().getName();
				} else if (value instanceof Function) {
					s = "function";
				} else {
					s = "object";
				}
			} else {
				s = ReflectsKit.javaSignature(value.getClass());
			}

			if (i != 0) {
				sig.append(',');
			}
			sig.append(s);
		}
		return sig.toString();
	}

	@Override
	public String toString() {
		val sb = new StringBuilder();
		for (int i = 0, N = methods.length; i != N; ++i) {
			if (i > 0) {
				sb.append('\n');
			}

			// Check member type, we also use this for overloaded constructors
			if (methods[i].isMethod()) {
				val method = methods[i].method();
				sb.append(ReflectsKit.javaSignature(method.getReturnType()));
				sb.append(' ');
				sb.append(method.getName());
			} else {
				sb.append(methods[i].getName());
			}
			sb.append(methods[i].liveConnectSignature());
		}
		return sb.toString();
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// Find a method that matches the types given.
		if (methods.length == 0) {
			throw new RuntimeException("No methods defined for call");
		}

		val index = findCachedFunction(cx, args);
		if (index < 0) {
			val c = methods[0].method().getDeclaringClass();
			val sig = c.getName() + '.' + getFunctionName() + '(' + scriptSignature(args) + ')';
			throw Context.reportRuntimeError1("msg.java.no_such_method", sig);
		}

		val meth = methods[index];
        val argTypes = meth.argTypeInfos;

		if (meth.isVararg()) {
			// marshall the explicit parameters
			val newArgs = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length - 1; i++) {
				newArgs[i] = Context.jsToJava(cx, args[i], argTypes[i]);
			}

			Object varArgs;

			// Handle special situation where a single variable parameter
			// is given, and it is a Java or ECMA array or is null.
			if (args.length == argTypes.length
				&& (args[args.length - 1] == null
						|| args[args.length - 1] instanceof NativeArray
						|| args[args.length - 1] instanceof NativeJavaArray)
			) {
				// convert the ECMA array into a native array
				varArgs = Context.jsToJava(cx, args[args.length - 1], argTypes[argTypes.length - 1]);
			} else {
				// marshall the variable parameters
				val componentType = argTypes[argTypes.length - 1].componentType();
				val length = args.length - argTypes.length + 1;
				varArgs = componentType.newArray(length);
				for (int i = 0; i < length; i++) {
					Object value = Context.jsToJava(cx, args[argTypes.length - 1 + i], componentType);
					Array.set(varArgs, i, value);
				}
			}

			// add varargs
			newArgs[argTypes.length - 1] = varArgs;
			// replace the original args with the new one
			args = newArgs;
		} else {
			// First, we marshall the args.
			val origArgs = args;
			for (int i = 0; i < args.length; i++) {
				val arg = args[i];
				var coerced = arg;

				/*
				if (arg != null) {
					TypeWrapperFactory<?> factory = argTypes[i] != null && cx.hasTypeWrappers() ? cx.getTypeWrappers().getWrapperFactory(argTypes[i], arg) : null;

					if (factory != null) {
						coerced = factory.wrap(arg);
					}
				}
				 */

				coerced = Context.jsToJava(cx, coerced, argTypes[i]);

				if (coerced != arg) {
					if (origArgs == args) {
						args = args.clone();
					}
					args[i] = coerced;
				}
			}
		}
		Object javaObject;
		if (meth.isStatic()) {
			javaObject = null;  // don't need an object
		} else {
			var o = thisObj;
			val c = meth.getDeclaringClass();
			for (; ; ) {
				if (o == null) {
					throw Context.reportRuntimeError3(
						"msg.nonjava.method",
						getFunctionName(),
						ScriptRuntime.toString(thisObj),
						c.getName()
					);
				}
				if (o instanceof Wrapper wrapper) {
					javaObject = wrapper.unwrap();
					if (c.isInstance(javaObject)) {
						break;
					}
				}
				o = o.getPrototype();
			}
		}
		if (debug) {
			printDebug("Calling ", meth, args);
		}

		val retval = meth.invoke(javaObject, args);
		val staticType = meth.returnTypeInfo;

		if (debug) {
			val actualType = (retval == null) ? null : retval.getClass();
			System.err.println(" ----- Returned " + retval + " actual = " + actualType + " expect = " + staticType);
		}

		val wrapped = cx.getWrapFactory().wrap(cx, scope, retval, staticType);
		if (debug) {
			val actualType = (wrapped == null) ? null : wrapped.getClass();
			System.err.println(" ----- Wrapped as " + wrapped + " class = " + actualType);
		}

		if (wrapped == null && staticType.isVoid()) {
			return Undefined.instance;
		}
		return wrapped;
	}

	public int findCachedFunction(Context cx, Object[] args) {
		if (methods.length <= 1) {
			return findFunction(cx, methods, args);
		}
		for (val ovl : overloadCache) {
			if (ovl.matches(args)) {
				return ovl.index;
			}
		}
		val index = findFunction(cx, methods, args);
		// As a sanity measure, don't let the lookup cache grow longer
		// than twice the number of overloaded methods
		if (overloadCache.size() < methods.length * 2) {
			overloadCache.addIfAbsent(new ResolvedOverload(args, index));
		}
		return index;
	}

	private static int @Nullable [] failFastConvWeight(Context cx, MemberBox member, Object[] args) {
		int argsLength = member.argTypeInfos.length;

		if (member.vararg) {
			argsLength--;
			if (argsLength > args.length) {
				return null;
			}
		} else {
			if (argsLength != args.length) {
				return null;
			}
		}
		val weights = new int[argsLength];
		for (int j = 0; j != argsLength; ++j) {
			val weight = NativeJavaObject.getConversionWeight(cx, args[j], member.argTypeInfos[j]);
			if (weight == NativeJavaObject.CONVERSION_NONE) {
				if (debug) {
					printDebug("Rejecting (args can't convert) ", member, args);
				}
				return null;
			}
			weights[j] = weight;
		}
		return weights;
	}

	private static final IntArrayList BEST_FIT_BUFFER = new IntArrayList();
	private static final ArrayList<int[]> BEST_WEIGHT_BUFFER = new ArrayList<>();

	/**
	 * Find the index of the correct function to call given the set of methods
	 * or constructors and the arguments.
	 * If no function can be found to call, return -1.
	 */
	static int findFunction(Context cx, MemberBox[] members, Object[] args) {
		if (members.length == 0) {
			return -1;
		}
		if (members.length == 1) {
			if (failFastConvWeight(cx, members[0], args) == null) {
				return -1;
			}
			if (debug) {
				printDebug("Found ", members[0], args);
			}
			return 0;
		}

		BEST_FIT_BUFFER.clear();
		BEST_WEIGHT_BUFFER.clear();
        for (int i = 0, membersLength = members.length; i < membersLength; i++) {
            val member = members[i];
            val weights = failFastConvWeight(cx, member, args);
            if (weights == null) {
                continue;
            }
            if (BEST_FIT_BUFFER.isEmpty()) {
                BEST_FIT_BUFFER.add(i);
                BEST_WEIGHT_BUFFER.add(weights);
                if (debug) {
                    printDebug("Found first applicable ", member, args);
                }
                continue;
            }
            val bestSize = BEST_FIT_BUFFER.size();
            for (int j = 0; j < bestSize; j++) { //compare current <-> known best
                val knownBestFit = members[BEST_FIT_BUFFER.getInt(j)];
                val knownBestWeight = BEST_WEIGHT_BUFFER.get(j);
                val prefer = preferSignature(
                    cx, args,
                    member,
                    weights,
                    knownBestFit,
                    knownBestWeight
                );
                if (prefer == PREFERENCE_FIRST_ARG) { //current > known best
                    BEST_FIT_BUFFER.clear();
                    BEST_WEIGHT_BUFFER.clear();
                    BEST_FIT_BUFFER.add(i);
                    BEST_WEIGHT_BUFFER.add(weights);
                } else if (prefer == PREFERENCE_SECOND_ARG) { //current < known best
                    continue;
                } else if (prefer == PREFERENCE_EQUAL) {
                    // This should not happen in theory, since methods
                    // but (see below)
                    if (knownBestFit.isStatic() && knownBestFit.getDeclaringClass().isAssignableFrom(member.getDeclaringClass())) {
                        // On some JVMs, Class.getMethods will return all
                        // static methods of the class hierarchy, even if
                        // a derived class's parameters match exactly.
                        // We want to call the derived class's method.
                        if (debug) {
                            printDebug("Substituting (overridden static)", member, args);
                        }
                        //in this case, consider it as current>knownBest
                        BEST_FIT_BUFFER.clear();
                        BEST_WEIGHT_BUFFER.clear();
                        BEST_FIT_BUFFER.add(i);
                        BEST_WEIGHT_BUFFER.add(weights);
                    } else {
                        if (debug) {
                            printDebug("Ignoring same signature member ", member, args);
                        }
                    }
                } else if (prefer == PREFERENCE_AMBIGUOUS) {
                    BEST_FIT_BUFFER.add(i);
                    BEST_WEIGHT_BUFFER.add(weights);
                }
            }
        }

        return switch (BEST_FIT_BUFFER.size()) {
            case 0 -> -1;
            case 1 -> BEST_FIT_BUFFER.getInt(0);
			// report remaining ambiguity
			default -> throw reportRemainingAmbiguity(cx, members, args, BEST_FIT_BUFFER);
		};
	}

	private static EvaluatorException reportRemainingAmbiguity(
		Context cx,
		MemberBox[] methodsOrCtors,
		Object[] args,
		IntArrayList bestFits
	) {
		val buf = new StringBuilder();
		for (val bestFitIndex : bestFits) {
			buf.append("\n    ");
			buf.append(methodsOrCtors[bestFitIndex].toJavaDeclaration(cx));
		}

        return Context.reportRuntimeError3(
			methodsOrCtors[0].isCtor() ? "msg.constructor.ambiguous" : "msg.method.ambiguous",
            methodsOrCtors[bestFits.getInt(0)].getName(),
			scriptSignature(args),
			buf.toString()
		);
	}

	/**
	 * Types are equal
	 */
	private static final int PREFERENCE_EQUAL = 0;
	private static final int PREFERENCE_FIRST_ARG = 1;
	private static final int PREFERENCE_SECOND_ARG = 2;
	/**
	 * No clear "easy" conversion
	 */
	private static final int PREFERENCE_AMBIGUOUS = PREFERENCE_FIRST_ARG | PREFERENCE_SECOND_ARG;

	/**
	 * Determine which of two signatures is the closer fit.
	 * Returns one of {@link #PREFERENCE_EQUAL}, {@link #PREFERENCE_FIRST_ARG},
	 * {@link #PREFERENCE_SECOND_ARG}, or {@link #PREFERENCE_AMBIGUOUS}.
	 */
	private static int preferSignature(
		Context cx,
		Object[] args,
		MemberBox member1,
		int[] computedWeights1,
		MemberBox member2,
		int[] computedWeights2
	) {
		val sig1 = member1.argTypeInfos;
		val sig2 = member2.argTypeInfos;
		val vararg1 = member1.vararg;
		val vararg2 = member2.vararg;

		int totalPreference = 0;
		for (int j = 0; j < args.length; j++) {
			val type1 = vararg1 && j >= sig1.length ? sig1[sig1.length - 1] : sig1[j];
			val type2 = vararg2 && j >= sig2.length ? sig2[sig2.length - 1] : sig2[j];
			if (type1 == type2) {
				continue;
			}
			val arg = args[j];

			// Determine which of type1, type2 is easier to convert from arg.

			val rank1 = j < computedWeights1.length
				? computedWeights1[j]
				: NativeJavaObject.getConversionWeight(cx, arg, type1);
			val rank2 = j < computedWeights2.length
				? computedWeights2[j]
				: NativeJavaObject.getConversionWeight(cx, arg, type2);

			int preference;
			if (rank1 < rank2) {
				preference = PREFERENCE_FIRST_ARG;
			} else if (rank1 > rank2) {
				preference = PREFERENCE_SECOND_ARG;
			} else {
				// Equal ranks
				if (rank1 == NativeJavaObject.CONVERSION_NONTRIVIAL) {
					if (type1.asClass().isAssignableFrom(type2.asClass())) {
						preference = PREFERENCE_SECOND_ARG;
					} else if (type2.asClass().isAssignableFrom(type1.asClass())) {
						preference = PREFERENCE_FIRST_ARG;
					} else {
						preference = PREFERENCE_AMBIGUOUS;
					}
				} else {
					preference = PREFERENCE_AMBIGUOUS;
				}
			}

			totalPreference |= preference;

			if (totalPreference == PREFERENCE_AMBIGUOUS) {
				break;
			}
		}
		return totalPreference;
	}

	private static final boolean debug = false;

	private static void printDebug(String msg, MemberBox member, Object[] args) {
		if (debug) {
			StringBuilder sb = new StringBuilder();
			sb.append(" ----- ");
			sb.append(msg);
			sb.append(member.getDeclaringClass().getName());
			sb.append('.');
			if (member.isMethod()) {
				sb.append(member.getName());
			}
			sb.append(member.liveConnectSignature());
			sb.append(" for arguments (");
			sb.append(scriptSignature(args));
			sb.append(')');
			System.out.println(sb);
		}
	}

	public final MemberBox[] methods;
	public final String functionName;
	private transient final CopyOnWriteArrayList<ResolvedOverload> overloadCache = new CopyOnWriteArrayList<>();

	static final class ResolvedOverload {
		final Class<?>[] types;
		final int index;

		ResolvedOverload(Object[] args, int index) {
			this.index = index;
			types = new Class<?>[args.length];
			for (int i = 0, l = args.length; i < l; i++) {
				Object arg = args[i];
				if (arg instanceof Wrapper) {
					arg = ((Wrapper) arg).unwrap();
				}
				types[i] = arg == null ? null : arg.getClass();
			}
		}

		boolean matches(Object[] args) {
			if (args.length != types.length) {
				return false;
			}
			for (int i = 0, len = args.length; i < len; i++) {
				Object arg = args[i];
				if (arg instanceof Wrapper) {
                    arg = ((Wrapper) arg).unwrap();
				}
//				val type = types[i];
//				if (type == null) {
//					if (arg != null) {
//						return false;
//					}
//				} else if (arg == null) {
//					return false;
//				} else if (!type.isAssignableFrom(arg.getClass())) {
//					return false;
//				}
				if (arg == null) {
					if (types[i] != null) {
						return false;
					}
				} else if (arg.getClass() != types[i]) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ResolvedOverload overload
				&& Arrays.equals(types, overload.types)
				&& index == overload.index;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(types);
		}
	}
}

