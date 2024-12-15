package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.test.example.FnOverload;
import dev.latvian.mods.rhino.test.example.generic.GenerBase;
import dev.latvian.mods.rhino.test.example.generic.Impl1;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;
import lombok.val;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RhinoTest {
	public static void main(String[] args) {
		Context context = Context.enterWithNewFactory();
		// context.setClassShutter((fullClassName, type) -> type != ClassShutter.TYPE_CLASS_IN_PACKAGE || isClassAllowed(fullClassName));
		context.getTypeWrappers().register(
			GenerBase.class,
            (TypeWrapperFactory.New<GenerBase>) (cx, o, to) -> o instanceof CharSequence ? new Impl1(o.toString()) : null
		);
		RhinoTest test = new RhinoTest(context);
		test.add("console", TestConsole.class);
		test.add("overload", FnOverload.class);
		test.add("genericWrap", GenerBase.class);

		test.eval("init.js", """
			const log = console.log""");

		test.evalNewScope("gener.js", """
			const impl = genericWrap.ofGenericInReturn();
			log(impl)
			log(impl.getClass())
			impl.accept("try to wrap it")
			""");

		val result = test.eval("fn_interfaces.js",
			"""
			log(overload.of("nice"))
			log(overload.of("nice1", "nice2?"))
			overload.of("nice", (str)=>{
				log(str)
			})""");
//		test.load("/rhinotest/test.js");
	}

	public final Context context;
	public final ScriptableObject scope;

	public RhinoTest(Context c) {
		context = c;
		scope = context.initSafeStandardObjects();
	}

	public void add(String name, Object value) {
		if (value.getClass() == Class.class) {
			ScriptableObject.putProperty(scope, name, new NativeJavaClass(context, scope, (Class<?>) value));
		} else {
			ScriptableObject.putProperty(scope, name, Context.javaToJS(context, value, scope));
		}
	}

	public Object eval(String name, Scriptable scope, String script) {
		try	{
			val o = context.evaluateString(scope, script, name, 1, null);
			TestConsole.log(name + ": passed");
			return o;
		} catch (Throwable ex) {
			ex.printStackTrace();
			return ex;
		}
	}

	public Object eval(String name, String script) {
		return eval(name, scope, script);
	}

	public Object evalNewScope(String name, String script) {
		val scope = context.initSafeStandardObjects();
		scope.setParentScope(this.scope);
		return eval(name, scope, script);
	}

	public void load(String file) {
		try (InputStream stream = RhinoTest.class.getResourceAsStream(file)) {
			String script = new String(IOUtils.toByteArray(new BufferedInputStream(stream)), StandardCharsets.UTF_8);
			context.evaluateString(scope, script, file, 1, null);
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
}
