/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.regexp.NativeRegExp;

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
final class NativeString extends IdScriptableObject {
	private static final long serialVersionUID = 920268368584188687L;

	private static final Object STRING_TAG = "String";

	static void init(Scriptable scope, boolean sealed) {
		NativeString obj = new NativeString("");
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
	}

	NativeString(CharSequence s) {
		string = s;
	}

	@Override
	public String getClassName() {
		return "String";
	}

	private static final int Id_length = 1, Id_namespace = 2, Id_path = 3, MAX_INSTANCE_ID = Id_path;

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected int findInstanceIdInfo(String s) {
        return switch (s) {
            case "length" -> instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_length);
            case "namespace" -> instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_namespace);
            case "path" -> instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_path);
            default -> super.findInstanceIdInfo(s);
        };
    }

	@Override
	protected String getInstanceIdName(int id) {
        return switch (id) {
            case Id_length -> "length";
            case Id_namespace -> "namespace";
            case Id_path -> "path";
            default -> super.getInstanceIdName(id);
        };
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
        s = switch (id) {
            case Id_constructor -> {
                arity = 1;
                yield "constructor";
            }
            case Id_toString -> {
                arity = 0;
                yield "toString";
            }
            case Id_toSource -> {
                arity = 0;
                yield "toSource";
            }
            case Id_valueOf -> {
                arity = 0;
                yield "valueOf";
            }
            case Id_charAt -> {
                arity = 1;
                yield "charAt";
            }
            case Id_charCodeAt -> {
                arity = 1;
                yield "charCodeAt";
            }
            case Id_indexOf -> {
                arity = 1;
                yield "indexOf";
            }
            case Id_lastIndexOf -> {
                arity = 1;
                yield "lastIndexOf";
            }
            case Id_split -> {
                arity = 2;
                yield "split";
            }
            case Id_substring -> {
                arity = 2;
                yield "substring";
            }
            case Id_toLowerCase -> {
                arity = 0;
                yield "toLowerCase";
            }
            case Id_toUpperCase -> {
                arity = 0;
                yield "toUpperCase";
            }
            case Id_substr -> {
                arity = 2;
                yield "substr";
            }
            case Id_concat -> {
                arity = 1;
                yield "concat";
            }
            case Id_slice -> {
                arity = 2;
                yield "slice";
            }
            case Id_bold -> {
                arity = 0;
                yield "bold";
            }
            case Id_italics -> {
                arity = 0;
                yield "italics";
            }
            case Id_fixed -> {
                arity = 0;
                yield "fixed";
            }
            case Id_strike -> {
                arity = 0;
                yield "strike";
            }
            case Id_small -> {
                arity = 0;
                yield "small";
            }
            case Id_big -> {
                arity = 0;
                yield "big";
            }
            case Id_blink -> {
                arity = 0;
                yield "blink";
            }
            case Id_sup -> {
                arity = 0;
                yield "sup";
            }
            case Id_sub -> {
                arity = 0;
                yield "sub";
            }
            case Id_fontsize -> {
                arity = 0;
                yield "fontsize";
            }
            case Id_fontcolor -> {
                arity = 0;
                yield "fontcolor";
            }
            case Id_link -> {
                arity = 0;
                yield "link";
            }
            case Id_anchor -> {
                arity = 0;
                yield "anchor";
            }
            case Id_equals -> {
                arity = 1;
                yield "equals";
            }
            case Id_equalsIgnoreCase -> {
                arity = 1;
                yield "equalsIgnoreCase";
            }
            case Id_match -> {
                arity = 1;
                yield "match";
            }
            case Id_search -> {
                arity = 1;
                yield "search";
            }
            case Id_replace -> {
                arity = 2;
                yield "replace";
            }
            case Id_localeCompare -> {
                arity = 1;
                yield "localeCompare";
            }
            case Id_toLocaleLowerCase -> {
                arity = 0;
                yield "toLocaleLowerCase";
            }
            case Id_toLocaleUpperCase -> {
                arity = 0;
                yield "toLocaleUpperCase";
            }
            case Id_trim -> {
                arity = 0;
                yield "trim";
            }
            case Id_trimLeft -> {
                arity = 0;
                yield "trimLeft";
            }
            case Id_trimRight -> {
                arity = 0;
                yield "trimRight";
            }
            case Id_includes -> {
                arity = 1;
                yield "includes";
            }
            case Id_startsWith -> {
                arity = 1;
                yield "startsWith";
            }
            case Id_endsWith -> {
                arity = 1;
                yield "endsWith";
            }
            case Id_normalize -> {
                arity = 0;
                yield "normalize";
            }
            case Id_repeat -> {
                arity = 1;
                yield "repeat";
            }
            case Id_codePointAt -> {
                arity = 1;
                yield "codePointAt";
            }
            case Id_padStart -> {
                arity = 1;
                yield "padStart";
            }
            case Id_padEnd -> {
                arity = 1;
                yield "padEnd";
            }
            case Id_trimStart -> {
                arity = 0;
                yield "trimStart";
            }
            case Id_trimEnd -> {
                arity = 0;
                yield "trimEnd";
            }
            default -> throw new IllegalArgumentException(String.valueOf(id));
        };
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
					return ScriptRuntime.checkRegExpProxy(cx).js_split(cx, scope, thisStr, args);
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
						actionType = RegExpProxy.RA_MATCH;
					} else if (id == Id_search) {
						actionType = RegExpProxy.RA_SEARCH;
					} else {
						actionType = RegExpProxy.RA_REPLACE;
					}

					ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f);
					return ScriptRuntime.checkRegExpProxy(cx).action(cx, scope, thisObj, args, actionType);
				}
				// ECMA-262 1 5.5.4.9
				case Id_localeCompare: {
					// For now, create and configure a collator instance. I can't
					// actually imagine that this'd be slower than caching them
					// a la ClassCache, so we aren't trying to outsmart ourselves
					// with a caching mechanism for now.
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					Collator collator = Collator.getInstance(cx.getLocale());
					collator.setStrength(Collator.IDENTICAL);
					collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
					return ScriptRuntime.wrapNumber(collator.compare(thisStr, ScriptRuntime.toString(args, 0)));
				}
				case Id_toLocaleLowerCase: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toLowerCase(cx.getLocale());
				}
				case Id_toLocaleUpperCase: {
					String thisStr = ScriptRuntime.toString(ScriptRuntimeES6.requireObjectCoercible(cx, thisObj, f));
					return thisStr.toUpperCase(cx.getLocale());
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

	public CharSequence toCharSequence() {
		return string;
	}

	@Override
	public String toString() {
		return string instanceof String ? (String) string : string.toString();
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

	int getLength() {
		return string.length();
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

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.ITERATOR.equals(k)) {
			return SymbolId_iterator;
		}
		return 0;
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

	// #string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		int id;
		// #generated# Last update: 2020-06-27 11:15:47 CST
		L0:
		{
			id = 0;
			String X = null;
			int c;
			L:
			switch (s.length()) {
				case 3:
					c = s.charAt(2);
					if (c == 'b') {
						if (s.charAt(0) == 's' && s.charAt(1) == 'u') {
							id = Id_sub;
							break L0;
						}
					} else if (c == 'g') {
						if (s.charAt(0) == 'b' && s.charAt(1) == 'i') {
							id = Id_big;
							break L0;
						}
					} else if (c == 'p') {
						if (s.charAt(0) == 's' && s.charAt(1) == 'u') {
							id = Id_sup;
							break L0;
						}
					}
					break L;
				case 4:
					c = s.charAt(0);
					if (c == 'b') {
						X = "bold";
						id = Id_bold;
					} else if (c == 'l') {
						X = "link";
						id = Id_link;
					} else if (c == 't') {
						X = "trim";
						id = Id_trim;
					}
					break L;
				case 5:
					switch (s.charAt(4)) {
						case 'd':
							X = "fixed";
							id = Id_fixed;
							break L;
						case 'e':
							X = "slice";
							id = Id_slice;
							break L;
						case 'h':
							X = "match";
							id = Id_match;
							break L;
						case 'k':
							X = "blink";
							id = Id_blink;
							break L;
						case 'l':
							X = "small";
							id = Id_small;
							break L;
						case 't':
							X = "split";
							id = Id_split;
							break L;
					}
					break L;
				case 6:
					switch (s.charAt(1)) {
						case 'a':
							X = "padEnd";
							id = Id_padEnd;
							break L;
						case 'e':
							c = s.charAt(0);
							if (c == 'r') {
								X = "repeat";
								id = Id_repeat;
							} else if (c == 's') {
								X = "search";
								id = Id_search;
							}
							break L;
						case 'h':
							X = "charAt";
							id = Id_charAt;
							break L;
						case 'n':
							X = "anchor";
							id = Id_anchor;
							break L;
						case 'o':
							X = "concat";
							id = Id_concat;
							break L;
						case 'q':
							X = "equals";
							id = Id_equals;
							break L;
						case 't':
							X = "strike";
							id = Id_strike;
							break L;
						case 'u':
							X = "substr";
							id = Id_substr;
							break L;
					}
					break L;
				case 7:
					switch (s.charAt(1)) {
						case 'a':
							X = "valueOf";
							id = Id_valueOf;
							break L;
						case 'e':
							X = "replace";
							id = Id_replace;
							break L;
						case 'n':
							X = "indexOf";
							id = Id_indexOf;
							break L;
						case 'r':
							X = "trimEnd";
							id = Id_trimEnd;
							break L;
						case 't':
							X = "italics";
							id = Id_italics;
							break L;
					}
					break L;
				case 8:
					switch (s.charAt(6)) {
						case 'c':
							X = "toSource";
							id = Id_toSource;
							break L;
						case 'e':
							X = "includes";
							id = Id_includes;
							break L;
						case 'f':
							X = "trimLeft";
							id = Id_trimLeft;
							break L;
						case 'n':
							X = "toString";
							id = Id_toString;
							break L;
						case 'r':
							X = "padStart";
							id = Id_padStart;
							break L;
						case 't':
							X = "endsWith";
							id = Id_endsWith;
							break L;
						case 'z':
							X = "fontsize";
							id = Id_fontsize;
							break L;
					}
					break L;
				case 9:
					switch (s.charAt(4)) {
						case 'R':
							X = "trimRight";
							id = Id_trimRight;
							break L;
						case 'S':
							X = "trimStart";
							id = Id_trimStart;
							break L;
						case 'a':
							X = "normalize";
							id = Id_normalize;
							break L;
						case 'c':
							X = "fontcolor";
							id = Id_fontcolor;
							break L;
						case 't':
							X = "substring";
							id = Id_substring;
							break L;
						case 's':
							X = "namespace";
							id = Id_namespace;
							break L;
					}
					break L;
				case 10:
					c = s.charAt(0);
					if (c == 'c') {
						X = "charCodeAt";
						id = Id_charCodeAt;
					} else if (c == 's') {
						X = "startsWith";
						id = Id_startsWith;
					}
					break L;
				case 11:
					switch (s.charAt(2)) {
						case 'L':
							X = "toLowerCase";
							id = Id_toLowerCase;
							break L;
						case 'U':
							X = "toUpperCase";
							id = Id_toUpperCase;
							break L;
						case 'd':
							X = "codePointAt";
							id = Id_codePointAt;
							break L;
						case 'n':
							X = "constructor";
							id = Id_constructor;
							break L;
						case 's':
							X = "lastIndexOf";
							id = Id_lastIndexOf;
							break L;
					}
					break L;
				case 13:
					X = "localeCompare";
					id = Id_localeCompare;
					break L;
				case 16:
					X = "equalsIgnoreCase";
					id = Id_equalsIgnoreCase;
					break L;
				case 17:
					c = s.charAt(8);
					if (c == 'L') {
						X = "toLocaleLowerCase";
						id = Id_toLocaleLowerCase;
					} else if (c == 'U') {
						X = "toLocaleUpperCase";
						id = Id_toLocaleUpperCase;
					}
					break L;
			}
			if (X != null && X != s && !X.equals(s)) {
				id = 0;
			}
			break L0;
		}
		// #/generated#
		return id;
	}

	private static final int ConstructorId_fromCharCode = -1, ConstructorId_fromCodePoint = -2, ConstructorId_raw = -3,

	Id_constructor = 1, Id_toString = 2, Id_toSource = 3, Id_valueOf = 4, Id_charAt = 5, Id_charCodeAt = 6, Id_indexOf = 7, Id_lastIndexOf = 8, Id_split = 9, Id_substring = 10, Id_toLowerCase = 11, Id_toUpperCase = 12, Id_substr = 13, Id_concat = 14, Id_slice = 15, Id_bold = 16, Id_italics = 17, Id_fixed = 18, Id_strike = 19, Id_small = 20, Id_big = 21, Id_blink = 22, Id_sup = 23, Id_sub = 24, Id_fontsize = 25, Id_fontcolor = 26, Id_link = 27, Id_anchor = 28, Id_equals = 29, Id_equalsIgnoreCase = 30, Id_match = 31, Id_search = 32, Id_replace = 33, Id_localeCompare = 34, Id_toLocaleLowerCase = 35, Id_toLocaleUpperCase = 36, Id_trim = 37, Id_trimLeft = 38, Id_trimRight = 39, Id_includes = 40, Id_startsWith = 41, Id_endsWith = 42, Id_normalize = 43, Id_repeat = 44, Id_codePointAt = 45, Id_padStart = 46, Id_padEnd = 47, SymbolId_iterator = 48, Id_trimStart = 49, Id_trimEnd = 50, MAX_PROTOTYPE_ID = Id_trimEnd;

	// #/string_id_map#

	private static final int ConstructorId_charAt = -Id_charAt, ConstructorId_charCodeAt = -Id_charCodeAt, ConstructorId_indexOf = -Id_indexOf, ConstructorId_lastIndexOf = -Id_lastIndexOf, ConstructorId_split = -Id_split, ConstructorId_substring = -Id_substring, ConstructorId_toLowerCase = -Id_toLowerCase, ConstructorId_toUpperCase = -Id_toUpperCase, ConstructorId_substr = -Id_substr, ConstructorId_concat = -Id_concat, ConstructorId_slice = -Id_slice, ConstructorId_equalsIgnoreCase = -Id_equalsIgnoreCase, ConstructorId_match = -Id_match, ConstructorId_search = -Id_search, ConstructorId_replace = -Id_replace, ConstructorId_localeCompare = -Id_localeCompare, ConstructorId_toLocaleLowerCase = -Id_toLocaleLowerCase;

	private final CharSequence string;
}

