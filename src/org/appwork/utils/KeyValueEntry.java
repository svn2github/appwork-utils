package org.appwork.utils;

import java.util.Map.Entry;

public class KeyValueEntry<KeyType, ValueType> {
    private KeyType key;

    public KeyType getKey() {
        return key;
    }

    public void setKey(KeyType key) {
        this.key = key;
    }

    public ValueType getValue() {
        return value;
    }

    public void setValue(ValueType value) {
        this.value = value;
    }

    private ValueType value;

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * @param key
     * @param value
     */
    public KeyValueEntry(KeyType key, ValueType value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @param entry
     */
    public KeyValueEntry(Entry<KeyType, ValueType> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     *
     */
    public KeyValueEntry(/* STorable */) {
        // TODO Auto-generated constructor stub
    }
}
