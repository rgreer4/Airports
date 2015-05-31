/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nadmm.airports.wx;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.SlidingTabLayout;

import java.util.ArrayList;

public final class WxMainActivity extends ActivityBase {

    private final String[] mTabTitles = new String[] {
            "Favorites",
            "Nearby",
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteWxFragment.class,
            NearbyWxFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;

    private ArrayList<Fragment> mWxFragments = new ArrayList<>();
    private int mCurrentFragmentIndex = -1;

    private ViewPager mViewPager = null;
    private WxViewPagerAdapter mViewPagerAdapter = null;
    private SlidingTabLayout mSlidingTabLayout = null;

    private static final String WX_SAVED_TAB = "wxtab";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_tab_pager );

        initFragments( savedInstanceState );

        setActionBarTitle( "Weather", null );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );
        mViewPager.setOffscreenPageLimit( 2 );
        mViewPagerAdapter = new WxViewPagerAdapter( getSupportFragmentManager() );
        mViewPager.setAdapter( mViewPagerAdapter );

        Resources res = getResources();
        mSlidingTabLayout = (SlidingTabLayout) findViewById( R.id.sliding_tabs );
        mSlidingTabLayout.setCustomTabView( R.layout.tab_indicator, android.R.id.text1 );
        mSlidingTabLayout.setSelectedIndicatorColors( res.getColor( R.color.tab_selected_strip ) );
        mSlidingTabLayout.setDistributeEvenly( false );
        mSlidingTabLayout.setViewPager( mViewPager );

        mSlidingTabLayout.setOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled( int position, float v, int i1 ) {
            }

            @Override
            public void onPageSelected( int position ) {
                mCurrentFragmentIndex = position;
                enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
                // Show the actionbar when a new page is selected
                resetActionBarAutoHide();
                autoShowOrHideActionBar( true );
            }

            @Override
            public void onPageScrollStateChanged( int state ) {
                enableDisableSwipeRefresh( state == ViewPager.SCROLL_STATE_IDLE
                        && getCurrentFragment().isRefreshable() );
            }
        } );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        enableActionBarAutoHide();
        registerHideableHeaderView( findViewById( R.id.headerbar ),
                UiUtils.calculateActionBarSize( this ) );

        if ( savedInstanceState != null ) {
            mCurrentFragmentIndex = savedInstanceState.getInt( WX_SAVED_TAB );
        } else {
            mCurrentFragmentIndex = getInitialFragmentId();
        }
        mViewPager.setCurrentItem( mCurrentFragmentIndex );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        for ( Fragment fragment : mWxFragments ) {
            // Save the fragments so we can restore them later
            getSupportFragmentManager().putFragment( outState,
                    fragment.getClass().getName(), fragment );
        }

        outState.putInt( WX_SAVED_TAB, mViewPager.getCurrentItem() );
    }

    public void onFragmentStarted( ListFragmentBase fragment ) {
        registerActionBarAutoHideListView( fragment.getListView() );
        updateContentTopClearance( fragment );
    }

    private void updateContentTopClearance( ListFragmentBase fragment ) {
        int actionbarClearance = UiUtils.calculateActionBarSize( this );
        int tabbarClearance = getResources().getDimensionPixelSize( R.dimen.tabbar_height );
        fragment.setContentTopClearance( actionbarClearance + tabbarClearance );
    }

    private void initFragments( Bundle savedInstanceState ) {
        if ( savedInstanceState != null ) {
            // Activity was recreated, check if our fragments survived
            for ( Class<?> clss : mClasses ) {
                // Restore the fragments from state saved earlier
                Fragment fragment = getSupportFragmentManager().getFragment(
                        savedInstanceState, clss.getName() );
                if ( fragment == null ) {
                    // Fragments were not saved
                    break;
                }
                mWxFragments.add( fragment );
            }
        }

        if ( mWxFragments.isEmpty() ) {
            // Create the fragments
            for ( Class<?> clss : mClasses ) {
                Bundle args = getIntent().getExtras();
                if ( args == null ) {
                    args = new Bundle();
                }
                args.putInt( ListFragmentBase.FRAGMENT_ID, mWxFragments.size() );
                Fragment fragment = Fragment.instantiate( this, clss.getName(), args );
                mWxFragments.add( fragment );
            }
        }
    }

    private ListFragmentBase getCurrentFragment() {
        return getFragmentAtPositoin( mCurrentFragmentIndex );
    }

    private ListFragmentBase getFragmentAtPositoin( int position ) {
        return (ListFragmentBase) mWxFragments.get( position );
    }

    private class WxViewPagerAdapter extends FragmentPagerAdapter {

        public WxViewPagerAdapter( FragmentManager fm ) {
            super( fm );
        }

        @Override
        public Fragment getItem( int position ) {
            return mWxFragments.get( position );
        }

        @Override
        public int getCount() {
            return mTabTitles.length;
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return mTabTitles[ position ];
        }
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_WX;
    }

    protected int getInitialFragmentId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        boolean alwaysShowNearby = prefs.getBoolean(
                PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
        ArrayList<String> fav = getDbManager().getWxFavorites();
        if ( alwaysShowNearby || fav.isEmpty() ) {
            return ID_NEARBY;
        } else {
            return ID_FAVORITES;
        }
    }

}
