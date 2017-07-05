package com.bluefletch.motorola.scanhelpers;

import java.util.HashMap;
import java.util.Map;

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

    public DataWedgeParser(byte[] bytes) {
        this.ais = new HashMap<String, String>();
        parseFromDataWedge(bytes);
    }

    public void parseFromDataWedge(byte[] bytes) {
        byte fncOneByte = bytes[0];
        byte startByte = bytes[0];
        int currentCode = ApplicationIdentifiers.CODE_C;
        switch (String.valueOf((bytes[0] & 0xFF))) {
            case "103":
                currentCode = ApplicationIdentifiers.CODE_A;
                break;
            case "104":
                currentCode = ApplicationIdentifiers.CODE_B;
                break;
            case "105":
                currentCode = ApplicationIdentifiers.CODE_C;
                break;
        }
        boolean done = false;
        int i = 0;
        boolean nextByteIsAI = false;
        /*
         * Support for triple and quadruple character AI's.
         * nextByteIsSecondaryAI: flags the next byte as being part of an AI and not content
         * primaryAI: In the event that a triple or quadruple AI start is initiated, we store that
         * initial byte pair to append it to the next byte, which completes the byte pair.
         */
        boolean nextByteIsSecondaryAI = false;
        String primaryAI = "";
        AI currentAi = null;
        int currentAiMax = -1;
        int currentAiCharCount = 0;

        while (true) {
            String byteValue = String.valueOf((bytes[i] & 0xFF));
            if (byteValue.length() == 1) byteValue = "0" + byteValue;

            //Handle Byte

            if (nextByteIsSecondaryAI) {
                if (ApplicationIdentifiers.tripleAis.contains(primaryAI)) {
                    //First character should be the last character for the AI
                    String aiCode = primaryAI + byteValue.substring(0, 1);
                    currentAi = new AI(ApplicationIdentifiers.names.get(aiCode));
                    //Second value should be first value of the new AI, after it's been mapped
                    String mappedVal = mapCharacterFromByteValue(byteValue, currentCode)
                            .substring(1);
                    currentAi.addToValue(mappedVal);
                    currentAiMax = ApplicationIdentifiers.lengths.get(aiCode);
                } else {
                    //Complete the AI and don't add a value yet, because the next byte starts
                    //the AI's value.
                    byteValue = primaryAI + byteValue;
                    currentAi = new AI(ApplicationIdentifiers.names.get(byteValue));
                    currentAiMax = ApplicationIdentifiers.lengths.get(byteValue);
                }
                nextByteIsSecondaryAI = false;
                primaryAI = "";
            }
            /*
            If the byte is a code switch byte, handle that switch and consume the byte
             */
            else if (byteIsCodeSwitch(currentCode, byteValue)) {
                currentCode = getNewCode(currentCode, byteValue);
            }
            /*
            Because the second (and third if it's a code change byte) to last byte
            are used for the barcode reader we don't really need to know that.
            */
            else if (i >= bytes.length - 2) {
                if (currentAi != null)
                    ais.put(currentAi.aiName, currentAi.aiValue);
                return;
            }
            //if the byte is the fnc1 byte then we know that the next value will be an AI
            else if (bytes[i] == fncOneByte) {
                nextByteIsAI = true;
            }
            /*
            If this byte is explicitly set as an AI, or if the current read values are the max
            value for the current ai value, then we store it and read in the next value as an
            ai byte
             */
            else if (nextByteIsAI || (currentAi != null && currentAiMax == currentAi.getAiValue()
                    .length())) {
                if (ApplicationIdentifiers.tripleAis.contains(byteValue) ||
                        ApplicationIdentifiers.quadrupleAis.contains(byteValue)) {
                    // We need to delay reading in data in order to complete the 3/4 byte
                    nextByteIsSecondaryAI = true;
                    primaryAI = byteValue;
                } else {
                    if (currentAi != null) ais.put(currentAi.aiName, currentAi.aiValue);
                    currentAi = new AI(ApplicationIdentifiers.names.get(byteValue));
                    currentAiMax = ApplicationIdentifiers.lengths.get(byteValue);
                }
                nextByteIsAI = false;
            }
            /*
                If we get this far, then the byte is clearly a value for the current AI,
                but only if it isn't the start byte
             */
            else if (bytes[i] != startByte && currentAi != null) {
                String mappedVal = mapCharacterFromByteValue(byteValue, currentCode);
                currentAi.addToValue(mappedVal);
            }
            i++;
        }
    }

    private String mapCharacterFromByteValue(String val, Integer currentCode) {        
        HashMap<String, String> currentCodeValues = ApplicationIdentifiers.values.get(currentCode);
        return currentCodeValues.get(val);
    }

    private boolean byteIsCodeSwitch(int currentCode, String byteValue) {
        switch (currentCode) {
            case ApplicationIdentifiers.CODE_A:
                return byteValue.equals("99") || byteValue.equals("100");
            case ApplicationIdentifiers.CODE_B:
                return byteValue.equals("99") || byteValue.equals("101");
            case ApplicationIdentifiers.CODE_C:
                return byteValue.equals("100") || byteValue.equals("101");
        }
        return false;
    }

    private int getNewCode(int currentCode, String byteValue) {
        switch (currentCode) {
            case ApplicationIdentifiers.CODE_A:
                if (byteValue.equals("99")) {
                    return ApplicationIdentifiers.CODE_C;
                }
                if (byteValue.equals("100")) {
                    return ApplicationIdentifiers.CODE_B;
                }
                break;
            case ApplicationIdentifiers.CODE_B:
                if (byteValue.equals("99")) {
                    return ApplicationIdentifiers.CODE_C;
                }
                if (byteValue.equals("101")) {
                    return ApplicationIdentifiers.CODE_A;
                }
                break;
            case ApplicationIdentifiers.CODE_C:
                //Trust me, this is supposed to be in this order.
                if (byteValue.equals("101")) {
                    return ApplicationIdentifiers.CODE_A;
                }
                if (byteValue.equals("100")) {
                    return ApplicationIdentifiers.CODE_B;
                }
                break;
        }
        return -1;
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
        return this.ais.containsKey("batchLot") ? this.ais.get("batchLot") : "";
    }

    public int getQuantity() {
        return this.ais.containsKey("count") ? Integer.parseInt(this.ais.get("count")) : 1;
    }
    private class AI {
        private String aiName;
        private String aiValue;

        public AI(String aiName) {
            this.aiName = aiName;
            this.aiValue = "";
        }

        /**
         * Adds the input value onto the end of the aiValue.
         *
         * @param val new character to add to aiValue
         */
        public void addToValue(String val) {
            this.aiValue += val;
        }

        /**
         * @return value as a string for the AI
         */
        public String getAiValue() {
            return aiValue;
        }
    }
}
