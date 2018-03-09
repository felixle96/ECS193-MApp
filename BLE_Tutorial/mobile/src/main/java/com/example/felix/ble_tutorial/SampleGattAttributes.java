package com.example.felix.ble_tutorial;

import java.util.HashMap;

/**
 * Created by felix on 3/6/18.
 */

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb";
    public static String PATCH_ATTRIBUTE = "72369d5c-94e1-41d7-acab-a88062c506a8";

    static {
        attributes.put(GENERIC_ACCESS, "Generic Access");
        attributes.put(GENERIC_ATTRIBUTE, "Generic Attributes");
        attributes.put(PATCH_ATTRIBUTE, "Patch Attributes");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
