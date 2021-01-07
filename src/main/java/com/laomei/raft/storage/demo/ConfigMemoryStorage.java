package com.laomei.raft.storage.demo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author luobo.hwz on 2020/12/21 17:28
 */
public class ConfigMemoryStorage {

    private final Map<String, Object> configs;

    public ConfigMemoryStorage() {
        this.configs = new HashMap<>();
    }

    public void add(final String key, final Object value) {
        configs.put(key, value);
    }

    public Object get(final String key) {
        return configs.get(key);
    }

    public Object delete(final String key) {
        return configs.remove(key);
    }

    public void clear() {
        configs.clear();
    }
}
