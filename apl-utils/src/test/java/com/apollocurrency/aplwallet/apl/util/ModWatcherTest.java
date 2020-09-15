
package com.apollocurrency.aplwallet.apl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author al
 */
public class ModWatcherTest {

    public ModWatcherTest() {
    }


    /**
     * Test of howLate method, of class ModWatcher.
     */
    @Test
    public void testHowLate() {
        System.out.println("howLate");
        ModWatcher instance = new ModWatcher(100, 15);
        long result = instance.howLate(0);
        assertEquals(0, result);
        result = instance.howLate(200);
        assertEquals(0, result);
        result = instance.howLate(210);
        assertEquals(10, result);
    }

    /**
     * Test of fullCircles method, of class ModWatcher.
     */
    @Test
    public void testFullCircles() {
        System.out.println("fullCircles");
        ModWatcher instance = new ModWatcher(100, 15);
        long result = instance.fullCircles(20);
        assertEquals(0, result);
        result = instance.fullCircles(320);
        assertEquals(3, result);
    }

    /**
     * Test of isTooLatelong method, of class ModWatcher.
     */
    @Test
    public void testIsTooLate() {
        System.out.println("isTooLatelong");
        ModWatcher instance = new ModWatcher(100, 15);
        boolean result = instance.isTooLate(5);
        assertEquals(false, result);
        result = instance.isTooLate(15);
        assertEquals(false, result);
        result = instance.isTooLate(115);
        assertEquals(false, result);
        result = instance.isTooLate(100);
        assertEquals(false, result);
        result = instance.isTooLate(2200);
        assertEquals(false, result);
        result = instance.isTooLate(116);
        assertEquals(true, result);
    }

}
