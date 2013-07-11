package com.tigerknows.view.discover;

import com.decarta.Globals;
import com.tigerknows.Sphinx;
import com.tigerknows.view.BaseFragment;

/**
 * The base class of all Fragment used in discover
 * @author jiangshuai
 *
 */
public class DiscoverBaseFragment extends BaseFragment {

    public DiscoverBaseFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    /**
     * On Pause implementation for fragments in discovers<br>
     * Stop the on going image downlaod when on pause
     */
    @Override
    public void onPause() {
        super.onPause();
        Globals.getAsyncImageLoader().stop(toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
