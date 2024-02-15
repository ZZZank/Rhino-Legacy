/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.regexp.NativeRegExp;
import dev.latvian.mods.rhino.regexp.RegExp;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;

/**
 * This class implements the String native object.
 * <p>
 * See ECMA 15.5.
 * <p>
 * String methods for dealing with regular expressions are
 * ported directly from C. Latest port is from version 1.40.12.19
 * in the JSFUN13_BRANCH.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 * @author Ronald Brill
 */
final class NativeString extends IdScriptableObject implements Wrapper {
	private static final Object STRING_TAG = "String";
	private static final int Id_length = 1;
	private static final int Id_namespace = 2;
	private static final int Id_path = 3;
	private static final int MAX_INSTANCE_ID = Id_path;
	private static final int ConstructorId_fromCharCode = -1;
	private static final int ConstructorId_fromCodePoint = -2;
	private static final int ConstructorId_raw = -3;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_valueOf = 4;
	private static final int Id_charAt = 5;
	private static final int Id_charCodeAt = 6;
	private static final int Id_indexOf = 7;
	private static final int Id_lastIndexOf = 8;
	private static final int Id_split = 9;
	private static final int Id_substring = 10;
	private static final int Id_toLowerCase = 11;
	private static final int Id_toUpperCase = 12;
	private static final int Id_substr = 13;
	private static final int Id_concat = 14;
	private static final int Id_slice = 15;
	private static final int Id_bold = 16;
	private static final int Id_italics = 17;
	private static final int Id_fixed = 18;
	private static final int Id_strike = 19;
	private static final int Id_small = 20;
	private static final int Id_big = 21;
	private static final int Id_blink = 22;
	private static final int Id_sup = 23;
	private static final int Id_sub = 24;
	private static final int Id_fontsize = 25;
	private static final int Id_fontcolor = 26;
	private static final int Id_link = 27;
	private static final int Id_anchor = 28;
	private static final int Id_equals = 29;
	private static final int Id_equalsIgnoreCase = 30;
	private static final int Id_match = 31;

	// #string_id_map#
	private static final int Id_search = 32;
	private static final int Id_replace = 33;
	private static final int Id_localeCompare = 34;
	private static final int Id_toLocaleLowerCase = 35;
	private static final int Id_toLocaleUpperCase = 36;
	private static final int Id_trim = 37;
	private static final int Id_trimLeft = 38;
	private static final int Id_trimRight = 39;
	private static final int Id_includes = 40;
	private static final int Id_startsWith = 41;
	private static final int Id_endsWith = 42;
	private static final int Id_normalize = 43;
	private static final int Id_repeat = 44;
	private static final int Id_codePointAt = 45;
	private static final int Id_padStart = 46;
	private static final int Id_padEnd = 47;
	private static final int Id_trimStart = 48;
	private static final int Id_trimEnd = 49;
	private static final int SymbolId_iterator = 50;
	private static final int MAX_PROTOTYPE_ID = SymbolId_iterator;
	private static final int ConstructorId_charAt = -Id_charAt;
	private static final int ConstructorId_charCodeAt = -Id_charCodeAt;
	private static final int ConstructorId_indexOf = -Id_indexOf;
	private static final int ConstructorId_lastIndexOf = -Id_lastIndexOf;
	private static final int ConstructorId_split = -Id_split;
	private static final int ConstructorId_substring = -Id_substring;
	private static final int ConstructorId_toLowerCase = -Id_toLowerCase;
	private static final int ConstructorId_toUpperCase = -Id_toUpperCase;
	private static final int ConstructorId_substr = -Id_substr;
	private static final int ConstructorId_concat = -Id_concat;
	private static final int ConstructorId_slice = -Id_slice;
	private static final int ConstructorId_equalsIgnoreCase = -Id_equalsIgnoreCase;
	private static final int ConstructorId_match = -Id_match;
	private static final int ConstructorId_search = -Id_search;
	private static final int ConstructorId_replace = -Id_replace;
	private static final int ConstructorId_localeCompare = -Id_localeCompare;
	private static final int ConstructorId_toLocaleLowerCase = -Id_toLocaleLowerCase;

	static void init(Scriptable scope, boolean sealed) {
		NativeString obj = new NativeString("");
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
	}

