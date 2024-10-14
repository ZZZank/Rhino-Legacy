package dev.latvian.mods.rhino.mod.remapper;

import lombok.val;
import net.neoforged.srgutils.IMappingFile;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author ZZZank
 */
public class MappingTester {
    public static void main(String[] args) {
        if (MappingTransformer.get() == null) {
            MappingTransformer.IMPL.setValue(new MappingTransformerForge());
        }
        val remapper = RhizoRemapper.instance();

        printSome(remapper.mappingC, 3);
        printSome(remapper.mappingF, 5);
        printSome(remapper.mappingM, 5);
    }

    private static void println(String line) {
        System.out.print(line);
        System.out.print('\n');
    }

    private static void printSome(Map<String, String> mapping, int limit) {
        mapping.entrySet()
            .stream()
            .limit(limit)
            .map(e -> e.getKey() + "->" + e.getValue())
            .forEach(MappingTester::println);
    }

    public static class MappingTransformerForge implements MappingTransformer {
        private static final Pattern FIELD_PATTERN = Pattern.compile("^field_[0-9]+_[a-zA-Z]+_?$");
        private static final Pattern METHOD_PATTERN = Pattern.compile("^func_[0-9]+_[a-zA-Z]+_?$");

        @Override
        public IMappingFile transform(IMappingFile vanillaMapping) {
            try {
                return vanillaMapping.chain(IMappingFile.load(MappingIO.getUrlConnection(
                    "https://github.com/ZZZank/Rhizo/raw/1.16-rhizo/_dev/joined_old.tsrg").getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean filterMethod(IMappingFile.IMethod method) {
            val original = method.getOriginal();
            val mapped = method.getMapped();
            return !original.equals(mapped)
                && METHOD_PATTERN.matcher(original).matches()
                && !mapped.startsWith("lambda$")
                && !mapped.startsWith("<");
        }

        @Override
        public String trimMethod(String name) {
            return name.substring("func_".length());
        }

        @Override
        public String restoreMethod(String trimmed) {
            return "func_" + trimmed;
        }

        @Override
        public boolean filterField(IMappingFile.IField field) {
            val original = field.getOriginal();
            val mapped = field.getMapped();
            return !original.equals(mapped)
                && FIELD_PATTERN.matcher(original).matches();
        }

        @Override
        public String trimField(String name) {
            return name.substring("field_".length());
        }

        @Override
        public String restoreField(String trimmed) {
            return "field_" + trimmed;
        }
    }

}
