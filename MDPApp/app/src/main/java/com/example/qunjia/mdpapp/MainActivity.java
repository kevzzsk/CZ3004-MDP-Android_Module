package com.example.qunjia.mdpapp;

import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;
public class MainActivity extends AppCompatActivity{
    private GridMapUpdateManager gridMapUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize the ViewPager and set an adapter
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.topTabs);
        tabs.setViewPager(pager);

        gridMapUpdateManager = new GridMapUpdateManager(this);
    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {

        private String[] TITLES = {"Grid Map", "Bluetooth"};

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    GridMapFragment gridMapFragment = GridMapFragment.newInstance(position);
                    GridMapFragment.mapUpdateManager = gridMapUpdateManager;
                return gridMapFragment;
                case 1:
                    BluetoothFragment bluetoothFragment = BluetoothFragment.newInstance(position);
                    BluetoothFragment.mapUpdateManager = gridMapUpdateManager;
                return bluetoothFragment;
            }
            return null;
        }
    }

    public void GridMapOnClickMethods(View v) {
        GridMapFragment.myClickMethod(v);
    }

    public void BluetoothOnclickMethods(View v) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BluetoothFragment) {
                BluetoothFragment bluetooth_fragment = (BluetoothFragment) fragment;
                bluetooth_fragment.myClickMethod(v, this);
            }
        }
    }
}
