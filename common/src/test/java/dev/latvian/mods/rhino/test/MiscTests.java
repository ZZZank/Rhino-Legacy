package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiscTests {

	public static final RhinoTest TEST = new RhinoTest("misc").shareScope();

	@Test
	@DisplayName("Init")
	@Order(1)
	public void init() {
		TEST.test(
			"init",
			String.join(
				"\n",
				"const testObject = {",
				"a: -39, b: 2, c: 3439438",
				"}",
				"",
				"let testList = console.testList",
				"",
				"for (let string of testList) {",
				"console.info(string)",
				"}"
			),
			String.join("\n", "abc", "def", "ghi")
		);
	}

	@Test
	@DisplayName("Test Array")
	public void testArray() {
		TEST.test(
			"testArray",
			String.join("\n", "for (let x of console.testArray) {", "console.info(x)", "}"),
			String.join("\n", "abc", "def", "ghi")
		);
	}

	@Test
	@DisplayName("Enums")
	public void enums() {
		TEST.test(
			"enums",
			String.join("\n", "console.theme = 'Dark'", "console.info(console.theme === 'DaRK')"),
			String.join("\n", "Set theme to DARK", "true")
		);
	}

	@Test
	@DisplayName("Array Length")
	@Order(2)
	public void arrayLength() {
		TEST.test(
			"arrayLength",
			String.join(
				"\n",
				"console.info('init ' + testList.length)",
				"testList.add('abcawidawidaiwdjawd')",
				"console.info('add ' + testList.length)",
				"testList.push('abcawidawidaiwdjawd')",
				"console.info('push ' + testList.length)"
			),
			String.join("\n", "init 3", "add 4", "push 5")
		);
	}

	@Test
	@DisplayName("Pop, Unshift, Map")
	@Order(3)
	public void popUnshiftMap() {
		TEST.test(
			"popUnshiftMap",
			String.join(
				"\n",
				"console.info('pop ' + testList.pop() + ' ' + testList.length)",
				"console.info('shift ' + testList.shift() + ' ' + testList.length)",
				"console.info('map ' + testList.concat(['xyz']).reverse().map(e => e.toUpperCase()).join(\" | \"))"
			),
			String.join(
				"\n",
				"pop abcawidawidaiwdjawd 4",
				"shift abc 3",
				"map XYZ | ABCAWIDAWIDAIWDJAWD | GHI | DEF"
			)
		);
	}

	@Test
	@DisplayName("Keys, Values, Entries")
	@Order(4)
	public void keysValuesEntries() {
		TEST.test(
			"keysValuesEntries",
			String.join(
				"\n",
				"console.info(Object.keys(testObject))",
				"console.info(Object.values(testObject))",
				"console.info(Object.entries(testObject))"
			),
			String.join(
				"\n",
				"[a, b, c]",
				"[-39.0, 2.0, 3439438.0]",
				"[[a, -39.0], [b, 2.0], [c, 3439438.0]]"
			)
		);
	}

	@Test
	@DisplayName("Deconstruction")
	@Order(4)
	public void deconstruction() {
		TEST.test(
			"deconstruction",
			String.join(
				"\n",
				"for (let [key, value] of Object.entries(testObject)) {",
				"console.info(`${key} : ${value}`)",
				"}"
			),
			String.join("\n", "a : -39", "b : 2", "c : 3439438")
		);
	}
}
