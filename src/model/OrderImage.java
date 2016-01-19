package model;

import util.SimpleDateParser;

import java.util.ArrayList;

public class OrderImage {
    private long orderId;
    private long patientId;
    private SimpleDateParser.SimpleDate simpleDate;
    private ArrayList<String> orderImage;   //url of images

    public OrderImage(long orderId, long patientId, SimpleDateParser.SimpleDate simpleDate, ArrayList<String> orderImage) {
        this.orderId = orderId;
        this.patientId = patientId;
        this.simpleDate = simpleDate;
        this.orderImage = orderImage;
    }

    /**
     * Parse urls divided by ","
     *
     * @param s String value of urls
     * @return ArrayList<String> excluding empty invalid values
     */
    public static ArrayList<String> parseUrlsString(String s) {
        String[] input = s.trim().split(",");
        ArrayList<String> result = new ArrayList<>();

        for (String str : input) {
            if (!str.trim().isEmpty()) {
                result.add(str.trim());
            }
        }

        return result;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getPatientId() {
        return patientId;
    }

    public SimpleDateParser.SimpleDate getSimpleDate() {
        return simpleDate;
    }

    public ArrayList<String> getOrderImage() {
        return orderImage;
    }
}
