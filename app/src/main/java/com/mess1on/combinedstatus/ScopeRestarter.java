package com.mess1on.combinedstatus;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class ScopeRestarter {
    static final String SENTINEL = "COMBINED_STATUS_RESTART_SENT";

    public interface Launcher {
        Process start() throws Exception;
    }

    private ScopeRestarter() {
    }

    static String command() {
        return "(killall com.android.systemui || pkill -f com.android.systemui || kill -9 $(pidof com.android.systemui)) >/dev/null 2>&1; echo " + SENTINEL;
    }

    public static boolean run(Launcher launcher, int timeoutMs) {
        Process proc = null;
        try {
            proc = launcher.start();
            proc.getOutputStream().write((command() + "\nexit\n").getBytes(StandardCharsets.UTF_8));
            proc.getOutputStream().flush();
            proc.getOutputStream().close();
            if (!proc.waitFor(Math.max(1, timeoutMs), TimeUnit.MILLISECONDS)) {
                proc.destroyForcibly();
                return false;
            }
            InputStream in = proc.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            String out = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            if (proc.exitValue() == 0 && out.contains(SENTINEL)) {
                return true;
            }
        } catch (Throwable ignored) {
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
        return false;
    }
}
