package net.thedreamers.guards.client;

import net.fabricmc.loader.api.FabricLoader;
import java.util.stream.Collectors;

public class ModScanner {

    private static final String[] BLACKLISTED_MOD_IDS = {
            "wurst", "meteor-client", "liquidbounce", "bleachhack", "aristois",
            "kami", "rusherhack", "future", "inertia", "phobos",
            "salhack", "impact", "mathax", "vector", "danielfrominternet",
            "kami-blue", "seppuku", "coffee", "konas", "lambda",
            "abyss", "w+3", "w+2", "gopro", "earthhack",
            "bleach", "pixel", "liquid", "ares", "novoline",
            "flux", "rise", "tenacity", "vape", "astolfo", "zeroday"
    };

    public static String getInstalledModsString() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .sorted()
                .collect(Collectors.joining(","));
    }

    public static boolean containsIllegalMod() {
        for (String illegalId : BLACKLISTED_MOD_IDS) {
            if (FabricLoader.getInstance().isModLoaded(illegalId)) {
                return true;
            }
        }
        return false;
    }
}