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
    private boolean checkForErrorFlags = false;

    public RawTagData() {}

    public RawTagData(String tagId, byte[] data) {
        this(tagId, System.currentTimeMillis(), data);
    }

    public RawTagData(String tagId, byte[] data, boolean checkForErrorFlags) {
        this(tagId, System.currentTimeMillis(), data);
        this.checkForErrorFlags = checkForErrorFlags;
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

    //val temperature = readBits(data, offset, 0x1a, 0xc).shl(2)
    //        var temperatureAdjustment = readBits(data, offset, 0x26, 0x9) shl 2
    //        val negativeAdjustment = readBits(data, offset, 0x2f, 0x1)
    //        if (negativeAdjustment != 0) { temperatureAdjustment = -temperatureAdjustment }
    //        val error = (readBits(data, offset, 0xe, 0xb)).toUInt() and 0x1ff.toUInt()
    //        val hasError = readBits(data, offset, 0x19, 0x1) != 0

    public boolean checkIfErrorData(int index) {
        return readBits(data, index * tableEntrySize + offsetTrendTable, 0x19, 0x1) != 0;
    }

    public int getErrorOffset(int index) {
        return readBits(data, index * tableEntrySize + offsetTrendTable, 0xe, 0xb) & 0x1ff;
    }

    public int getRawTemperature(int index) {
        return readBits(data, index * tableEntrySize + offsetTrendTable, 0x1a, 0xc) << 2;
    }

    public int getTemperatureAdjustment(int index) {
        int temperatureAdjustment = readBits(data, index * tableEntrySize + offsetTrendTable, 0x26, 0x9) << 2;
        int negativeAdjustment = readBits(data, index * tableEntrySize + offsetTrendTable, 0x2f, 0x1);
        if (negativeAdjustment != 0) {
            temperatureAdjustment = -temperatureAdjustment;
        }
        return temperatureAdjustment;
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

    public boolean isCheckForErrorFlags() {
        return checkForErrorFlags;
    }
}
