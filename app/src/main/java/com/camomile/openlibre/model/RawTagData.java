package com.camomile.openlibre.model;

import java.util.Locale;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static java.lang.Math.max;

public class RawTagData extends RealmObject {
    public static final String ID = "id";
    public static final String DATE = "date";
    public static final String TIMEZONE_OFFSET_IN_MINUTES = "timezoneOffsetInMinutes";
    public static final String TAG_ID = "tagId";
    public static final String DATA = "data";

    private static final int offsetTrendTable = 28;
    private static final int offsetHistoryTable = 124;
    private static final int offsetTrendIndex = 26;
    private static final int offsetHistoryIndex = 27;
    private static final int offsetSensorAge = 316;
    private static final int tableEntrySize = 6;
    private static final int sensorInitializationInMinutes = 60;

    @PrimaryKey
    private String id;
    private long date = -1;
    private int timezoneOffsetInMinutes;
    private String tagId;
    private byte[] data;

    public RawTagData() {}

    public RawTagData(String tagId, byte[] data) {
        this(tagId, System.currentTimeMillis(), data);
    }

    public RawTagData(String tagId, long utc_date, byte[] data) {
        date = utc_date;
        timezoneOffsetInMinutes = TimeZone.getDefault().getOffset(date) / 1000 / 60;
        this.tagId = tagId;
        id = String.format(Locale.US, "%s_%d", tagId, date);
        this.data = data.clone();
    }

    int getTrendValue(int index) {
        return getWord(offsetTrendTable + index * tableEntrySize) & 0x3FFF;
    }

    int getHistoryValue(int index) {
        return getWord(offsetHistoryTable + index * tableEntrySize) & 0x3FFF;
    }

    private static int makeWord(byte high, byte low) {
        return 0x100 * (high & 0xFF) + (low & 0xFF);
    }

    int getWord(int offset) {
        return getWord(data, offset);
    }

    private static int getWord(byte[] data, int offset) {
        return makeWord(data[offset + 1], data[offset]);
    }

    int getByte(int offset) {
        return data[offset] & 0xFF;
    }

    public int getIndexTrend() {
        return getByte(offsetTrendIndex);
    }

    int getIndexHistory() {
        return getByte(offsetHistoryIndex);
    }

    public int getSensorAgeInMinutes() {
        return getSensorAgeInMinutes(data);
    }

    private static int getSensorAgeInMinutes(byte[] data) {
        return getWord(data, offsetSensorAge);
    }

    public static int getSensorReadyInMinutes(byte[] data) {
        return max(0, sensorInitializationInMinutes - getSensorAgeInMinutes(data));
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getTimezoneOffsetInMinutes() {
        return timezoneOffsetInMinutes;
    }

    public void setTimezoneOffsetInMinutes(int timezoneOffsetInMinutes) {
        this.timezoneOffsetInMinutes = timezoneOffsetInMinutes;
    }

    public String getId() {
        return id;
    }

    public String getTagId() {
        return tagId;
    }

    public byte[] getData() {
        return data;
    }

    public int getHistoryFlags(int index) {
        return readBits(data, index * tableEntrySize + offsetHistoryTable, 0xe, 0xc);
    }

    public int getTrendFlags(int index) {
        return readBits(data, index * tableEntrySize + offsetTrendTable, 0xe, 0xc);
    }

    private int readBits(byte []buffer, int byteOffset,int  bitOffset, int  bitCount) {
        if (bitCount == 0) {
            return 0;
        }
        int res = 0;
        for (int i = 0; i < bitCount; i++) {
            final int totalBitOffset = byteOffset * 8 + bitOffset + i;
            final int byte1 = (int)Math.floor(totalBitOffset / 8);
            final int bit = totalBitOffset % 8;
            if (totalBitOffset >= 0 && ((buffer[byte1] >> bit) & 0x1) == 1) {
                res = res | (1 << i);
            }
        }
        return res;
    }
}
