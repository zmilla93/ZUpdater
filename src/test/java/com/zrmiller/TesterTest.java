package com.zrmiller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TesterTest {

    @BeforeEach
    public void pre() {
        System.out.println("pre");
    }

    @Test
    public void testThing() {
        System.out.println("test thing");
        assertTrue(true);
        assertTrue(true);
        assertTrue(true);
    }

    @Test
    public void coolTest() {
        assert (true);
    }

}
