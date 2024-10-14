package dev.latvian.mods.rhino.mod.remapper;

import com.google.common.collect.ImmutableMap;
import dev.latvian.mods.rhino.mod.RhinoProperties;
import dev.latvian.mods.rhino.util.remapper.Remapper;
import dev.latvian.mods.rhino.util.remapper.RemapperException;
import lombok.val;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static dev.latvian.mods.rhino.mod.remapper.RhizoMappingGen.SKIP_MARK;

/**
 * @author ZZZank
 */
public class RhizoRemapper implements Remapper {

    private static RhizoRemapper INSTANCE = null;

    public final Map<String, String> mappingC; //class mapping
    public final Map<String, String> unmappingC; //class mapping
    public final Map<String, String> mappingM; //method mapping
    public final Map<String, String> mappingF; //field mapping

    private RhizoRemapper() {
        val builderMappingC = ImmutableMap.<String, String>builder();
        val builderUnmappingC = ImmutableMap.<String, String>builder();
        val builderMappingM = ImmutableMap.<String, String>builder();
        val builderMappingF = ImmutableMap.<String, String>builder();
        //load
        try (val in = locateMappingFile()) {
            if (in == null) {
                throw new RemapperException("No Rhizo mapping file available!");
            }
            if (in.read() != RhizoMappingGen.MAPPING_MARK) {
                throw new RemapperException("Invalid Rhizo mapping file!");
            }
            {
                val version = in.read();
                if (version != RhizoMappingGen.MAPPING_VERSION) {
                    throw new RemapperException(String.format(
                        "Rhizo mapping file version %d not matching expected version %d",
                        version,
                        RhizoMappingGen.MAPPING_VERSION
                    ));
                }
            }
            MappingIO.LOGGER.info("Loading mappings for {}", MappingIO.readUtf(in));
            val transformer = MappingTransformer.get();
            //class
            val classCount = MappingIO.readVarInt(in);
            for (int i = 0; i < classCount; i++) {
                val originalC = MappingIO.readUtf(in);
                if (SKIP_MARK.equals(originalC)) {
                    continue;
                }
                val mappedC = MappingIO.readUtf(in);
                builderMappingC.put(originalC, mappedC);
                builderUnmappingC.put(mappedC, originalC);
                //method
                val methodCount = MappingIO.readVarInt(in);
                for (int j = 0; j < methodCount; j++) {
                    val originalM = MappingIO.readUtf(in);
                    if (SKIP_MARK.equals(originalM)) {
                        continue;
                    }
                    val mappedM = MappingIO.readUtf(in);
                    builderMappingM.put(transformer.restoreMethod(originalM), mappedM);
                }
                //field
                val fieldCount = MappingIO.readVarInt(in);
                for (int j = 0; j < fieldCount; j++) {
                    val originalF = MappingIO.readUtf(in);
                    if (SKIP_MARK.equals(originalF)) {
                        continue;
                    }
                    val mappedF = MappingIO.readUtf(in);
                    builderMappingF.put(transformer.restoreField(originalF), mappedF);
                }
            }
        } catch (Exception e) {
            MappingIO.LOGGER.error("Exception happened during Rhizo Minecraft remapper initialization!", e);
        }
        mappingC = builderMappingC.build();
        unmappingC = builderUnmappingC.build();
        mappingM = builderMappingM.build();
        mappingF = builderMappingF.build();
    }

    private static InputStream locateMappingFile() {
        val cfgPath = RhinoProperties.getGameDir().resolve("config/" + RhizoMappingGen.MAPPING_FILENAME);
        try {
            if (Files.exists(cfgPath)) {
                MappingIO.LOGGER.info("Found Rhizo mapping file from config/{}.", RhizoMappingGen.MAPPING_FILENAME);
                return new GZIPInputStream(Files.newInputStream(cfgPath));
            }
            val in = new GZIPInputStream(RhinoProperties.openResource(RhizoMappingGen.MAPPING_FILENAME));
            MappingIO.LOGGER.info("Found Rhizo mapping file from Rhizo mod jar.");
            return in;
        } catch (Exception e) {
            return null;
        }
    }

    public static RhizoRemapper instance() {
        if (INSTANCE == null) {
            long start = System.currentTimeMillis();
            INSTANCE = new RhizoRemapper();
            MappingIO.LOGGER.info("Rhizo remapper initialization took {} milliseconds", System.currentTimeMillis()-start);
        }
        return INSTANCE;
    }

    @Override
    public String remapClass(Class<?> from) {
        return mappingC.getOrDefault(from.getName(), NOT_REMAPPED);
    }

    @Override
    public String unmapClass(String from) {
        return unmappingC.getOrDefault(from, NOT_REMAPPED);
    }

    @Override
    public String remapField(Class<?> from, Field field) {
        return mappingF.getOrDefault(field.getName(), NOT_REMAPPED);
    }

    @Override
    public String remapMethod(Class<?> from, Method method) {
        return mappingM.getOrDefault(method.getName(), NOT_REMAPPED);
    }
}
