package cn.oneachina.onmiCore.util;

public final class LocationSerializer {

    public static String toString(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    public static String[] fromString(String serialized) {
        return serialized.split(",");
    }
}
