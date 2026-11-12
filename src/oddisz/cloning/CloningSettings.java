package oddisz.cloning;

import lunalib.lunaSettings.LunaSettings;

/** LunaLib values with code defaults; the defaults match data/config/LunaSettings.csv. */
public final class CloningSettings {

    public static final String MOD_ID = "oddisz_cloning";

    private CloningSettings() {}

    public static int getInt(String field, int def) {
        try {
            Integer v = LunaSettings.getInt(MOD_ID, field);
            return v != null ? v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static double getDouble(String field, double def) {
        try {
            Double v = LunaSettings.getDouble(MOD_ID, field);
            return v != null ? v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean getBool(String field, boolean def) {
        try {
            Boolean v = LunaSettings.getBoolean(MOD_ID, field);
            return v != null ? v : def;
        } catch (Throwable t) {
            return def;
        }
    }
}
