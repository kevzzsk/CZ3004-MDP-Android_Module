package com.example.qunjia.mdpapp;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

public class MainActivity extends AppCompatActivity{

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
                    return GridMapFragment.newInstance(position);
                case 1:
                    return BluetoothFragment.newInstance(position);
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
