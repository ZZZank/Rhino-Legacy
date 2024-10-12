/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the JavaScript scanner.
 * <p>
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @author Mike McCabe
 * @author Brendan Eich
 * @see Parser
 */

public interface Token {
	enum CommentType {
		LINE, BLOCK_COMMENT, JSDOC, HTML
	}

	// debug flags
	boolean printTrees = false;
	boolean printICode = false;
	boolean printNames = printTrees || printICode;

	/**
	 * Token types.  These values correspond to JSTokenType values in
	 * jsscan.c.
	 */

    // start enum
    int ERROR = -1; // well-known as the only code < EOF
    int EOF = 0;  // end of file token - (not EOF_CHAR)
    int EOL = 1;  // end of line

    // Interpreter reuses the following as bytecodes
    int FIRST_BYTECODE_TOKEN = 2;

    int ENTERWITH = 2;
    int LEAVEWITH = ENTERWITH + 1;
    int RETURN = LEAVEWITH + 1;
    int GOTO = RETURN + 1;
    int IFEQ = GOTO + 1;
    int IFNE = IFEQ + 1;
    int SETNAME = IFNE + 1;
    int BITOR = SETNAME + 1;
    int BITXOR = BITOR + 1;
    int BITAND = BITXOR + 1;
    int EQ = BITAND + 1;
    int NE = EQ + 1;
    int LT = NE + 1;
    int LE = LT + 1;
    int GT = LE + 1;
    int GE = GT + 1;
    int LSH = GE + 1;
    int RSH = LSH + 1;
    int URSH = RSH + 1;
    int ADD = URSH + 1;
    int SUB = ADD + 1;
    int MUL = SUB + 1;
    int DIV = MUL + 1;
    int MOD = DIV + 1;
    int NOT = MOD + 1;
    int BITNOT = NOT + 1;
    int POS = BITNOT + 1;
    int NEG = POS + 1;
    int NEW = NEG + 1;
    int DELPROP = NEW + 1;
    int TYPEOF = DELPROP + 1;
    int GETPROP = TYPEOF + 1;
    int GETPROPNOWARN = GETPROP + 1;
    int SETPROP = GETPROPNOWARN + 1;
    int GETELEM = SETPROP + 1;
    int SETELEM = GETELEM + 1;
    int CALL = SETELEM + 1;
    int NAME = CALL + 1;
    int NUMBER = NAME + 1;
    int STRING = NUMBER + 1;
    int NULL = STRING + 1;
    int THIS = NULL + 1;
    int FALSE = THIS + 1;
    int TRUE = FALSE + 1;
    int SHEQ = TRUE + 1;   // shallow equality (===)
    int SHNE = SHEQ + 1;   // shallow inequality (!==)
    int REGEXP = SHNE + 1;
    int BINDNAME = REGEXP + 1;
    int THROW = BINDNAME + 1;
    int RETHROW = THROW + 1; // rethrow caught exception: catch (e if ) use it
    int IN = RETHROW + 1;
    int INSTANCEOF = IN + 1;
    int LOCAL_LOAD = INSTANCEOF + 1;
    int GETVAR = LOCAL_LOAD + 1;
    int SETVAR = GETVAR + 1;
    int CATCH_SCOPE = SETVAR + 1;
    int ENUM_INIT_KEYS = CATCH_SCOPE + 1;
    int ENUM_INIT_VALUES = ENUM_INIT_KEYS + 1;
    int ENUM_INIT_ARRAY = ENUM_INIT_VALUES + 1;
    int ENUM_INIT_VALUES_IN_ORDER = ENUM_INIT_ARRAY + 1;
    int ENUM_NEXT = ENUM_INIT_VALUES_IN_ORDER + 1;
    int ENUM_ID = ENUM_NEXT + 1;
    int THISFN = ENUM_ID + 1;
    int RETURN_RESULT = THISFN + 1; // to return previously stored return result
    int ARRAYLIT = RETURN_RESULT + 1; // array literal
    int OBJECTLIT = ARRAYLIT + 1; // object literal
    int GET_REF = OBJECTLIT + 1; // *reference
    int SET_REF = GET_REF + 1; // *reference    = something
    int DEL_REF = SET_REF + 1; // delete reference
    int REF_CALL = DEL_REF + 1; // f(args)    = something or f(args)++
    int REF_SPECIAL = REF_CALL + 1; // reference for special properties like __proto
    int YIELD = REF_SPECIAL + 1;  // JS 1.7 yield pseudo keyword
    int STRICT_SETNAME = YIELD + 1;
    int NULLISH_COALESCING = STRICT_SETNAME + 1; // nullish coalescing operator (??)
    int OPTIONAL_CHAINING = NULLISH_COALESCING + 1; // optional chaining operator (?.), type is GETOPTIONAL
    int GETOPTIONAL = OPTIONAL_CHAINING + 1;

