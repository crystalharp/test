package com.tigerknows.view.discover;

import com.decarta.Globals;
import com.tigerknows.Sphinx;
import com.tigerknows.view.BaseFragment;

public class DiscoverBaseFragment extends BaseFragment {

    public DiscoverBaseFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

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
