package com.mess1on.combinedstatus;

final class IconState {
    boolean airplane = false;
    int battery = 82;
    boolean charging = false;
    boolean inService = false;
    boolean mobileKnown = false;
    int mobileLevel = 0;
    String mobileTechnology = "";
    boolean nativeMobile = false;
    int rawMobileLevel = -1;
    boolean simPresent = true;
    int subId = -1;
    boolean wifiConnected = false;
    boolean wifiEnabled = false;
    boolean wifiKnown = false;
    int wifiLevel = 0;

    IconState() {
    }

    static float batterySweep(int level) {
        return (Math.max(0, Math.min(100, level)) * 308.0f) / 100.0f;
    }

    static int wifiSegments(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(3, level);
    }

    String centreTechnology() {
        if (this.airplane || !this.simPresent || !this.inService) {
            return "";
        }
        if (this.mobileTechnology == null || this.mobileTechnology.isEmpty()) {
            return "4G";
        }
        return this.mobileTechnology;
    }

    String description() {
        String wifiStr = !this.wifiEnabled ? "off" : (this.wifiConnected ? ("lvl " + this.wifiLevel) : "disconnected");
        String simStr = !this.simPresent ? "no-sim" : (!this.inService ? "no-service" : (centreTechnology() + " lvl " + this.mobileLevel));
        return "battery=" + this.battery + (this.charging ? "%+" : "%")
            + " wifi=" + wifiStr
            + " sim=" + simStr
            + (this.airplane ? " airplane" : "");
    }

    boolean readyForMerge() {
        return this.wifiKnown && this.mobileKnown;
    }
}
