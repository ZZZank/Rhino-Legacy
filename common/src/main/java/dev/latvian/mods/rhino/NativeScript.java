/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * The JavaScript Script object.
 * <p>
 * Note that the C version of the engine uses XDR as the format used
 * by freeze and thaw. Since this depends on the internal format of
 * structures in the C runtime, we cannot duplicate it.
 * <p>
 * Since we cannot replace 'this' as a result of the compile method,
 * will forward requests to execute to the nonnull 'script' field.
 *
 * @author Norris Boyd
 * @since 1.3
 */

class NativeScript extends BaseFunction {
	private static final long serialVersionUID = -6795101161980121700L;

	private static final Object SCRIPT_TAG = "Script";

	static void init(Scriptable scope, boolean sealed) {
		NativeScript obj = new NativeScript(null);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
	}

	private NativeScript(Script script) {
		this.script = script;
	}

	/**
	 * Returns the name of this JavaScript class, "Script".
	 */
	@Override
	public String getClassName() {
		return "Script";
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (script != null) {
			return script.exec(cx, scope);
		}
		return Undefined.instance;
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		throw Context.reportRuntimeError0("msg.script.is.not.constructor");
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public int getArity() {
		return 0;
	}

	@Override
	protected void initPrototypeId(int id) {
		String s;
		int arity;
        s = switch (id) {
            case Id_constructor -> {
                arity = 1;
                yield "constructor";
            }
            case Id_toString -> {
                arity = 0;
                yield "toString";
            }
            case Id_exec -> {
                arity = 0;
                yield "exec";
            }
            case Id_compile -> {
                arity = 1;
                yield "compile";
            }
            default -> throw new IllegalArgumentException(String.valueOf(id));
        };
		initPrototypeMethod(SCRIPT_TAG, id, s, arity);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(SCRIPT_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor: {
				String source = (args.length == 0) ? "" : ScriptRuntime.toString(args[0]);
				Script script = compile(cx, source);
				NativeScript nscript = new NativeScript(script);
				ScriptRuntime.setObjectProtoAndParent(nscript, scope);
				return nscript;
			}

			case Id_toString: {
				return "not_supported";
			}

			case Id_exec: {
				throw Context.reportRuntimeError1("msg.cant.call.indirect", "exec");
			}

			case Id_compile: {
				NativeScript real = realThis(thisObj, f);
				String source = ScriptRuntime.toString(args, 0);
				real.script = compile(cx, source);
				return real;
			}
		}
		throw new IllegalArgumentException(String.valueOf(id));
	}

	private static NativeScript realThis(Scriptable thisObj, IdFunctionObject f) {
		if (!(thisObj instanceof NativeScript)) {
			throw incompatibleCallError(f);
		}
		return (NativeScript) thisObj;
	}

	private static Script compile(Context cx, String source) {
		int[] linep = {0};
		String filename = Context.getSourcePositionFromStack(cx, linep);
		if (filename == null) {
			filename = "<Script object>";
			linep[0] = 1;
		}
		ErrorReporter reporter;
		reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());
		return cx.compileString(source, null, reporter, filename, linep[0], null);
	}

	// #string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		int id;
		// #generated# Last update: 2007-05-09 08:16:01 EDT
		L0:
		{
			id = 0;
			String X = null;
            id = switch (s.length()) {
                case 4 -> {
                    X = "exec";
                    yield Id_exec;
                }
                case 7 -> {
                    X = "compile";
                    yield Id_compile;
                }
                case 8 -> {
                    X = "toString";
                    yield Id_toString;
                }
                case 11 -> {
                    X = "constructor";
                    yield Id_constructor;
                }
                default -> id;
            };
			if (X != null && X != s && !X.equals(s)) {
				id = 0;
			}
			break L0;
		}
		// #/generated#
		return id;
	}

	private static final int Id_constructor = 1, Id_toString = 2, Id_compile = 3, Id_exec = 4, MAX_PROTOTYPE_ID = 4;

	// #/string_id_map#

	private Script script;
}

