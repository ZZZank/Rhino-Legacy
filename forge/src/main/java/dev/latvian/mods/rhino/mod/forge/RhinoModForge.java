package dev.latvian.mods.rhino.mod.forge;

import dev.latvian.mods.rhino.mod.RhinoProperties;
import dev.latvian.mods.rhino.mod.remapper.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("rhino")
public class RhinoModForge {

    public RhinoModForge() {
        MappingTransformer.IMPL.setValue(new MappingTransformerForge());

        FMLJavaModLoadingContext.get().getModEventBus().register(RhinoModForge.class);
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (RhinoProperties.INSTANCE.generateMapping) {
            Thread t = new Thread(() -> RhizoMappingGen.generate("1.16.5"));
            t.setDaemon(true);
            t.start();
        }
    }
}