	private static NativeString realThis(Scriptable thisObj, IdFunctionObject f) {
		if (!(thisObj instanceof NativeString)) {
			throw incompatibleCallError(f);
		}
		return (NativeString) thisObj;
	}

	/*
	 * HTML composition aids.
	 */
	private static String tagify(Scriptable thisObj, String tag, String attribute, Object[] args) {
		String str = ScriptRuntime.toString(thisObj);
		StringBuilder result = new StringBuilder();
		result.append('<').append(tag);
		if (attribute != null) {
			result.append(' ').append(attribute).append("=\"").append(ScriptRuntime.toString(args, 0)).append('"');
		}
		result.append('>').append(str).append("</").append(tag).append('>');
		return result.toString();
	}

	/*
	 *
	 * See ECMA 15.5.4.6.  Uses Java String.indexOf()
	 * OPT to add - BMH searching from jsstr.c.
	 */
	private static int js_indexOf(int methodId, String target, Object[] args) {
		String searchStr = ScriptRuntime.toString(args, 0);
		double position = ScriptRuntime.toInteger(args, 1);

		if (methodId != Id_startsWith && methodId != Id_endsWith && searchStr.length() == 0) {
			return position > target.length() ? target.length() : (int) position;
		}

		if (methodId != Id_startsWith && methodId != Id_endsWith && position > target.length()) {
			return -1;
		}

		if (position < 0) {
			position = 0;
		} else if (position > target.length()) {
			position = target.length();
		} else if (methodId == Id_endsWith && (Double.isNaN(position) || position > target.length())) {
			position = target.length();
		}

		if (Id_endsWith == methodId) {
			if (args.length == 0 || args.length == 1 || (args.length == 2 && args[1] == Undefined.instance)) {
				position = target.length();
			}
			return target.substring(0, (int) position).endsWith(searchStr) ? 0 : -1;
		}
		return methodId == Id_startsWith ? target.startsWith(searchStr, (int) position) ? 0 : -1 : target.indexOf(searchStr, (int) position);
	}

	/*
	 *
	 * See ECMA 15.5.4.7
	 *
	 */
	private static int js_lastIndexOf(String target, Object[] args) {
		String search = ScriptRuntime.toString(args, 0);
		double end = ScriptRuntime.toNumber(args, 1);

		if (Double.isNaN(end) || end > target.length()) {
			end = target.length();
		} else if (end < 0) {
			end = 0;
		}

		return target.lastIndexOf(search, (int) end);
	}

	/*
	 * See ECMA 15.5.4.15
	 */
	private static CharSequence js_substring(Context cx, CharSequence target, Object[] args) {
		int length = target.length();
		double start = ScriptRuntime.toInteger(args, 0);
		double end;

		if (start < 0) {
			start = 0;
		} else if (start > length) {
			start = length;
		}

		if (args.length <= 1 || args[1] == Undefined.instance) {
			end = length;
		} else {
			end = ScriptRuntime.toInteger(args[1]);
			if (end < 0) {
				end = 0;
			} else if (end > length) {
				end = length;
			}

			// swap if end < start
			if (end < start) {
				double temp = start;
				start = end;
				end = temp;
			}
		}
		return target.subSequence((int) start, (int) end);
	}

	/*
	 * Non-ECMA methods.
	 */
	private static CharSequence js_substr(CharSequence target, Object[] args) {
		if (args.length < 1) {
			return target;
		}

		double begin = ScriptRuntime.toInteger(args[0]);
		double end;
		int length = target.length();

		if (begin < 0) {
			begin += length;
			if (begin < 0) {
				begin = 0;
			}
		} else if (begin > length) {
			begin = length;
		}

		end = length;
		if (args.length > 1) {
			Object lengthArg = args[1];

			if (!Undefined.isUndefined(lengthArg)) {
				end = ScriptRuntime.toInteger(lengthArg);
				if (end < 0) {
					end = 0;
				}
				end += begin;
				if (end > length) {
					end = length;
				}
			}
		}

		return target.subSequence((int) begin, (int) end);
	}

