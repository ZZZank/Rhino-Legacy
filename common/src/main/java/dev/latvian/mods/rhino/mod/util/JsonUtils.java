package dev.latvian.mods.rhino.mod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public interface JsonUtils {
	Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();

	static JsonElement copy(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return JsonNull.INSTANCE;
		} else if (element instanceof JsonArray jsonArray) {
			val a = new JsonArray();
			for (val e : jsonArray) {
				a.add(copy(e));
			}
			return a;
		} else if (element instanceof JsonObject jsonObject) {
			val o = new JsonObject();
			for (val entry : jsonObject.entrySet()) {
				o.add(entry.getKey(), copy(entry.getValue()));
			}
			return o;
		}
		return element;
	}

	static JsonElement of(@Nullable Object o) {
        if (o instanceof JsonSerializable serializable) {
            return serializable.toJson();
        } else if (o instanceof JsonElement element) {
            return element;
        } else if (o instanceof CharSequence) {
            return new JsonPrimitive(o.toString());
        } else if (o instanceof Boolean b) {
            return new JsonPrimitive(b);
        } else if (o instanceof Number number) {
            return new JsonPrimitive(number);
        } else if (o instanceof Character c) {
            return new JsonPrimitive(c);
        }
        return JsonNull.INSTANCE;
    }

	@Nullable
	static Object toObject(@Nullable JsonElement json) {
		if (json == null || json.isJsonNull()) {
			return null;
		} else if (json.isJsonObject()) {
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			JsonObject o = json.getAsJsonObject();

			for (val entry : o.entrySet()) {
				map.put(entry.getKey(), toObject(entry.getValue()));
			}

			return map;
		} else if (json.isJsonArray()) {
			JsonArray a = json.getAsJsonArray();
			List<Object> objects = new ArrayList<>(a.size());

			for (JsonElement e : a) {
				objects.add(toObject(e));
			}

			return objects;
		}

		return toPrimitive(json);
	}

	static String toString(JsonElement json) {
		StringWriter writer = new StringWriter();

		try {
			JsonWriter jsonWriter = new JsonWriter(writer);
			jsonWriter.setSerializeNulls(true);
			jsonWriter.setLenient(true);
			jsonWriter.setHtmlSafe(false);
			Streams.write(json, jsonWriter);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return writer.toString();
	}

	static String toPrettyString(JsonElement json) {
		StringWriter writer = new StringWriter();

		try {
			JsonWriter jsonWriter = new JsonWriter(writer);
			jsonWriter.setIndent("\t");
			jsonWriter.setSerializeNulls(true);
			jsonWriter.setLenient(true);
			jsonWriter.setHtmlSafe(false);
			Streams.write(json, jsonWriter);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return writer.toString();
	}

	static JsonElement fromString(@Nullable String string) {
		if (string == null || string.isEmpty() || string.equals("null")) {
			return JsonNull.INSTANCE;
		}

		try {
			JsonReader jsonReader = new JsonReader(new StringReader(string));
			JsonElement element;
			boolean lenient = jsonReader.isLenient();
			jsonReader.setLenient(true);
			element = Streams.parse(jsonReader);

			if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
				throw new JsonSyntaxException("Did not consume the entire document.");
			}

			return element;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return JsonNull.INSTANCE;
	}

	@Nullable
	static Object toPrimitive(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		} else if (element.isJsonPrimitive()) {
			JsonPrimitive p = element.getAsJsonPrimitive();

			if (p.isBoolean()) {
				return p.getAsBoolean();
			} else if (p.isNumber()) {
				return p.getAsNumber();
			}

			try {
				Double.parseDouble(p.getAsString());
				return p.getAsNumber();
			} catch (Exception ex) {
				return p.getAsString();
			}
		}

		return null;
	}
}