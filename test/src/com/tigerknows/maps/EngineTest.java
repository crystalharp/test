package com.tigerknows.maps;

import android.test.ActivityInstrumentationTestCase2;

import com.tigerknows.Sphinx;

public class EngineTest extends ActivityInstrumentationTestCase2<Sphinx>{

    protected Sphinx mActivity;  // the activity under test

    protected MapEngine engine = null;
    protected static final String testFolderPath = "/sdcard/tigermap/res/test/";
    protected static final String tempFolderPath = "/sdcard/tigermap/res/test/temp/";
    
    protected static final String dataRootFolderPath = "/sdcard/tigermap/map";
    
    public EngineTest() {
        super("com.tigerknows", Sphinx.class);
      }
      
      @Override
      protected void setUp() throws Exception {
          super.setUp();
          mActivity = this.getActivity();
          engine = mActivity.getMapEngine();
          setActivityInitialTouchMode(false);
      }
      
      protected void tearDown() {
          engine.suggestwordDestroy();
          try {
              super.tearDown();
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
      
      public void testPreconditions() {
        assertNotNull(engine);
      }
}
