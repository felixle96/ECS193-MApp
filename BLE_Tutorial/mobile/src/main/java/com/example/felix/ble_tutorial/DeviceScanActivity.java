/**
 * Copyright Stuff:
 *
 * Author: Felix Le
 * Reference: Android Open Source Project (BLE Tutorial)
 * https://android.googlesource.com/platform/development/+/7167a054a8027f75025c865322fa84791a9b3bd
 * 1/samples/BluetoothLeGatt?autodive=0%2F
 */
package com.example.felix.ble_tutorial;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Class: DeviceScanActivity
 * This activity scans for BLE devices and displays them in a view
 */
public class DeviceScanActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView mListView;
    private boolean mScanning;
    private Handler mHandler;
    private String[] mPermissions = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final int REQUEST_APP_PERMISSIONS = 2;
    private static final int REQUEST_ENABLE_BT = 1;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;

    /**
     * onCreate()
     * Is called on creation of activity. Override original implementation
     * to add in Bluetooth scanning related initialization.
     *
     * @param savedInstanceState Previously saved state of activity if exists.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Load previously saved state and set the view for the
         * activity. Set the title of the activity using the action
         * bar and create a handlers to schedule messages and
         * runnables/actions.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        //getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();
        mListView = findViewById(R.id.list);
        mListView.setOnItemClickListener(devicesClickListener);

        /**
         * Check whether BLE is supported on the device. You can then
         * selectively disable BLE_related features. Display non-intrusive
         * pop up message to indicate this and end activity.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        /**
         * Request access to dangerous permissions: ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION.
         * These permissions have to be declared in the manifest file and explicitly requested
         * (prompting the user for access).
         */
        ActivityCompat.requestPermissions(this, mPermissions, REQUEST_APP_PERMISSIONS);

        /**
         * Initializes a Bluetooth adapter to handle BLE requests/tasks such as scanning,
         * pairing, etc. Get the system level Bluetooth Service and acquire its bluetooth
         * adapter.
         */
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        /**
         * Check if Bluetooth is supported on this device, i.e. no bluetooth adapter.
         * If it isn't, display a short message and end activity.
         */
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    /**
     * onCreateOptionsMenu()
     * Is called when the user tries to display the options menu for the first time or
     * when the menu has been changed with (if invalidateOptionsMenu() has been called.
     *
     * @param menu This activity's (DeviceScanActivity) menu.
     * @return To display or to not display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /**
         * Instantiate the menu with an xml layout file. Display items from the menu
         * depending on the scanning state of the scanner.
         */
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
          //  menu.findItem(R.id.menu_refresh).setActionView(
            //        R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    /**
     * onPrepareOptionsMenu()
     * Is always called right before the options menu is displayed, use this to
     * dynamically update the contents of the options menu.
     *
     * @param menu The menu last shown or initialized by onCreateOptionsMenu()
     * @return To display or to not display the menu.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /**
         * Display items from the menu depending on the scanning state of the scanner.
         */
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            //  menu.findItem(R.id.menu_refresh).setActionView(
            //        R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    /**
     * onOptionsItemSelected()
     * Is called whenever an item from the options menu is selected/clicked. By default
     * it will return false and handle events normally (either call the item's Runnable or
     * send a message to its Handler). If true is returned those default events will not happen
     * (will end events here).
     *
     * @param item The menu item that was selected.
     * @return False will continue normal processing, true will prevent that from happening.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * Clear currently found BLE devices and rescan again or stop the scan.
         */
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }

        return true;
    }

    /**
     * onResume()
     * Is called whenever the activity will start interacting with the user. Always followed
     * by onPause().
     */
    @Override
    protected void onResume() {
        /**
         * Enable Bluetooth if it is not enabled. Create a new list of devices and attach
         * it to the list view so it will be shown; then start scanning for devices.
         */
        super.onResume();
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter((mLeDeviceListAdapter));
        scanLeDevice(true);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }


    ListView.OnItemClickListener devicesClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if(device == null) return;
            final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }
            startActivity(intent);
        }
    };
    /**
     * scanLeDevice()
     * Scans for BLE devices only when scan is enabled.
     *
     * @param enable Activates/deactivates scanning.
     */
    private void scanLeDevice(final boolean enable) {
        final ListView listView = findViewById(R.id.list);
        /**
         * Start scanning for a certain amount of time if scanning is enabled
         * amount of time, else stop scanning.
         */
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    listView.setVisibility(View.VISIBLE);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            listView.setVisibility(View.INVISIBLE);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            listView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Class: LeDeviceListAdapter
     * Used to hold data on BLE devices and to make them
     * available to a View Group for displaying.
     */
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater;

        /**
         * LeDeviceListAdapter()
         * Initializes the list of BLE devices and the layout inflater.
         */
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflater = DeviceScanActivity.this.getLayoutInflater();
        }

        /**
         * addDevice()
         * Add the a BLE device to the list of BLE devices if it is not already in the list.
         *
         * @param device A new BLE device.
         */
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        /**
         * getDevice()
         * Gets the BLE device in a certain position of the list.
         *
         * @param position The position of the BLE device to return.
         * @return The BLE device at position.
         */
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        /**
         * clear()
         * Deletes list of currently stored/discovered BLE devices.
         */
        public void clear() {
            mLeDevices.clear();
        }

        /**
         * getCount()
         * Gets the number of BLE devices currently stored/discovered.
         *
         * @return The number of BLE devices in list (currently found).
         */
        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        /**
         * getItem()
         * Gets the BLE device in a certain position of the list.
         *
         * @param i The position of the BLE device to return.
         * @return The BLE device at position.
         */
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        /**
         * getItemId()
         * Returns the row identifier of the BLE device at a certain position. Not really
         * needed, but it still has to be overridden or can't instantiate class.
         *
         * @param i The position of the BLE device in list
         * @return The id of the BLE device.
         */
        @Override
        public long getItemId(int i) {
            return i;
        }

        /**
         * getView()
         * Gets a view that displays the data of a BLE device in list. This is used by the
         * listview to determine how to display each BLE device in list.
         *
         * @param i The position of the BLE device in list.
         * @param view The old view to reuse (if not null).
         * @param viewGroup The parent that the returned view will be attached to.
         * @return A view that properly displays the BLE device data.
         */
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            /**
             * Inflate a new view using a layout file or reuse the view
             * if it exists.
             */
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            /**
             * Fill out the data/fields of the new BLE device view so that
             * the listview can display it.
             */
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if ( (deviceName != null) && (deviceName.length() > 0) )
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

    }

    /**
     * Initialize BluetoothAdapter callbacks so that they will handle BluetoothAdapter
     * events.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    {

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            /**
             * onLeScan()
             * Is called when a BLE device is found during a scan. It then stores the
             * BLE device into the list and notifies any views that are displaying the
             * list of BLE devices to update themselves. This is run specifically on the
             * UI (main) thread.
             *
             * @param device The newly found BLE device.
             * @param rssi Data on the proximity of the BLE device.
             * @param scanRecord The contents of the BLE device's advertisement.
             */
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
    }

    /**
     * Class: ViewHolder
     *
     * Holds the data that will be displayed in the list view (BLE devices list).
     */
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}

