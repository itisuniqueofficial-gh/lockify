package com.itisuniqueofficial.lockify.core.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoFenceTest {
    @Test
    fun distanceBetweenKnownPointsIsApproximatelyCorrect() {
        // ~1.11 km per 0.01 degree of latitude near the equator.
        val d = GeoFence.distanceMeters(0.0, 0.0, 0.01, 0.0)
        assertTrue("expected ~1112m but was $d", d in 1100.0..1125.0)
    }

    @Test
    fun withinAndOutsideRadius() {
        val home = LocationRule(1, "home", 12.9716, 77.5946, radiusMeters = 150f)
        assertTrue(GeoFence.isWithin(home, 12.9716, 77.5946))           // exact centre
        assertFalse(GeoFence.isWithin(home, 13.0716, 77.5946))          // ~11 km north
    }
}
