package cityrescue;

import cityrescue.enums.IncidentType;
import cityrescue.enums.UnitType;
import cityrescue.exceptions.IDNotRecognisedException;
import cityrescue.exceptions.InvalidCapacityException;
import cityrescue.exceptions.InvalidGridException;
import cityrescue.exceptions.InvalidLocationException;
import cityrescue.exceptions.InvalidNameException;
import cityrescue.exceptions.InvalidSeverityException;
import cityrescue.exceptions.InvalidUnitException;
import cityrescue.exceptions.CapacityExceededException;

/**
 * CityRescueImpl (Starter)
 *
 * Your task is to implement the full specification.
 * You may add additional classes in any package(s) you like.
 */
public class CityRescueImpl implements CityRescue {

    // TODO: add fields (map, arrays for stations/units/incidents, counters, tick, etc.) added i think

    //constants 
    final int MAX_STATIONS = 20;
    final int MAX_UNITS = 50;
    final int MAX_INCIDENTS = 200;

    //attributes creating objects for each class
    CityMap cityMap;
    Station[] stations = new Station[MAX_STATIONS];
    Unit[] unit = new Unit[MAX_UNITS];
    Incident[] incidents = new Incident[MAX_INCIDENTS];

    //counts for station, unit, incident and obstacle 
    int stationCount; 
    int unitCount;
    int incidentCount; 
    int obstacleCount;

    //used for creating new objects with new id's 
    int nextStationId = 1;
    int nextUnitId = 1;
    int nextIncidentId = 1;

    int tick;

    @Override
    public void initialise(int width, int height) throws InvalidGridException {
        if (width <= 0 || height <= 0) {
            throw new InvalidGridException("Grid dimensions must be positive");
        }

        cityMap = new CityMap(width, height);
        stations = new Station[MAX_STATIONS];
        unit = new Unit[MAX_UNITS];
        incidents = new Incident[MAX_INCIDENTS];

        stationCount = 0;
        unitCount = 0;
        incidentCount = 0;
        obstacleCount = 0;

        nextStationId = 1;
        nextUnitId = 1;
        nextIncidentId = 1;
        tick = 0;
    }

    //getters for grid size, station/unit/incident ids, and status

    @Override
    public int[] getGridSize() {
        return new int[] {cityMap.getWidth(), cityMap.getHeight()};
    }

    @Override
    public int[] getStationIds() {
        int[] stationIds = new int[stationCount];
        
        for (int i = 0; i <= stationCount - 1; i++) {
            stationIds[i] = stations[i].getStationId();
        }

        return stationIds;
    }

    @Override
    public int[] getUnitIds() {
        int[] unitIds = new int[unitCount];
        
        for (int i = 0; i <= unitCount - 1; i++) {
            unitIds[i] = unit[i].getUnitId();
        }

        return unitIds;
    }

    @Override
    public int[] getIncidentIds() {
        int[] incidentIds = new int[incidentCount];
        
        for (int i = 0; i <= incidentCount - 1; i++) {
            incidentIds = incidents[i].getIncidentId();
        }

        return incidentIds;
    }

    @Override
    public String getStatus() {
        String report = "TICK=" + toString(tick);
        report += "\nSTATIONS=" + toString(stationCount) + "UNITS=" + toString(unitCount) + "INCIDENTS=" + incidentCount + "OBSTACLES=" + toString(obstacleCount);
        
        report += "\nINCIDENTS\n";
        int[] incidentIds = getIncidentIds();
        for (int i = 0; i <= incidentCount - 1; i++) {
            report += viewIncident(incidentsIds[i]) + "\n";
        }

        report += "\nUNITS\n";
        int[] unitIds = getUnitIds();
        for (int i = 0; i <= unitCount - 1; i++) {
            report += viewUnit(unitIds[i]) + "\n";
        }

        return report;
    }


    //methods for obsticles 

