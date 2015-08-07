package com.dmtaiwan.alexander.iloveyoubike;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.dmtaiwan.alexander.iloveyoubike.Utilities.FragmentCallback;
import com.dmtaiwan.alexander.iloveyoubike.Utilities.LocationProvider;
import com.dmtaiwan.alexander.iloveyoubike.Utilities.RecyclerAdapterStation;
import com.dmtaiwan.alexander.iloveyoubike.Utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by lenovo on 8/6/2015.
 */
public class MainActivityViewPager extends AppCompatActivity implements StationListFragmentPager.Callback, StationDetailFragmentPager.OnFavoriteListener, LocationProvider.LocationCallback {
    private boolean mTabletLayout = false;
    ViewPagerAdaper mAdapter;
    private LocationProvider mLocationProvider;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.tab_layout)
    TabLayout mTabLayout;

    @InjectView(R.id.view_pager)
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pager);
        ButterKnife.inject(this);

        //Setup a location provider
        mLocationProvider = new LocationProvider(this, this);

        setSupportActionBar(mToolbar);

        setupViewPager(mViewPager);

        //Set up tab layout
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                FragmentCallback fragmentToShow = (FragmentCallback) mAdapter.instantiateItem(mViewPager, position);
                fragmentToShow.onFragmentShown();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationProvider.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationProvider.disconnect();
    }

    public void setupViewPager(ViewPager viewPager) {
        mAdapter = new ViewPagerAdaper(getSupportFragmentManager(), this);

        //Set up favorites fragment
        StationListFragmentPager favoritesFragment = new StationListFragmentPager();
        Bundle args = new Bundle();
        args.putBoolean(Utilities.EXTRA_FAVORITES, true);
        favoritesFragment.setArguments(args);
        mAdapter.addFragment(favoritesFragment);
        //Set up nearest station fragment
        StationDetailFragmentPager nearestStationFragment = new StationDetailFragmentPager();
        mAdapter.addFragment(nearestStationFragment);

        //Set up all stations fragment
        StationListFragmentPager allStations = new StationListFragmentPager();
        mAdapter.addFragment(allStations);

        viewPager.setAdapter(mAdapter);
    }

    @Override
    public void onFavorited() {
        StationListFragmentPager fragment = (StationListFragmentPager) mAdapter.getItem(0);
        fragment.restartLoader();
    }

    @Override
    public void handleNewLocation(Location location) {
        Utilities.setUserLocation(location, this);
        int currentFragment = mViewPager.getCurrentItem();
        Fragment fragment = (Fragment) mAdapter.instantiateItem(mViewPager, currentFragment);


        if (fragment instanceof StationDetailFragmentPager) {
            StationDetailFragmentPager stationDetailFragmentPager = (StationDetailFragmentPager) fragment;
            stationDetailFragmentPager.restartLoader();
        }

        if (fragment instanceof StationListFragmentPager) {
            StationListFragmentPager stationListFragmentPager = (StationListFragmentPager) fragment;
            stationListFragmentPager.restartLoader();
        }


    }

    public class ViewPagerAdaper extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private Context mContext;

        public ViewPagerAdaper(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String[] titleArray = mContext.getResources().getStringArray(R.array.tab_titles);
            return titleArray[position];
        }
    }


    @Override
    public void onItemSelected(int stationId, RecyclerAdapterStation.ViewHolder vh) {

        if (mTabletLayout) {
            Bundle args = new Bundle();
            args.putInt(Utilities.EXTRA_STATION_ID, stationId);
            args.putBoolean(Utilities.EXTRA_DETAIL_ACTIVITY, true);
            StationDetailFragmentPager fragment = new StationDetailFragmentPager();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.detail_container, fragment).commit();
        } else {

            Intent detailIntent = new Intent(this, StationDetailActivity.class);
            detailIntent.putExtra(Utilities.EXTRA_STATION_ID, stationId);
            Pair<View, String> p1 = Pair.create((View) vh.stationStatus, getString(R.string.transition_status_iamge_view));
            Pair<View, String> p2 = Pair.create((View) vh.stationName, getString(R.string.transitoin_station_name_text));
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, p1, p2);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                startActivity(detailIntent, options.toBundle());
            } else {
                startActivity(detailIntent);
            }
        }
    }
}
