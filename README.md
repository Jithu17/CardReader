# CardReader

## Description

``The Card Reader application is executed by running in the CardReader class. The application reads the data from the 
taps.csv files where all the tap records are present and converts each record
into an array of json objects. The calculation of the charge amount based on the taps is calculated and output is created as a json object. 
The json object are the converted to csv format i.e. each object being a row and saved under resources directory``

### How to run the application
* git clone this repository and go to CardReader directory and run:
* mvn clean
* mvn install
* mvn complie exec:java
* The output file will be present in src/main/resources as trips.csv

##### Assumptions

``Every taps will have a unique ID and the unique ID will be the same for a tap on and tap off.``
