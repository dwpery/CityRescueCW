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

    //attributes 
    CityMap cityMap;
    Station[] stations = new Station[MAX_STATIONS];
    Unit[] unit = new Unit[MAX_UNITS];
    Incident[] incidents = new Incident[MAX_INCIDENTS];
    int stationCount, unitCount, incidentCount, obstacleCount;
    int nextStationId = 1, nextUnitId = 1, nextIncidentId = 1;
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
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void removeObstacle(int x, int y) throws InvalidLocationException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }



    //methods for stations 

    @Override
    public int addStation(String name, int x, int y) throws InvalidNameException, InvalidLocationException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void removeStation(int stationId) throws IDNotRecognisedException, IllegalStateException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void setStationCapacity(int stationId, int maxUnits) throws IDNotRecognisedException, InvalidCapacityException {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
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

