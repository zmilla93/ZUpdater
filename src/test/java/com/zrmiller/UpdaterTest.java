package com.zrmiller;

import com.zrmiller.zupdate.data.AppVersion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class UpdaterTest {

    @Test
    public void test1() {
        AppVersion target = new AppVersion("v0.4.0");
        AppVersion v1 = new AppVersion("v0.3.5");
        AppVersion v2 = new AppVersion("v0.4.0");
        AppVersion v3 = new AppVersion("v0.4.5");
        AppVersion v4 = new AppVersion("v0.4.5-pre1");
        AppVersion v5 = new AppVersion("v0.4.5-pre2");

        assertEquals(v1.compareTo(target), -1);
        assertEquals(v2.compareTo(target), 0);
        assertEquals(v3.compareTo(target), 1);

        assertFalse(v1.isPreRelease);
        assertFalse(v2.isPreRelease);
        assertFalse(v3.isPreRelease);
        assertTrue(v4.isPreRelease);
        assertTrue(v5.isPreRelease);
        assertFalse(target.isPreRelease);
    }

    @Test
    public void test2() {
        AppVersion v1 = new AppVersion("v0.3.5");
        AppVersion v2 = new AppVersion("v0.4.0-pre1");
        AppVersion v3 = new AppVersion("v0.4.0-pre2");
        AppVersion v4 = new AppVersion("v0.4.1");
        AppVersion v5 = new AppVersion("v0.3.5");

        assertEquals(v1.compareTo(v5), 0);
        assertEquals(v1.compareTo(v2), -1);
        assertEquals(v2.compareTo(v3), -1);
        assertEquals(v3.compareTo(v4), -1);
        assertEquals(v2.compareTo(v4), -1);
        assertEquals(v4.compareTo(v1), 1);
        assertTrue(v2.isPreRelease);
        assertTrue(v3.isPreRelease);
    }

    @Test
    public void sortingTest() {
        ArrayList<AppVersion> list = new ArrayList<>();
        list.add(new AppVersion("v0.2.0"));
        list.add(new AppVersion("v1.2.1"));
        list.add(new AppVersion("v1.2.0"));
        list.add(new AppVersion("v1.2.0-pre4"));
        list.add(new AppVersion("v0.0.2-pre1"));
        list.add(new AppVersion("v1.0.0"));
        list.add(new AppVersion("v0.2.0-pre81"));
        list.add(new AppVersion("v0.0.2"));
        list.add(new AppVersion("v1.2.0-pre5"));
        list.add(new AppVersion("v0.0.3"));
        list.add(new AppVersion("v0.2.0-pre3"));
        list.add(new AppVersion("v1.2.0-pre23"));
        System.out.println(Arrays.toString(list.toArray()));
        Collections.sort(list);
        System.out.println(Arrays.toString(list.toArray()));
    }

}
