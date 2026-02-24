public class CityRescueImpl {
    public void initialise(int width, int height) throws InvalidGridException {

    }

    public int[] getGridSize() {

    }

    public void addObstacle(int x, int y) throws InvalidLocationException {

    }

    public void removeObstacle(int x, int y) throws InvalidLocationException {

    }

    public int addStation(String name, int x, int y) throws InvalidNameException, InvalidLocationException {

    }

    public void removeStation(int stationId) throws IDNotRecognisedException, IllegalStateException {

    }

    public void setStationCapacity(int stationId, int maxUnits) throws IDNotRecognisedException, InvalidCapacityException {

    }

    public int[] getStationIds() {

    }

    public int addUnit(int stationId, UnitType type) throws IDNotRecognisedException, InvalidUnitException, IllegalStateException {

    }

    public void decommissionUnit(int unitId) throws IDNotRecognisedException, IllegalStateException {

    }

    public void transferUnit(int unitId, int newStationId) throws IDNotRecognisedException, IllegalStateException {

    }

    public void setUnitOutOfService(int unitId, boolean outOfService) throws IDNotRecognisedException, IllegalStateException {

    }

    public int[] getUnitIds() {

    }

    public String viewUnit(int unitId) throws IDNotRecognisedException {

    }

    public int reportIncident(IncidentType type, int severity, int x, int y) throws InvalidSeverityException, InvalidLocationException {

    }

    public void cancelIncident(int incidentId) throws IDNotRecognisedException, IllegalStateException {

    }

    public void escalateIncident(int incidentId, int newSeverity) throws IDNotRecognisedException, InvalidSeverityException, IllegalStateException {

    }

    public int[] getIncidentIds() {

    }

    public String viewIncident(int incidentId) throws IDNotRecognisedException {

    }

    public void dispatch() {

    }

    public void tick() {

    }

    public String getStatus() {
        
    }
}