    @Override
    public void addObstacle(int x, int y) throws InvalidLocationException {
        if (cityMap.inBounds(x, y) != true || cityMap.isLegalCell(x, y) != true) {
            throw new InvalidLocationException("Coordinates are out of bounds");
        }

        if (cityMap.isLegalCell(x, y)) {
            cityMap.addObstacle(x, y);
            obstacleCount++;
        }
    }

    @Override
    public void removeObstacle(int x, int y) throws InvalidLocationException {
        if (cityMap.isBounds(x, y) != true) {
            throw new InvalidLocationException("Coordinates are out of bounds");
        }
        
        if (cityMap.isLegalCell(x, y)) {
            cityMap.removeObstacle(x, y);
            obstacleCount--;
        }
    }



    //methods for stations, adding, removing and setting the capactiy  

    @Override
    public int addStation(String name, int x, int y) throws InvalidNameException, InvalidLocationException {
        //exception handling 
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidNameException("Station name must not be blank");
        }
        if (!cityMap.inBounds(x, y) || cityMap.isBlocked(x, y)) {
            throw new InvalidLocationException("Station location invalid (out of bounds or blocked)");
        }
        if (stationCount >= MAX_STATIONS) {
            throw new IllegalStateException("Maximum stations reached");
        }

        //determine the station id for the new station being 1 above the previous 
        int id = nextStationId++;
        //creating the new station object 
        Station s = new Station(id, name.trim(), x, y, DEFAULT_STATION_CAPACITY);

        //adding to the station count
        stations[stationCount++] = s;
        return id;
    }

    @Override
    public void removeStation(int stationId) throws IDNotRecognisedException, IllegalStateException {
        //storing the id of the going to be removed station in a temporary variable 
        int idx = indexOfStation(stationId);
        
        //exception handling
        if (idx == -1) {
            throw new IDNotRecognisedException("Station ID not recognised");
        }

        Station s = stations[idx];
        if (s.getUnitCount() > 0) {
            throw new IllegalStateException("Station still owns units");
        }

        //remove by shifting to ensure there is not a gap in the array 
        for (int i = idx; i < stationCount - 1; i++) {
            stations[i] = stations[i + 1];
        }
        stations[stationCount - 1] = null;
        stationCount--;
    }


    @Override
    public void setStationCapacity(int stationId, int maxUnits) throws IDNotRecognisedException, InvalidCapacityException {

        //temporary variable to store the index of the station
        int idx = indexOfStation(stationId);

        //exception handling 
        if (idx == -1) {
            throw new IDNotRecognisedException("Station ID not recognised");
        }

        //finding the station object with the id and storing it in a temporary variable
        Station s = stations[idx];

        //exception handling for the capacity of the station, ensuring it is not negative and that it is not less than the number of units currently at the station
        if (maxUnits <= 0 || maxUnits < s.getUnitCount()) {
            throw new InvalidCapacityException("Invalid station capacity");
        }

        //apply the new capactity to the station object
        s.setCapacity(maxUnits);
    }


    //methods for units

    @Override
    public int addUnit(int stationId, UnitType type) throws IDNotRecognisedException, InvalidUnitException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void decommissionUnit(int unitId) throws IDNotRecognisedException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void transferUnit(int unitId, int newStationId) throws IDNotRecognisedException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void setUnitOutOfService(int unitId, boolean outOfService) throws IDNotRecognisedException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }


    @Override
    public String viewUnit(int unitId) throws IDNotRecognisedException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }





    //method for incidents 

    @Override
    public int reportIncident(IncidentType type, int severity, int x, int y) throws InvalidSeverityException, InvalidLocationException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void cancelIncident(int incidentId) throws IDNotRecognisedException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void escalateIncident(int incidentId, int newSeverity) throws IDNotRecognisedException, InvalidSeverityException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String viewIncident(int incidentId) throws IDNotRecognisedException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }



    //methods for dispatching and ticking

    @Override
    public void dispatch() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void tick() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }


    
    
    
}