    // End of interpreter bytecodes
    int LAST_BYTECODE_TOKEN = GETOPTIONAL;

    int TRY = LAST_BYTECODE_TOKEN + 1;
    int SEMI = TRY + 1;  // semicolon
    int LB = SEMI + 1;  // left and right brackets
    int RB = LB + 1;
    int LC = RB + 1;  // left and right curlies (braces)
    int RC = LC + 1;
    int LP = RC + 1;  // left and right parentheses
    int RP = LP + 1;
    int COMMA = RP + 1;  // comma operator

    int ASSIGN = COMMA + 1;  // simple assignment  (=)
    int ASSIGN_BITOR = ASSIGN + 1;  // |=
    int ASSIGN_BITXOR = ASSIGN_BITOR + 1;  // ^=
    int ASSIGN_BITAND = ASSIGN_BITXOR + 1;  // |=
    int ASSIGN_LSH = ASSIGN_BITAND + 1;  // <<=
    int ASSIGN_RSH = ASSIGN_LSH + 1;  // >>=
    int ASSIGN_URSH = ASSIGN_RSH + 1;  // >>>=
    int ASSIGN_ADD = ASSIGN_URSH + 1;  // +=
    int ASSIGN_SUB = ASSIGN_ADD + 1;  // -=
    int ASSIGN_MUL = ASSIGN_SUB + 1;  // *=
    int ASSIGN_DIV = ASSIGN_MUL + 1;  // /=
    int ASSIGN_MOD = ASSIGN_DIV + 1;  // %=

    int FIRST_ASSIGN = ASSIGN;
    int LAST_ASSIGN = ASSIGN_MOD;

    int HOOK = LAST_ASSIGN + 1; // tri-conditional (bool ? a : b)
    int COLON = HOOK + 1;
    int OR = COLON + 1; // logical or (||)
    int AND = OR + 1; // logical and (&&)
    int INC = AND + 1; // increment/decrement (++ --)
    int DEC = INC + 1;
    int DOT = DEC + 1; // member operator (.)
    int FUNCTION = DOT + 1; // function keyword
    int EXPORT = FUNCTION + 1; // export keyword
    int IMPORT = EXPORT + 1; // import keyword
    int IF = IMPORT + 1; // if keyword
    int ELSE = IF + 1; // else keyword
    int SWITCH = ELSE + 1; // switch keyword
    int CASE = SWITCH + 1; // case keyword
    int DEFAULT = CASE + 1; // default keyword
    int WHILE = DEFAULT + 1; // while keyword
    int DO = WHILE + 1; // do keyword
    int FOR = DO + 1; // for keyword
    int BREAK = FOR + 1; // break keyword
    int CONTINUE = BREAK + 1; // continue keyword
    int VAR = CONTINUE + 1; // var keyword
    int WITH = VAR + 1; // with keyword
    int CATCH = WITH + 1; // catch keyword
    int FINALLY = CATCH + 1; // finally keyword
    int VOID = FINALLY + 1; // void keyword
    int RESERVED = VOID + 1; // reserved keywords

    int EMPTY = RESERVED + 1;

    // types used for the parse tree - these never get returned  by the scanner.
    int BLOCK = EMPTY + 1; // statement block
    int LABEL = BLOCK + 1; // label
    int TARGET = LABEL + 1;
    int LOOP = TARGET + 1;
    int EXPR_VOID = LOOP + 1; // expression statement in functions
    int EXPR_RESULT = EXPR_VOID + 1; // expression statement in scripts
    int JSR = EXPR_RESULT + 1;
    int SCRIPT = JSR + 1; // top-level node for entire script
    int TYPEOFNAME = SCRIPT + 1; // for typeof(simple-name)
    int USE_STACK = TYPEOFNAME + 1;
    int SETPROP_OP = USE_STACK + 1; // x.y op= something
    int SETELEM_OP = SETPROP_OP + 1; // x[y] op= something
    int LOCAL_BLOCK = SETELEM_OP + 1;
    int SET_REF_OP = LOCAL_BLOCK + 1; // *reference op= something

    // Optimizer-only-tokens
    int TO_OBJECT = SET_REF_OP + 1;
    int TO_DOUBLE = TO_OBJECT + 1;

