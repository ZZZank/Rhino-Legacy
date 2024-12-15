package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.ScriptableObject;
import lombok.val;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RhinoTest {
	public static void main(String[] args) {
		Context context = Context.enterWithNewFactory();
		// context.setClassShutter((fullClassName, type) -> type != ClassShutter.TYPE_CLASS_IN_PACKAGE || isClassAllowed(fullClassName));

		RhinoTest test = new RhinoTest(context);
		test.add("console", TestConsole.class);
		test.add("fnWhat", FnInterfaceFrame.class);

		val result = test.eval("fn_interfaces.js",
			"""
			console.log(fnWhat.create("nice"))
			console.log('1 passed')
			fnWhat.create("nice", (str)=>{
				console.log(str)
			})""");
		if (result instanceof Exception e) {
			e.printStackTrace(System.out);
		}
//		test.load("/rhinotest/test.js");
	}

	public final Context context;
	public final ScriptableObject scope;

	public RhinoTest(Context c) {
		context = c;
		scope = context.initStandardObjects();
	}

	public void add(String name, Object value) {
		if (value.getClass() == Class.class) {
			ScriptableObject.putProperty(scope, name, new NativeJavaClass(context, scope, (Class<?>) value));
		} else {
			ScriptableObject.putProperty(scope, name, Context.javaToJS(context, value, scope));
		}
	}

	public Object eval(String name, String script) {
		try	{
			return context.evaluateString(scope, script, name, 0, null);
		} catch (Throwable ex) {
			return ex;
		}
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