	/*
	 * Python-esque sequence operations.
	 */
	private static String js_concat(String target, Object[] args) {
		int N = args.length;
		if (N == 0) {
			return target;
		} else if (N == 1) {
			String arg = ScriptRuntime.toString(args[0]);
			return target.concat(arg);
		}

		// Find total capacity for the final string to avoid unnecessary
		// re-allocations in StringBuilder
		int size = target.length();
		String[] argsAsStrings = new String[N];
		for (int i = 0; i != N; ++i) {
			String s = ScriptRuntime.toString(args[i]);
			argsAsStrings[i] = s;
			size += s.length();
		}

		StringBuilder result = new StringBuilder(size);
		result.append(target);
		for (int i = 0; i != N; ++i) {
			result.append(argsAsStrings[i]);
		}
		return result.toString();
	}

	private static CharSequence js_slice(CharSequence target, Object[] args) {
		double begin = args.length < 1 ? 0 : ScriptRuntime.toInteger(args[0]);
		double end;
		int length = target.length();
		if (begin < 0) {
			begin += length;
			if (begin < 0) {
				begin = 0;
			}
		} else if (begin > length) {
			begin = length;
		}

		if (args.length < 2 || args[1] == Undefined.instance) {
			end = length;
		} else {
			end = ScriptRuntime.toInteger(args[1]);
			if (end < 0) {
				end += length;
				if (end < 0) {
					end = 0;
				}
			} else if (end > length) {
				end = length;
			}
			if (end < begin) {
				end = begin;
			}
		}
		return target.subSequence((int) begin, (int) end);
	}

	private static String js_repeat(Context cx, Scriptable thisObj, IdFunctionObject f, Object[] args) {
		String str = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
		double cnt = ScriptRuntime.toInteger(args, 0);

		if ((cnt < 0.0) || (cnt == Double.POSITIVE_INFINITY)) {
			throw ScriptRuntime.rangeError("Invalid count value");
		}

		if (cnt == 0.0 || str.length() == 0) {
			return "";
		}

		long size = str.length() * (long) cnt;
		// Check for overflow
		if ((cnt > Integer.MAX_VALUE) || (size > Integer.MAX_VALUE)) {
			throw ScriptRuntime.rangeError("Invalid size or count value");
		}

		StringBuilder retval = new StringBuilder((int) size);
		retval.append(str);

		int i = 1;
		int icnt = (int) cnt;
		while (i <= (icnt / 2)) {
			retval.append(retval);
			i *= 2;
		}
		if (i < icnt) {
			retval.append(retval.substring(0, str.length() * (icnt - i)));
		}

		return retval.toString();
	}

	/**
	 * @see <a href='https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padstart'>padstart</a>
	 * @see <a href='https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padend'>padend</a>
	 */
	private static String js_pad(Context cx, Scriptable thisObj, IdFunctionObject f, Object[] args, boolean atStart) {
		String pad = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
		long intMaxLength = ScriptRuntime.toLength(args, 0);
		if (intMaxLength <= pad.length()) {
			return pad;
		}

		String filler = " ";
		if (args.length >= 2 && !Undefined.isUndefined(args[1])) {
			filler = ScriptRuntime.toString(args[1]);
			if (filler.length() < 1) {
				return pad;
			}
		}

		// cast is not really correct here
		int fillLen = (int) (intMaxLength - pad.length());
		StringBuilder concat = new StringBuilder();
		do {
			concat.append(filler);
		} while (concat.length() < fillLen);
		concat.setLength(fillLen);

		if (atStart) {
			return concat.append(pad).toString();
		}

		return concat.insert(0, pad).toString();
	}

	/**
	 * <h1>String.raw (callSite, ...substitutions)</h1>
	 * <p>15.5.3.4 String.raw [ECMA 6 - draft]</p>
	 */
	private static CharSequence js_raw(Context cx, Scriptable scope, Object[] args) {
		final Object undefined = Undefined.instance;
		/* step 1-3 */
		Object arg0 = args.length > 0 ? args[0] : undefined;
		Scriptable cooked = ScriptRuntime.toObject(cx, scope, arg0);
		/* step 4-6 */
		Object rawValue = cooked.get("raw", cooked);
		if (rawValue == NOT_FOUND) {
			rawValue = undefined;
		}
		Scriptable raw = ScriptRuntime.toObject(cx, scope, rawValue);
		/* step 7-9 */
		Object len = raw.get("length", raw);
		if (len == NOT_FOUND) {
			len = undefined;
		}
		long literalSegments = ScriptRuntime.toUint32(len);
		/* step 10 */
		if (literalSegments == 0) {
			return "";
		}
		/* step 11-13 */
		StringBuilder elements = new StringBuilder();
		long nextIndex = 0;
		for (; ; ) {
			/* step 13 a-e */
			Object next;
			if (nextIndex > Integer.MAX_VALUE) {
				next = raw.get(Long.toString(nextIndex), raw);
			} else {
				next = raw.get((int) nextIndex, raw);
			}
			if (next == NOT_FOUND) {
				next = undefined;
			}
			String nextSeg = ScriptRuntime.toString(next);
			elements.append(nextSeg);
			nextIndex += 1;
			if (nextIndex == literalSegments) {
				break;
			}
			next = args.length > nextIndex ? args[(int) nextIndex] : undefined;
			String nextSub = ScriptRuntime.toString(next);
			elements.append(nextSub);
		}
		return elements.toString();
	}

