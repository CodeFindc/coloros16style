package com.mess1on.combinedstatus;

final class CompactGeometry {
    private CompactGeometry() {
    }

    static int height(int customPx, int stockHeight, int parentHeight, int fallbackPx) {
        int base = customPx > 0 ? customPx : (stockHeight > 0 ? stockHeight : fallbackPx);
        int max = Math.max(fallbackPx, stockHeight);
        if (parentHeight > 0) {
            max = Math.min(max, parentHeight);
        }
        return Math.max(1, Math.min(base, max));
    }

    static int width(int customPx, int stockHeight, int fallbackPx) {
        if (customPx > 0) {
            return customPx;
        }
        return stockHeight > 0 ? stockHeight : fallbackPx;
    }
}
