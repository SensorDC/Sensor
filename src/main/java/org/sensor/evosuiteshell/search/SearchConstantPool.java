package org.sensor.evosuiteshell.search;

import java.util.HashMap;
import java.util.HashSet;

public class SearchConstantPool {
    private static SearchConstantPool instance = null;

    private SearchConstantPool() {
    }

    public static SearchConstantPool getInstance() {
        if (instance == null)
            instance = new SearchConstantPool();
        return instance;
    }

    private HashMap<String, HashSet<String>> pool = new HashMap<String, HashSet<String>>();

    public void setPool(String name, String value) {
        HashSet<String> values = pool.get(name);
        if (values == null) {
            values = new HashSet<String>();
            pool.put(name, values);
        }
        values.add(value);
    }

    public void setPool(String name, HashSet<String> values) {
        HashSet<String> set = pool.get(name);
        if (set == null) {
            set = new HashSet<String>();
            pool.put(name, set);
        }
        set.addAll(values);
    }

    public HashSet<String> getPoolValues(String name) {
        return pool.get(name);
    }

    public String getPoolValueRandom(String name) {
        HashSet<String> values = pool.get(name);
        if (values == null) return null;
        int num = (int) (Math.random() * values.size());
        int i = 0;
        String defaultValue = "";

        for (String value : values) {
            defaultValue = value;
            if (num == i) {
                defaultValue = value;
                break;
            }
            i++;
        }
        return defaultValue;
    }
}
