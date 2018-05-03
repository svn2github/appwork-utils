package org.appwork.utils;

import java.util.Arrays;

public class CompareUtils {
    /**
     * @param x
     * @param y
     * @return
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /**
     * @param height
     * @param height2
     * @return
     */
    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compare(double x, double y) {
        // since 1.4
        return Double.compare(x, y);
    }

    /**
     * @param projection
     * @param projection2
     * @return
     */
    public static int compare(Comparable x, Comparable y) {
        if (x == y) {
            return 0;
        }
        if (x == null) {
            return -1;
        }
        if (y == null) {
            return 1;
        }
        return x.compareTo(y);
    }

    /**
     * @param hash
     * @param hash2
     * @return
     */
    public static boolean equals(byte[] hash, byte[] hash2) {
        if (hash == hash2) {
            return true;
        }
        if (hash == null || hash2 == null) {
            return false;
        }
        return Arrays.equals(hash, hash2);
    }
}