    int GET = TO_DOUBLE + 1;  // JS 1.5 get pseudo keyword
    int SET = GET + 1;  // JS 1.5 set pseudo keyword
    int LET = SET + 1;  // JS 1.7 let pseudo keyword
    int CONST = LET + 1;
    int SETCONST = CONST + 1;
    int SETCONSTVAR = SETCONST + 1;
    int ARRAYCOMP = SETCONSTVAR + 1;  // array comprehension
    int LETEXPR = ARRAYCOMP + 1;
    int WITHEXPR = LETEXPR + 1;
    // int DEBUGGER = 161;
    int COMMENT = WITHEXPR + 1;
    int GENEXPR = COMMENT + 1;
    int METHOD = GENEXPR + 1;  // ES6 MethodDefinition
    int ARROW = METHOD + 1;  // ES6 ArrowFunction
    int YIELD_STAR = ARROW + 1;  // ES6 "yield *", a specialization of yield
    int TEMPLATE_LITERAL = YIELD_STAR + 1;  // template literal
    int TEMPLATE_CHARS = TEMPLATE_LITERAL + 1;  // template literal - literal section
    int TEMPLATE_LITERAL_SUBST = TEMPLATE_CHARS + 1;  // template literal - substitution
    int TAGGED_TEMPLATE_LITERAL = TEMPLATE_LITERAL_SUBST + 1;  // template literal - tagged/handler
    int DOTDOTDOT = TAGGED_TEMPLATE_LITERAL + 1; // spread/rest ...

    int LAST_TOKEN = DOTDOTDOT;

    /**
	 * Returns a name for the token.  If Rhino is compiled with certain
	 * hardcoded debugging flags in this file, it calls {@code #typeToName};
	 * otherwise it returns a string whose value is the token number.
	 */
	static String name(int token) {
		if (!printNames) {
			return String.valueOf(token);
		}
		return typeToName(token);
	}

