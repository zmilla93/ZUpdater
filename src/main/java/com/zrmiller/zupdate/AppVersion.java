package com.zrmiller.zupdate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppVersion implements Comparable<AppVersion> {

    private String string;
    public boolean valid;
    public final int major;
    public final int minor;
    public final int patch;
    public boolean prerelease;

    private static final String matchString = "v?(\\d+)\\.(\\d+)\\.(\\d+)(-pre(\\d+))?";
    private static final Pattern pattern = Pattern.compile(matchString);

    // FIXME : Implement prerelease
    public AppVersion(String tag) {
        this.string = tag;
        Matcher matcher = null;
        if (tag != null) {
            matcher = pattern.matcher(tag);
            valid = matcher.matches();
        }
        if (valid) {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));
            patch = Integer.parseInt(matcher.group(3));
        } else {
            major = -1;
            minor = -1;
            patch = -1;
        }
    }

    // FIXME : Move this to an actual test
    public static void runTest() {
        AppVersion v1 = new AppVersion("v0.3.5");
        AppVersion v2 = new AppVersion("v0.4.0");
        AppVersion v3 = new AppVersion("v0.4.5");
        AppVersion target = new AppVersion("v0.4.0");
        int i1 = v1.compareTo(target);
        int i2 = v2.compareTo(target);
        int i3 = v3.compareTo(target);
        System.out.println("patch?" + (i1));
        System.out.println("patch?" + (i2));
        System.out.println("patch?" + (i3));
    }

    @Override
    public int compareTo(AppVersion o) {
        if (o.major > major) return -1;
        else if (o.major < major) return 1;
        else if (o.minor > minor) return -1;
        else if (o.minor < minor) return 1;
        else if (o.patch > patch) return -1;
        else if (o.patch < patch) return 1;
        return 0;
    }

    @Override
    public String toString() {
        return string;
    }

}
