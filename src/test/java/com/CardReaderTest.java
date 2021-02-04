package com;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

public class CardReaderTest {

    @Test
    public void shouldReturnDurationInSeconds() throws ParseException {
        String start = "22/1/18 13:00";
        String end = "22/1/18 13:05";
        CardReader cardReader = new CardReader();
        long duration = cardReader.calculateDuration(start, end);
        Assert.assertEquals(300, duration);
    }

    @Test(expected = ParseException.class)
    public void shouldReturnParseException() throws ParseException {
        String start = "22-1-18 13:00";
        String end = "22/1/18 13:05";
        CardReader cardReader = new CardReader();
        cardReader.calculateDuration(start, end);
    }

    @Test
    public void shouldReturnChargeAmountBasedStopId(){
        String start = "Stop1";
        String end = "Stop2";
        CardReader cardReader = new CardReader();
        Assert.assertEquals("$3.25", cardReader.calculateChargeAmount(start, end));
    }

    @Test
    public void shouldReturnChargeAmountForSameStopId(){
        String start = "Stop1";
        String end = "Stop1";
        CardReader cardReader = new CardReader();
        Assert.assertEquals("$0.00", cardReader.calculateChargeAmount(start, end));
    }

    @Test
    public void shouldReturnZeroForInvalidStopId(){
        String start = "Stop4";
        String end = "Stop3";
        CardReader cardReader = new CardReader();
        Assert.assertEquals("0", cardReader.calculateChargeAmount(start, end));
        Assert.assertEquals("0", cardReader.calculateIncompleteCharge(start));
    }

    @Test
    public void shouldReturnMaximumChargeAmountForIncomplete(){
        String start = "Stop1";
        CardReader cardReader = new CardReader();
        Assert.assertEquals("$7.30", cardReader.calculateIncompleteCharge(start));
    }

    @Test
    public void shouldReturnJsonForCompletedTrips() throws IOException, ParseException {
        String content1 = new String(Files.readAllBytes(Paths.get("src/test/resources/data1.json")));
        String content2 = new String(Files.readAllBytes(Paths.get("src/test/resources/data2.json")));
        JSONObject on = new JSONObject(content1);
        JSONObject off = new JSONObject(content2);
        CardReader cardReader = new CardReader();
        JSONObject completedTrips = cardReader.calculateTrips(on, off);
        Assert.assertEquals(300, completedTrips.getInt("DurationSecs"));
        Assert.assertEquals("Completed", completedTrips.getString("Status"));
        Assert.assertEquals("$3.25", completedTrips.getString("ChargeAmount"));
    }

    @Test
    public void shouldReturnJsonForInCancelledTrips() throws IOException, ParseException {
        String content1 = new String(Files.readAllBytes(Paths.get("src/test/resources/data1.json")));
        String content2 = new String(Files.readAllBytes(Paths.get("src/test/resources/data1.json")));
        JSONObject on = new JSONObject(content1);
        JSONObject off = new JSONObject(content2);
        CardReader cardReader = new CardReader();
        JSONObject canceledTrips =  cardReader.calculateTrips(on, off);
        Assert.assertEquals("N/A", canceledTrips.getString("DurationSecs"));
        Assert.assertEquals("Cancelled", canceledTrips.getString("Status"));
        Assert.assertEquals("$0.00", canceledTrips.getString("ChargeAmount"));
    }

    @Test
    public void shouldReturnJsonForInCompletedTrips() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/data3.json")));
        JSONObject on = new JSONObject(content);
        CardReader cardReader = new CardReader();
        JSONObject incompleteTrips = cardReader.calculateIncompleteTrips(on);
        Assert.assertEquals("N/A", incompleteTrips.getString("DurationSecs"));
        Assert.assertEquals("Incomplete", incompleteTrips.getString("Status"));
        Assert.assertEquals("$7.30", incompleteTrips.getString("ChargeAmount"));
    }

}
