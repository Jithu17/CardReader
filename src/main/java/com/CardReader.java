package com;

import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;


public class CardReader {

    public static void main(String[] args) throws ParseException, IOException {

        List<String> csvRows = null;
        //ead the csv file
        try(Stream<String> reader = Files.lines(Paths.get("src/main/resources/taps.csv"))){
            csvRows = reader.collect(Collectors.toList());
        }catch(Exception e){
            e.printStackTrace();
        }
        String json = null;
        CardReader cardReader = new CardReader();
        if(csvRows != null){
            //create a json object for each row
            json = cardReader.csvToJson(csvRows);
        }
        cardReader.calculateTripsAndSaveCSV(json);
    }

    public void calculateTripsAndSaveCSV(String taps) throws ParseException, IOException {
        //convert the json string to json array of object
        JSONArray tapAr = new JSONArray(taps);
        JSONArray completedArr = new JSONArray();
        HashMap<String, Integer> tapRecord = new HashMap<>();
        for (int i = 0; i < tapAr.length(); i++){
            //initialize the json object
            JSONObject jsonObj = tapAr.getJSONObject(i);
            //check if the record is present in the hashmap
            if(tapRecord.containsKey(jsonObj.getString("UniqueId"))){
                //get the previous object i.e. would ne tap on
                JSONObject previousObj = tapAr.getJSONObject(tapRecord.get(jsonObj.getString("UniqueId")));
                //calculate the charge amount and duration
                JSONObject trips = calculateTrips(previousObj, jsonObj);
                //put the new object in json array
                completedArr.put(trips);
                tapRecord.remove(jsonObj.getString("UniqueId"));
            } else {
                //add the current record to Hashmap with UniqueId value and index of the object in an array
                tapRecord.put(jsonObj.getString("UniqueId"), i);
            }
        }

        //check if there is incomplete trip
        if(tapRecord.size() > 0){
            for(int index: tapRecord.values()){
                //get the charge amount for the incomplete trip
                JSONObject incompleteTrips = calculateIncompleteTrips(tapAr.getJSONObject(index));
                //put the new object in json array
                completedArr.put(incompleteTrips);
            }
        }
        //create a csv file for the output trips
        saveToCsv(completedArr);
    }

    public JSONObject calculateTrips(JSONObject on, JSONObject off) throws ParseException {
        JSONObject completedTrip = new JSONObject();
        //create a new object for each trip
        completedTrip.put("Started", on.getString("DateTimeUTC"));
        completedTrip.put("Finished", off.getString("DateTimeUTC"));
        completedTrip.put("FromStopId", on.getString("StopId"));
        completedTrip.put("ToStopId", off.getString("StopId"));
        completedTrip.put("CompanyID", on.getString("CompanyID"));
        completedTrip.put("BusID", on.getString("BusID"));
        completedTrip.put("PAN", on.getString("PAN"));
        String chargeAmount = calculateChargeAmount(on.getString("StopId"), off.getString("StopId"));
        completedTrip.put("ChargeAmount", chargeAmount);
        //check whether the tap on and tap off are same stop
        if(on.getString("StopId").equals(off.getString("StopId"))){
            completedTrip.put("Status", "Cancelled");
            completedTrip.put("DurationSecs", "N/A");
        } else {
            long duration = calculateDuration(on.getString("DateTimeUTC"), off.getString("DateTimeUTC"));
            completedTrip.put("DurationSecs", duration);
            completedTrip.put("Status", "Completed");
        }

        return completedTrip;
    }

    public JSONObject calculateIncompleteTrips(JSONObject on) {

        //create a json object for incomplete trip
        JSONObject incompleteTrip = new JSONObject();
        incompleteTrip.put("Started", on.getString("DateTimeUTC"));
        incompleteTrip.put("Finished", "N/A");
        incompleteTrip.put("DurationSecs", "N/A");
        incompleteTrip.put("FromStopId", on.getString("StopId"));
        incompleteTrip.put("ToStopId", "N/A");
        incompleteTrip.put("CompanyID", on.getString("CompanyID"));
        incompleteTrip.put("BusID", on.getString("BusID"));
        incompleteTrip.put("PAN", on.getString("PAN"));
        incompleteTrip.put("Status", "Incomplete");
        String chargeAmount = calculateIncompleteCharge(on.getString("StopId"));
        incompleteTrip.put("ChargeAmount", chargeAmount);
        return incompleteTrip;
    }

    public long calculateDuration(String start, String end) throws ParseException {

        //convert string to Date format
        Date startDate = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(start);
        Date endDate   = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(end);

        long duration  = endDate.getTime() - startDate.getTime();
        return TimeUnit.MILLISECONDS.toSeconds(duration);
    }

    public String calculateChargeAmount(String startStop, String endStop){
        switch (startStop){
            case "Stop1":
                if(endStop.equals("Stop2"))
                    return "$3.25";
                else if(endStop.equals("Stop3"))
                    return "$7.30";
                else
                    return "$0.00";
            case "Stop2":
                if(endStop.equals("Stop1"))
                    return "$3.25";
                else if(endStop.equals("Stop3"))
                    return "$5.50";
                else
                    return "$0.00";
            case "Stop3":
                if(endStop.equals("Stop1"))
                    return "$7.30";
                else if(endStop.equals("Stop2"))
                    return "$5.50";
                else
                    return "$0.00";
            default:
                return "0";
        }
    }

    public String calculateIncompleteCharge(String startStop){
        switch (startStop){
            case "Stop1":
            case "Stop3":
                return "$7.30";
            case "Stop2":
                return "$5.50";
            default:
                return "0";
        }
    }

    public void saveToCsv(JSONArray tripOutput) throws IOException {
        //create a new csv file
        File file = new File("src/main/resources/trips.csv");
        //convert the jsonarray to string -- comma-delimited
        String csv = CDL.toString(tripOutput);
        FileUtils.writeStringToFile(file, csv);
        System.out.println("Data has been Successfully Written to "+ file);
    }


    public String csvToJson(List<String> csv){

        //remove empty lines
        //this will affect permanently the list.
        //be careful if you want to use this list after executing this method
        csv.removeIf(e -> e.trim().isEmpty());

        //csv is empty or have declared only columns
        if(csv.size() <= 1){
            return "[]";
        }

        //get first line = columns names
        String[] columns = csv.get(0).split(",");

        //get all rows
        StringBuilder json = new StringBuilder("[\n");
        csv.subList(1, csv.size()) //substring without first row(columns)
                .stream()
                .map(e -> e.split(","))
                .filter(e -> e.length == columns.length) //values size should match with columns size
                .forEach(row -> {

                    json.append("\t{\n");

                    for(int i = 0; i < columns.length; i++){
                        json.append("\t\t\"")
                                .append(columns[i])
                                .append("\" : \"")
                                .append(row[i])
                                .append("\",\n"); //comma-1
                    }

                    //replace comma-1 with \n
                    json.replace(json.lastIndexOf(","), json.length(), "\n");

                    json.append("\t},"); //comma-2

                });

        //remove comma-2
        json.replace(json.lastIndexOf(","), json.length(), "");

        json.append("\n]");

        return json.toString();

    }
}

