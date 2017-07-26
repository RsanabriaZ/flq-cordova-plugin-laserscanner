package com.bluefletch.motorola.scanhelpers;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;

/**
 * Parse a GS1128Barcode from data retrieved from devices
 * Similar to {@link com.bluefletch.motorola.scanhelpers.ApplicationIdentifiers}
 */
public class DataWedgeParser {

    public static final Map<String, Integer> aiLengths = new HashMap<String, Integer>();

    static {
        aiLengths.put("00", 18);
        aiLengths.put("01", 14);
        aiLengths.put("02", 14);
        aiLengths.put("10", 0);
        aiLengths.put("11", 6);
        aiLengths.put("12", 6);
        aiLengths.put("13", 6);
        aiLengths.put("15", 6);
        aiLengths.put("16", 6);
        aiLengths.put("17", 6);
        aiLengths.put("20", 2);
        aiLengths.put("21", 0);
        aiLengths.put("30", 0);
        aiLengths.put("37", 0);
        aiLengths.put("3201", 0);
        aiLengths.put("3202", 0);
    }

    private final HashMap<String, String> ais;
    private String barcodeWithFNC1;

    public DataWedgeParser(byte[] bytes) {
        this.barcodeWithFNC1 = "";
        this.ais = new HashMap<String, String>();
        parseFromDataWedge(bytes);
    }

    public void parseFromDataWedge(byte[] bytes) {
        int i = 0;
        while ( i < bytes.length) {
            String a = String.valueOf(bytes[i]);
            if (a.equals("29")) {
                this.barcodeWithFNC1 += "{FNC1}";
            } else {
                this.barcodeWithFNC1 += String.valueOf((char) (bytes[i] & 0xFF));
            }
            i++;
        }
    }

    public boolean isGlobalTradeItemNumber() {
        return ais.containsKey("gtin");
    }

    public boolean isSSCC() {
        return ais.containsKey("sscc");
    }

    public String getGlobalTradeItemNumber() {
        if (isGlobalTradeItemNumber()) {
            return this.ais.get("gtin");
        }
        return "";
    }

    public String getBarcodeWithFNC1(){
        return this.barcodeWithFNC1;
    }

    public String getLot() {
        return this.ais.containsKey("batchLot") ? this.ais.get("batchLot") : "";
    }

    public String getUseThroughDate() {
        return this.ais.containsKey("useThrough") ? this.ais.get("useThrough") : "";
    }

    public String getPackDate() {
        return this.ais.containsKey("packDate") ? this.ais.get("packDate") : "";
    }

    public String getSerialNumber() {
        return this.ais.containsKey("serial") ? this.ais.get("serial") : "";
    }

    public int getQuantity() {
        return this.ais.containsKey("count") ? Integer.parseInt(this.ais.get("count")) : 0;
    }
}