	private final CharSequence string;

	NativeString(CharSequence s) {
		string = s;
	}

	@Override
	public String getClassName() {
		return "String";
	}

	@Override
	public Object unwrap() {
		return string;
	}

	@Override
	public MemberType getTypeOf() {
		return MemberType.STRING;
	}

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	// #/string_id_map#

	@Override
	protected int findInstanceIdInfo(String s) {
		switch (s) {
			case "length": return instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_length);
			case "namespace": return instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_namespace);
			case "path": return instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_path);
			default: return super.findInstanceIdInfo(s);
		}
	}

	@Override
	protected String getInstanceIdName(int id) {
		switch (id) {
			case Id_length: return "length";
			case Id_namespace: return "namespace";
			case Id_path: return "path";
			default: return super.getInstanceIdName(id);
		}
	}

	@Override
	protected Object getInstanceIdValue(int id) {
		switch (id) {
			case Id_length:
				return ScriptRuntime.wrapInt(string.length());
			case Id_namespace: {
				String str = ScriptRuntime.toString(string);
				int colon = str.indexOf(':');
				return colon == -1 ? "minecraft" : str.substring(0, colon);
			}
			case Id_path: {
				String str = ScriptRuntime.toString(string);
				int colon = str.indexOf(':');
				return colon == -1 ? str : str.substring(colon + 1);
			}
			default:
				return super.getInstanceIdValue(id);
		}
	}

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor) {
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_fromCharCode, "fromCharCode", 1);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_fromCodePoint, "fromCodePoint", 1);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_raw, "raw", 1);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_charAt, "charAt", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_charCodeAt, "charCodeAt", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_indexOf, "indexOf", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_lastIndexOf, "lastIndexOf", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_split, "split", 3);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_substring, "substring", 3);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_toLowerCase, "toLowerCase", 1);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_toUpperCase, "toUpperCase", 1);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_substr, "substr", 3);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_concat, "concat", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_slice, "slice", 3);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_equalsIgnoreCase, "equalsIgnoreCase", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_match, "match", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_search, "search", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_replace, "replace", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_localeCompare, "localeCompare", 2);
		addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_toLocaleLowerCase, "toLocaleLowerCase", 1);
		super.fillConstructorProperties(ctor);
	}

	@Override
	protected void initPrototypeId(int id) {
		if (id == SymbolId_iterator) {
			initPrototypeMethod(STRING_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
			return;
		}

		String s, fnName = null;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 1;
				s = "constructor";
			}
			case Id_toString -> {
				arity = 0;
				s = "toString";
			}
			case Id_toSource -> {
				arity = 0;
				s = "toSource";
			}
			case Id_valueOf -> {
				arity = 0;
				s = "valueOf";
			}
			case Id_charAt -> {
				arity = 1;
				s = "charAt";
			}
			case Id_charCodeAt -> {
				arity = 1;
				s = "charCodeAt";
			}
			case Id_indexOf -> {
				arity = 1;
				s = "indexOf";
			}
			case Id_lastIndexOf -> {
				arity = 1;
				s = "lastIndexOf";
			}
			case Id_split -> {
				arity = 2;
				s = "split";
			}
			case Id_substring -> {
				arity = 2;
				s = "substring";
			}
			case Id_toLowerCase -> {
				arity = 0;
				s = "toLowerCase";
			}
			case Id_toUpperCase -> {
				arity = 0;
				s = "toUpperCase";
			}
			case Id_substr -> {
				arity = 2;
				s = "substr";
			}
			case Id_concat -> {
				arity = 1;
				s = "concat";
			}
			case Id_slice -> {
				arity = 2;
				s = "slice";
			}
			case Id_bold -> {
				arity = 0;
				s = "bold";
			}
			case Id_italics -> {
				arity = 0;
				s = "italics";
			}
			case Id_fixed -> {
				arity = 0;
				s = "fixed";
			}
			case Id_strike -> {
				arity = 0;
				s = "strike";
			}
			case Id_small -> {
				arity = 0;
				s = "small";
			}
			case Id_big -> {
				arity = 0;
				s = "big";
			}
			case Id_blink -> {
				arity = 0;
				s = "blink";
			}
			case Id_sup -> {
				arity = 0;
				s = "sup";
			}
			case Id_sub -> {
				arity = 0;
				s = "sub";
			}
			case Id_fontsize -> {
				arity = 0;
				s = "fontsize";
			}
			case Id_fontcolor -> {
				arity = 0;
				s = "fontcolor";
			}
			case Id_link -> {
				arity = 0;
				s = "link";
			}
			case Id_anchor -> {
				arity = 0;
				s = "anchor";
			}
			case Id_equals -> {
				arity = 1;
				s = "equals";
			}
			case Id_equalsIgnoreCase -> {
				arity = 1;
				s = "equalsIgnoreCase";
			}
			case Id_match -> {
				arity = 1;
				s = "match";
			}
			case Id_search -> {
				arity = 1;
				s = "search";
			}
			case Id_replace -> {
				arity = 2;
				s = "replace";
			}
			case Id_localeCompare -> {
				arity = 1;
				s = "localeCompare";
			}
			case Id_toLocaleLowerCase -> {
				arity = 0;
				s = "toLocaleLowerCase";
			}
			case Id_toLocaleUpperCase -> {
				arity = 0;
				s = "toLocaleUpperCase";
			}
			case Id_trim -> {
				arity = 0;
				s = "trim";
			}
			case Id_trimLeft -> {
				arity = 0;
				s = "trimLeft";
			}
			case Id_trimRight -> {
				arity = 0;
				s = "trimRight";
			}
			case Id_includes -> {
				arity = 1;
				s = "includes";
			}
			case Id_startsWith -> {
				arity = 1;
				s = "startsWith";
			}
			case Id_endsWith -> {
				arity = 1;
				s = "endsWith";
			}
			case Id_normalize -> {
				arity = 0;
				s = "normalize";
			}
			case Id_repeat -> {
				arity = 1;
				s = "repeat";
			}
			case Id_codePointAt -> {
				arity = 1;
				s = "codePointAt";
			}
			case Id_padStart -> {
				arity = 1;
				s = "padStart";
			}
			case Id_padEnd -> {
				arity = 1;
				s = "padEnd";
			}
			case Id_trimStart -> {
				arity = 0;
				s = "trimStart";
			}
			case Id_trimEnd -> {
				arity = 0;
				s = "trimEnd";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(STRING_TAG, id, s, fnName, arity);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(STRING_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		again:
		for (; ; ) {
			switch (id) {
				case ConstructorId_charAt:
				case ConstructorId_charCodeAt:
				case ConstructorId_indexOf:
				case ConstructorId_lastIndexOf:
				case ConstructorId_split:
				case ConstructorId_substring:
				case ConstructorId_toLowerCase:
				case ConstructorId_toUpperCase:
				case ConstructorId_substr:
				case ConstructorId_concat:
				case ConstructorId_slice:
				case ConstructorId_equalsIgnoreCase:
				case ConstructorId_match:
				case ConstructorId_search:
				case ConstructorId_replace:
				case ConstructorId_localeCompare:
				case ConstructorId_toLocaleLowerCase: {
					if (args.length > 0) {
						thisObj = ScriptRuntime.toObject(cx, scope, ScriptRuntime.toCharSequence(args[0]));
						Object[] newArgs = new Object[args.length - 1];
						System.arraycopy(args, 1, newArgs, 0, newArgs.length);
						args = newArgs;
					} else {
						thisObj = ScriptRuntime.toObject(cx, scope, ScriptRuntime.toCharSequence(thisObj));
					}
					id = -id;
					continue again;
				}

				case ConstructorId_fromCodePoint: {
					int n = args.length;
					if (n < 1) {
						return "";
					}
					int[] codePoints = new int[n];
					for (int i = 0; i != n; i++) {
						Object arg = args[i];
						int codePoint = ScriptRuntime.toInt32(arg);
						double num = ScriptRuntime.toNumber(arg);
						if (!ScriptRuntime.eqNumber(num, codePoint) || !Character.isValidCodePoint(codePoint)) {
							throw ScriptRuntime.rangeError("Invalid code point " + ScriptRuntime.toString(arg));
						}
						codePoints[i] = codePoint;
					}
					return new String(codePoints, 0, n);
				}

				case ConstructorId_fromCharCode: {
					int n = args.length;
					if (n < 1) {
						return "";
					}
					char[] chars = new char[n];
					for (int i = 0; i != n; ++i) {
						chars[i] = ScriptRuntime.toUint16(args[i]);
					}
					return new String(chars);
				}

				case ConstructorId_raw:
					return js_raw(cx, scope, args);

				case Id_constructor: {
					CharSequence s;
					if (args.length == 0) {
						s = "";
					} else if (ScriptRuntime.isSymbol(args[0]) && (thisObj != null)) {
						// 19.4.3.2 et.al. Convert a symbol to a string with String() but not new String()
						s = args[0].toString();
					} else {
						s = ScriptRuntime.toCharSequence(args[0]);
					}
					if (thisObj == null) {
						// new String(val) creates a new String object.
						return new NativeString(s);
					}
					// String(val) converts val to a string value.
					return s instanceof String ? s : s.toString();
				}

				case Id_toString:
				case Id_valueOf:
					// ECMA 15.5.4.2: 'the toString function is not generic.
					CharSequence cs = realThis(thisObj, f).string;
					return cs instanceof String ? cs : cs.toString();

				case Id_toSource: {
					return "not_supported";
				}

				case Id_charAt:
				case Id_charCodeAt: {
					// See ECMA 15.5.4.[4,5]
					CharSequence target = ScriptRuntime.toCharSequence(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					double pos = ScriptRuntime.toInteger(args, 0);
					if (pos < 0 || pos >= target.length()) {
						if (id == Id_charAt) {
							return "";
						}
						return ScriptRuntime.NaNobj;
					}
					char c = target.charAt((int) pos);
					if (id == Id_charAt) {
						return String.valueOf(c);
					}
					return ScriptRuntime.wrapInt(c);
				}

				case Id_indexOf: {
					String thisString = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return ScriptRuntime.wrapInt(js_indexOf(Id_indexOf, thisString, args));
				}

				case Id_includes:
				case Id_startsWith:
				case Id_endsWith:
					String thisString = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					if (args.length > 0 && args[0] instanceof NativeRegExp) {
						throw ScriptRuntime.typeError2("msg.first.arg.not.regexp", String.class.getSimpleName(), f.getFunctionName());
					}

					int idx = js_indexOf(id, thisString, args);

					if (id == Id_includes) {
						return idx != -1;
					}
					if (id == Id_startsWith) {
						return idx == 0;
					}
					return idx != -1;
				// fallthrough

				case Id_padStart:
				case Id_padEnd:
					return js_pad(cx, thisObj, f, args, id == Id_padStart);

				case Id_lastIndexOf: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return ScriptRuntime.wrapInt(js_lastIndexOf(thisStr, args));
				}

				case Id_split: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return ScriptRuntime.getRegExpProxy(cx).js_split(cx, scope, thisStr, args);
				}

				case Id_substring: {
					CharSequence target = ScriptRuntime.toCharSequence(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return js_substring(cx, target, args);
				}

				case Id_toLowerCase: {
					// See ECMA 15.5.4.11
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toLowerCase(Locale.ROOT);
				}

				case Id_toUpperCase: {
					// See ECMA 15.5.4.12
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toUpperCase(Locale.ROOT);
				}

				case Id_substr: {
					CharSequence target = ScriptRuntime.toCharSequence(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return js_substr(target, args);
				}

				case Id_concat: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return js_concat(thisStr, args);
				}

				case Id_slice: {
					CharSequence target = ScriptRuntime.toCharSequence(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return js_slice(target, args);
				}

				case Id_bold:
					return tagify(thisObj, "b", null, null);

				case Id_italics:
					return tagify(thisObj, "i", null, null);

				case Id_fixed:
					return tagify(thisObj, "tt", null, null);

				case Id_strike:
					return tagify(thisObj, "strike", null, null);

				case Id_small:
					return tagify(thisObj, "small", null, null);

				case Id_big:
					return tagify(thisObj, "big", null, null);

				case Id_blink:
					return tagify(thisObj, "blink", null, null);

				case Id_sup:
					return tagify(thisObj, "sup", null, null);

				case Id_sub:
					return tagify(thisObj, "sub", null, null);

				case Id_fontsize:
					return tagify(thisObj, "font", "size", args);

				case Id_fontcolor:
					return tagify(thisObj, "font", "color", args);

				case Id_link:
					return tagify(thisObj, "a", "href", args);

				case Id_anchor:
					return tagify(thisObj, "a", "name", args);

				case Id_equals:
				case Id_equalsIgnoreCase: {
					String s1 = ScriptRuntime.toString(thisObj);
					String s2 = ScriptRuntime.toString(args, 0);
					return ScriptRuntime.wrapBoolean((id == Id_equals) ? s1.equals(s2) : s1.equalsIgnoreCase(s2));
				}

				case Id_match:
				case Id_search:
				case Id_replace: {
					int actionType;
					if (id == Id_match) {
						actionType = RegExp.RA_MATCH;
					} else if (id == Id_search) {
						actionType = RegExp.RA_SEARCH;
					} else {
						actionType = RegExp.RA_REPLACE;
					}

					ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f);
					return ScriptRuntime.getRegExpProxy(cx).action(cx, scope, thisObj, args, actionType);
				}
				// ECMA-262 1 5.5.4.9
				case Id_localeCompare: {
					// For now, create and configure a collator instance. I can't
					// actually imagine that this'd be slower than caching them
					// a la ClassCache, so we aren't trying to outsmart ourselves
					// with a caching mechanism for now.
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					Collator collator = Collator.getInstance();
					collator.setStrength(Collator.IDENTICAL);
					collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
					return ScriptRuntime.wrapNumber(collator.compare(thisStr, ScriptRuntime.toString(args, 0)));
				}
				case Id_toLocaleLowerCase: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toLowerCase();
				}
				case Id_toLocaleUpperCase: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toUpperCase();
				}
				case Id_trim: {
					String str = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					char[] chars = str.toCharArray();

					int start = 0;
					while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
						start++;
					}
					int end = chars.length;
					while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
						end--;
					}

					return str.substring(start, end);
				}
				case Id_trimLeft:
				case Id_trimStart: {
					String str = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					char[] chars = str.toCharArray();

					int start = 0;
					while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
						start++;
					}
					int end = chars.length;

					return str.substring(start, end);
				}
				case Id_trimRight:
				case Id_trimEnd: {
					String str = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					char[] chars = str.toCharArray();

					int start = 0;

					int end = chars.length;
					while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
						end--;
					}

					return str.substring(start, end);
				}
				case Id_normalize: {
					if (args.length == 0 || Undefined.isUndefined(args[0])) {
						return Normalizer.normalize(ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f)), Normalizer.Form.NFC);
					}

					final String formStr = ScriptRuntime.toString(args, 0);

					final Normalizer.Form form;
					if (Normalizer.Form.NFD.name().equals(formStr)) {
						form = Normalizer.Form.NFD;
					} else if (Normalizer.Form.NFKC.name().equals(formStr)) {
						form = Normalizer.Form.NFKC;
					} else if (Normalizer.Form.NFKD.name().equals(formStr)) {
						form = Normalizer.Form.NFKD;
					} else if (Normalizer.Form.NFC.name().equals(formStr)) {
						form = Normalizer.Form.NFC;
					} else {
						throw ScriptRuntime.rangeError("The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.");
					}

					return Normalizer.normalize(ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f)), form);
				}

				case Id_repeat: {
					return js_repeat(cx, thisObj, f, args);
				}
				case Id_codePointAt: {
					String str = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					double cnt = ScriptRuntime.toInteger(args, 0);

					return (cnt < 0 || cnt >= str.length()) ? Undefined.instance : Integer.valueOf(str.codePointAt((int) cnt));
				}

				case SymbolId_iterator:
					return new NativeStringIterator(scope, ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));

			}
			throw new IllegalArgumentException("String.prototype has no method: " + f.getFunctionName());
		}
	}

	public CharSequence toCharSequence() {
		return string;
	}

	@Override
	public String toString() {
		return string.toString();
	}

	/* Make array-style property lookup work for strings.
	 * XXX is this ECMA?  A version check is probably needed. In js too.
	 */
	@Override
	public Object get(int index, Scriptable start) {
		if (0 <= index && index < string.length()) {
			return String.valueOf(string.charAt(index));
		}
		return super.get(index, start);
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		if (0 <= index && index < string.length()) {
			return;
		}
		super.put(index, start, value);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (0 <= index && index < string.length()) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public int getAttributes(int index) {
		if (0 <= index && index < string.length()) {
			return READONLY | PERMANENT;
		}
		return super.getAttributes(index);
	}

	@Override
	protected Object[] getIds(boolean nonEnumerable, boolean getSymbols) {
		// In ES6, Strings have entries in the property map for each character.
		Context cx = Context.getCurrentContext();
		if ((cx != null)) {
			Object[] sids = super.getIds(nonEnumerable, getSymbols);
			Object[] a = new Object[sids.length + string.length()];
			int i;
			for (i = 0; i < string.length(); i++) {
				a[i] = i;
			}
			System.arraycopy(sids, 0, a, i, sids.length);
			return a;
		}
		return super.getIds(nonEnumerable, getSymbols);
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (!(id instanceof Symbol) && (cx != null)) {
			ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, id);
			if (s.stringId == null && 0 <= s.index && s.index < string.length()) {
				String value = String.valueOf(string.charAt(s.index));
				return defaultIndexPropertyDescriptor(value);
			}
		}
		return super.getOwnPropertyDescriptor(cx, id);
	}

	private ScriptableObject defaultIndexPropertyDescriptor(Object value) {
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		ScriptableObject desc = new NativeObject();
		ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
		desc.defineProperty("value", value, EMPTY);
		desc.defineProperty("writable", Boolean.FALSE, EMPTY);
		desc.defineProperty("enumerable", Boolean.TRUE, EMPTY);
		desc.defineProperty("configurable", Boolean.FALSE, EMPTY);
		return desc;
	}

	int getLength() {
		return string.length();
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.ITERATOR.equals(k)) {
			return SymbolId_iterator;
		}
		return 0;
	}

	@Override
	protected int findPrototypeId(String s) {
		switch (s) {
			case "constructor": return Id_constructor;
			case "toString": return Id_toString;
			case "toSource": return Id_toSource;
			case "valueOf": return Id_valueOf;
			case "charAt": return Id_charAt;
			case "charCodeAt": return Id_charCodeAt;
			case "indexOf": return Id_indexOf;
			case "lastIndexOf": return Id_lastIndexOf;
			case "split": return Id_split;
			case "substring": return Id_substring;
			case "toLowerCase": return Id_toLowerCase;
			case "toUpperCase": return Id_toUpperCase;
			case "substr": return Id_substr;
			case "concat": return Id_concat;
			case "slice": return Id_slice;
			case "bold": return Id_bold;
			case "italics": return Id_italics;
			case "fixed": return Id_fixed;
			case "strike": return Id_strike;
			case "small": return Id_small;
			case "big": return Id_big;
			case "blink": return Id_blink;
			case "sup": return Id_sup;
			case "sub": return Id_sub;
			case "fontsize": return Id_fontsize;
			case "fontcolor": return Id_fontcolor;
			case "link": return Id_link;
			case "anchor": return Id_anchor;
			case "equals": return Id_equals;
			case "equalsIgnoreCase": return Id_equalsIgnoreCase;
			case "match": return Id_match;
			case "search": return Id_search;
			case "replace": return Id_replace;
			case "localeCompare": return Id_localeCompare;
			case "toLocaleLowerCase": return Id_toLocaleLowerCase;
			case "toLocaleUpperCase": return Id_toLocaleUpperCase;
			case "trim": return Id_trim;
			case "trimLeft": return Id_trimLeft;
			case "trimRight": return Id_trimRight;
			case "includes": return Id_includes;
			case "startsWith": return Id_startsWith;
			case "endsWith": return Id_endsWith;
			case "normalize": return Id_normalize;
			case "repeat": return Id_repeat;
			case "codePointAt": return Id_codePointAt;
			case "padStart": return Id_padStart;
			case "padEnd": return Id_padEnd;
			case "trimStart": return Id_trimStart;
			case "trimEnd": return Id_trimEnd;
			default: return super.findPrototypeId(s);
		}
	}
}

