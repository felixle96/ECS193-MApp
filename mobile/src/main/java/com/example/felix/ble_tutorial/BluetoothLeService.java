package com.example.felix.ble_tutorial;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on
 * a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private List<BluetoothGattCharacteristic> myCharas;

    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    int myX = 1;
    int myY = 1;
    int currentCharIndex = 0;
    float[] myFloats = new float[64];
    private String currData;
    private JSONObject floats = new JSONObject();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.felix.ble_tutorial.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.felix.ble_tutorial.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.felix.ble_tutorial.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    //public final static UUID

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery: " +
                    mBluetoothGatt.discoverServices());
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    ByteBuffer floatBuffer = ByteBuffer.wrap(data);

//                    currData += String.valueOf(floatBuffer.getFloat())
//                            + " " + String.valueOf(floatBuffer.getFloat()) + " " + String.valueOf(floatBuffer.getFloat())
//                            + " " + String.valueOf(floatBuffer.getFloat()) + "\n";
                    try {
//                        floats.put("ch" + Integer.toString((myX - 1) * 4 + 0), floatBuffer.getFloat());
//                        floats.put("ch" + Integer.toString((myX - 1) * 4 + 1), floatBuffer.getFloat());
//                        floats.put("ch" + Integer.toString((myX - 1) * 4 + 2), floatBuffer.getFloat());
//                        floats.put("ch" + Integer.toString((myX - 1) * 4 + 3), floatBuffer.getFloat());
                        myFloats[(myX - 1) * 4 + 0] += floatBuffer.getFloat();
                        myFloats[(myX - 1) * 4 + 1] += floatBuffer.getFloat();
                        myFloats[(myX - 1) * 4 + 2] += floatBuffer.getFloat();
                        myFloats[(myX - 1) * 4 + 3] += floatBuffer.getFloat();

//                        currData += floats.toString();

//                        System.out.println("This is the output " + floats.toString());

//                        System.out.println("The characteristic is " + characteristic.getUuid());

                        Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                        intent.putExtra(EXTRA_DATA, currData);
                        //    sendBroadcast(intent);
                        //                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                        //                    if(!myCharas.isEmpty()) {
                        //                        BluetoothGattCharacteristic myChar = myCharas.remove(0);
                        //                        System.out.println(myChar.getUuid());
                        //                        readCharacteristic(myChar);
                        //                    }


//                        System.out.println(myCharas.get(myX).getUuid());

                        if (myX == 16) {
//                            System.out.println("16 Reads completed!");

                            myX = 1;

                            System.out.println(myY);

                            if(myY == 10){
                                for(int i = 0; i < 64; i++){
                                    myFloats[i] /= 10;
                                    floats.put("ch" + Integer.toString(i), myFloats[i]);
                                }

                                JSONObject patient = new JSONObject();
                                patient.put("patient_2", floats);

                                String jsonStr = patient.toString();

                                System.out.println(jsonStr);

                                // Send to server
//                                new Poster().execute(getString(R.string.insert_reading_url), jsonStr);

                                // Clear json object
                                floats = new JSONObject();
                                myY = 1;
                            }
                            else
                                myY++;
                        }
                        else {
                            readCharacteristic(myCharas.get(myX++));
                        }

                        sendBroadcast(intent);
                    } catch (Exception e) {
                        ;
                    }

                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

//            System.out.println("UUID is : " + characteristic.getUuid().toString());

//            final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
//            intent.putExtra(EXTRA_DATA, Integer.toString(myX++));
//            sendBroadcast(intent);
           // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//            System.out.println("Notification received!");

            BluetoothGattService updatedService = characteristic.getService();
            myCharas = updatedService.getCharacteristics();
//            myCharas.remove(0);
            currData = new String("");

//            myData.add(new String(""));

//            System.out.println("YOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO! " + myCharas.size());

//            for(BluetoothGattCharacteristic myChara : myCharas) {
//
//                if((myChara.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            BluetoothGattCharacteristic myChar = myCharas.remove(0);
//            System.out.println(myCharas.get(myX).getUuid());
            readCharacteristic(myCharas.get(myX));
//                }
//            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            //for (int i = 0; i < data.length && i < 15; i += 4) {
            //    stringBuilder.append(String.format("%02X ", data[i]));
            //}
            ByteBuffer floatBuffer = ByteBuffer.wrap(data);

            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + String.valueOf(floatBuffer.getFloat())
            + " " + String.valueOf(floatBuffer.getFloat()) + " " + String.valueOf(floatBuffer.getFloat())
            + " " + String.valueOf(floatBuffer.getFloat()));
        }

        sendBroadcast(intent);
    }

    public BluetoothLeService() {
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.}
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        /**
         * After using a given device, you should make sure that BluetoothGatt.close() is called
         * such that resources are cleaned up properly. In this particular case, close() is
         * invoked when the UI is disconnected from the Service.
         */
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * initialize()
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        /**
         * For API level 18 and above, get a reference to BluetoothAdapter through
         * BluetoothManager.
         */
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt!= null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

        /**
         * We want to directly connect to the device, so we are setting the autoConnect
         * parameter to false.
         */
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * disconnect()
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,
     * int, int)} callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
    }

    /**
     * close()
     * After using a given BLE device, the app must call this method to ensure resources
     * are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * readCharacteristic()
     * Request a read on a given {@code B$luetoothGattCharacteristic.} The read result is
     * reported asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * setCharacteristicNotification()
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notifications. False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * getSupportedGattServices()
     * Retrieves a list of supported GATT services on the connected device. This should only
     * be invoked after {@code BluetoothGAtt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
