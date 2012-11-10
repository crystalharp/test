package com.tigerknows.model.response;

import com.tigerknows.model.response.PositionCake;

import android.test.ActivityTestCase;

public class PositionCakeTest extends ActivityTestCase{

    public void testDecode() {
        assertEquals(PositionCake.decodeLat(0x40cb1487), 0);
        assertEquals(PositionCake.decodeLon(0x2bc2ef60), 0);
    }
}
