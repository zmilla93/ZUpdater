package com.zrmiller.zupdate.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppVersion implements Comparable<AppVersion> {

    // Public info
    public final boolean valid;
    public final boolean isPreRelease;

    // Internal
    private String string;
    private final int major;
    private final int minor;
    private final int patch;
    private final int pre;

    // Matching
    private static final String matchString = "v?(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)(-pre(?<pre>\\d+))?";
    private static final Pattern pattern = Pattern.compile(matchString);

    // FIXME : Implement prerelease
    public AppVersion(String tag) {
        Matcher matcher = null;
        if (tag != null) {
            matcher = pattern.matcher(tag);
            valid = matcher.matches();
        } else {
            valid = false;
        }
        if (valid) {
            major = Integer.parseInt(matcher.group("major"));
            minor = Integer.parseInt(matcher.group("minor"));
            patch = Integer.parseInt(matcher.group("patch"));
            String preString = matcher.group("pre");
            if (preString != null) {
                isPreRelease = true;
                pre = Integer.parseInt(matcher.group(5));
            } else {
                pre = -1;
                isPreRelease = false;
            }
        } else {
            major = -1;
            minor = -1;
            patch = -1;
            pre = -1;
            isPreRelease = false;
        }
        string = "v" + major + "." + minor + "." + patch;
        if (isPreRelease) string += "-pre" + pre;
    }

    // FIXME : Move this to an actual test
    public static void runTest() {
        AppVersion v1 = new AppVersion("v0.3.5");
        AppVersion v2 = new AppVersion("v0.4.0");
        AppVersion v3 = new AppVersion("v0.4.5");
        AppVersion v4 = new AppVersion("v0.4.5-pre1");
        AppVersion v5 = new AppVersion("v0.4.5-pre2");
        AppVersion target = new AppVersion("v0.4.0");
        int i1 = v1.compareTo(target);
        int i2 = v2.compareTo(target);
        int i3 = v3.compareTo(target);
        System.out.println("patch?" + (i1));
        System.out.println("patch?" + (i2));
        System.out.println("patch?" + (i3));
        System.out.println(v3);
        System.out.println(v4);
        System.out.println(v5);
        System.out.println(v3.isPreRelease);
        System.out.println(v4.isPreRelease);
    }

    @Override
    // FIXME : Add prerelease support
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