	/**
	 * Always returns a human-readable string for the token name.
	 * For instance, {@link #FINALLY} has the name "FINALLY".
	 *
	 * @param token the token code
	 * @return the actual name for the token code
	 */
	static String typeToName(int token) {
        return switch (token) {
            case ERROR -> "ERROR";
            case EOF -> "EOF";
            case EOL -> "EOL";
            case ENTERWITH -> "ENTERWITH";
            case LEAVEWITH -> "LEAVEWITH";
            case RETURN -> "RETURN";
            case GOTO -> "GOTO";
            case IFEQ -> "IFEQ";
            case IFNE -> "IFNE";
            case SETNAME -> "SETNAME";
            case BITOR -> "BITOR";
            case BITXOR -> "BITXOR";
            case BITAND -> "BITAND";
            case EQ -> "EQ";
            case NE -> "NE";
            case LT -> "LT";
            case LE -> "LE";
            case GT -> "GT";
            case GE -> "GE";
            case LSH -> "LSH";
            case RSH -> "RSH";
            case URSH -> "URSH";
            case ADD -> "ADD";
            case SUB -> "SUB";
            case MUL -> "MUL";
            case DIV -> "DIV";
            case MOD -> "MOD";
            case NOT -> "NOT";
            case BITNOT -> "BITNOT";
            case POS -> "POS";
            case NEG -> "NEG";
            case NEW -> "NEW";
            case DELPROP -> "DELPROP";
            case TYPEOF -> "TYPEOF";
            case GETPROP -> "GETPROP";
            case GETPROPNOWARN -> "GETPROPNOWARN";
            case SETPROP -> "SETPROP";
            case GETELEM -> "GETELEM";
            case SETELEM -> "SETELEM";
            case CALL -> "CALL";
            case NAME -> "NAME";
            case NUMBER -> "NUMBER";
            case STRING -> "STRING";
            case NULL -> "NULL";
            case THIS -> "THIS";
            case FALSE -> "FALSE";
            case TRUE -> "TRUE";
            case SHEQ -> "SHEQ";
            case SHNE -> "SHNE";
            case REGEXP -> "REGEXP";
            case BINDNAME -> "BINDNAME";
            case THROW -> "THROW";
            case RETHROW -> "RETHROW";
            case IN -> "IN";
            case INSTANCEOF -> "INSTANCEOF";
            case LOCAL_LOAD -> "LOCAL_LOAD";
            case GETVAR -> "GETVAR";
            case SETVAR -> "SETVAR";
            case CATCH_SCOPE -> "CATCH_SCOPE";
            case ENUM_INIT_KEYS -> "ENUM_INIT_KEYS";
            case ENUM_INIT_VALUES -> "ENUM_INIT_VALUES";
            case ENUM_INIT_ARRAY -> "ENUM_INIT_ARRAY";
            case ENUM_INIT_VALUES_IN_ORDER -> "ENUM_INIT_VALUES_IN_ORDER";
            case ENUM_NEXT -> "ENUM_NEXT";
            case ENUM_ID -> "ENUM_ID";
            case THISFN -> "THISFN";
            case RETURN_RESULT -> "RETURN_RESULT";
            case ARRAYLIT -> "ARRAYLIT";
            case OBJECTLIT -> "OBJECTLIT";
            case GET_REF -> "GET_REF";
            case SET_REF -> "SET_REF";
            case DEL_REF -> "DEL_REF";
            case REF_CALL -> "REF_CALL";
            case REF_SPECIAL -> "REF_SPECIAL";
            case TRY -> "TRY";
            case SEMI -> "SEMI";
            case LB -> "LB";
            case RB -> "RB";
            case LC -> "LC";
            case RC -> "RC";
            case LP -> "LP";
            case RP -> "RP";
            case COMMA -> "COMMA";
            case ASSIGN -> "ASSIGN";
            case ASSIGN_BITOR -> "ASSIGN_BITOR";
            case ASSIGN_BITXOR -> "ASSIGN_BITXOR";
            case ASSIGN_BITAND -> "ASSIGN_BITAND";
            case ASSIGN_LSH -> "ASSIGN_LSH";
            case ASSIGN_RSH -> "ASSIGN_RSH";
            case ASSIGN_URSH -> "ASSIGN_URSH";
            case ASSIGN_ADD -> "ASSIGN_ADD";
            case ASSIGN_SUB -> "ASSIGN_SUB";
            case ASSIGN_MUL -> "ASSIGN_MUL";
            case ASSIGN_DIV -> "ASSIGN_DIV";
            case ASSIGN_MOD -> "ASSIGN_MOD";
            case HOOK -> "HOOK";
            case COLON -> "COLON";
            case OR -> "OR";
            case AND -> "AND";
            case INC -> "INC";
            case DEC -> "DEC";
            case DOT -> "DOT";
            case FUNCTION -> "FUNCTION";
            case EXPORT -> "EXPORT";
            case IMPORT -> "IMPORT";
            case IF -> "IF";
            case ELSE -> "ELSE";
            case SWITCH -> "SWITCH";
            case CASE -> "CASE";
            case DEFAULT -> "DEFAULT";
            case WHILE -> "WHILE";
            case DO -> "DO";
            case FOR -> "FOR";
            case BREAK -> "BREAK";
            case CONTINUE -> "CONTINUE";
            case VAR -> "VAR";
            case WITH -> "WITH";
            case CATCH -> "CATCH";
            case FINALLY -> "FINALLY";
            case VOID -> "VOID";
            case RESERVED -> "RESERVED";
            case EMPTY -> "EMPTY";
            case BLOCK -> "BLOCK";
            case LABEL -> "LABEL";
            case TARGET -> "TARGET";
            case LOOP -> "LOOP";
            case EXPR_VOID -> "EXPR_VOID";
            case EXPR_RESULT -> "EXPR_RESULT";
            case JSR -> "JSR";
            case SCRIPT -> "SCRIPT";
            case TYPEOFNAME -> "TYPEOFNAME";
            case USE_STACK -> "USE_STACK";
            case SETPROP_OP -> "SETPROP_OP";
            case SETELEM_OP -> "SETELEM_OP";
            case LOCAL_BLOCK -> "LOCAL_BLOCK";
            case SET_REF_OP -> "SET_REF_OP";
            case TO_OBJECT -> "TO_OBJECT";
            case TO_DOUBLE -> "TO_DOUBLE";
            case GET -> "GET";
            case SET -> "SET";
            case LET -> "LET";
            case YIELD -> "YIELD";
            case CONST -> "CONST";
            case SETCONST -> "SETCONST";
            case ARRAYCOMP -> "ARRAYCOMP";
            case WITHEXPR -> "WITHEXPR";
            case LETEXPR -> "LETEXPR";
            case COMMENT -> "COMMENT";
            case GENEXPR -> "GENEXPR";
            case METHOD -> "METHOD";
            case ARROW -> "ARROW";
            case YIELD_STAR -> "YIELD_STAR";
            case TEMPLATE_LITERAL -> "TEMPLATE_LITERAL";
            case TEMPLATE_CHARS -> "TEMPLATE_CHARS";
            case TEMPLATE_LITERAL_SUBST -> "TEMPLATE_LITERAL_SUBST";
			case TAGGED_TEMPLATE_LITERAL -> "TAGGED_TEMPLATE_LITERAL";
			case NULLISH_COALESCING -> "NULLISH_COALESCING";
			case OPTIONAL_CHAINING -> "OPTIONAL_CHAINING";
			case GETOPTIONAL -> "GETOPTIONAL";
            default ->
                // Token without name
                throw new IllegalStateException(String.valueOf(token));
        };

    }

	/**
	 * Return true if the passed code is a valid Token constant.
	 *
	 * @param code a potential token code
	 * @return true if it's a known token
	 */
	static boolean isValidToken(int code) {
		return code >= ERROR && code <= LAST_TOKEN;
	}
}
