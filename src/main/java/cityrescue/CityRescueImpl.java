package cityrescue;

import cityrescue.enums.IncidentStatus;
import cityrescue.enums.IncidentType;
import cityrescue.enums.UnitStatus;
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

    //constants 
    private static final int MAX_STATIONS = 20;
    private static final int MAX_UNITS = 50;
    private static final int MAX_INCIDENTS = 200;

    private static final int DEFAULT_STATION_CAPACITY = MAX_UNITS;

    //attributes creating objects for each class
    private CityMap cityMap;

    private Station[] stations = new Station[MAX_STATIONS];
    private Unit[] units = new Unit[MAX_UNITS];
    private Incident[] incidents = new Incident[MAX_INCIDENTS];

    //counts for station, unit, incident and obstacle 
    private int stationCount; 
    private int unitCount;
    private int incidentCount; 
    private int obstacleCount;

    //used for creating new objects with new id's 
    private int nextStationId = 1;
    private int nextUnitId = 1;
    private int nextIncidentId = 1;

    private int tick;

    @Override
    public void initialise(int width, int height) throws InvalidGridException {
        if (width <= 0 || height <= 0) {
            throw new InvalidGridException("Grid dimensions must be positive");
        }

        cityMap = new CityMap(width, height);
        stations = new Station[MAX_STATIONS];
        units = new Unit[MAX_UNITS];
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
            unitIds[i] = units[i].getUnitId();
        }

        return unitIds;
    }

    @Override
    public int[] getIncidentIds() {
        int[] incidentIds = new int[incidentCount];
        
        for (int i = 0; i <= incidentCount - 1; i++) {
            incidentIds[i] = incidents[i].getIncidentId();
        }

        return incidentIds;
    }

    //gets the status of the city rescue system in the specified format, with stations, units and incidents in ascending order by their id
    @Override
    public String getStatus() {

        //using a stringbuilder to put strings together 
        StringBuilder sb = new StringBuilder();

        //header information with tick and counts of stations, units, incidents and obstacles
        sb.append("TICK=").append(tick).append("\n");
        sb.append("STATIONS=").append(stationCount)
          .append(" UNITS=").append(unitCount)
          .append(" INCIDENTS=").append(incidentCount)
          .append(" OBSTACLES=").append(cityMap.getObstacleCount())
          .append("\n");

        //incidents and units in ascending order by their id
        sb.append("INCIDENTS").append("\n");
        int[] incIds = getIncidentIds();
        for (int i = 0; i < incIds.length; i++) {
            Incident inc = getIncidentById(incIds[i]);
            if (inc != null) {
                sb.append(formatIncidentLine(inc)).append("\n");
            }
        }

        //units in ascending order by their id
        sb.append("UNITS").append("\n");
        int[] unitIds = getUnitIds();
        for (int i = 0; i < unitIds.length; i++) {
            Unit u = getUnitById(unitIds[i]);
            if (u != null) {
                sb.append(formatUnitLine(u)).append("\n");
            }
        }

        //remove trailing newline
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    //methods for obstacles

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
        if (cityMap.inBounds(x, y) != true) {
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

        //exception handling for the station id, unit type and maximum units
        int sIdx = indexOfStation(stationId);
        if (sIdx == -1) {
            throw new IDNotRecognisedException("Station ID not recognised");
        }
        if (type == null) {
            throw new InvalidUnitException("Unit type must not be null");
        }
        if (unitCount >= MAX_UNITS) {
            throw new IllegalStateException("Maximum units reached");
        }

        Station s = stations[sIdx];
        if (!s.hasSpace()) {
            throw new IllegalStateException("Station has no free capacity");
        }

        //determine the unit id for the new unit as 1 above the previous unit
        int id = nextUnitId++;

        //create the new unit object based on the type of unit being added
        Unit u;
        if (type == UnitType.AMBULANCE) {
            u = new Ambulance(id, stationId, s.getX(), s.getY());
        } else if (type == UnitType.FIRE_ENGINE) {
            u = new FireEngine(id, stationId, s.getX(), s.getY());
        } else if (type == UnitType.POLICE_CAR) {
            u = new PoliceCar(id, stationId, s.getX(), s.getY());
        } else {
            //exception handling for an unrecognised unit type 
            throw new InvalidUnitException("Unrecognised unit type");
        }

        //apply the new unit to the units array and add the unit to a station 
        units[unitCount++] = u;
        s.addUnitId(id);

        return id;
    }

    @Override
    public void decommissionUnit(int unitId) throws IDNotRecognisedException, IllegalStateException {

        //exception handling for the unit id and the status of the unit
        int uIdx = indexOfUnit(unitId);
        if (uIdx == -1) {
            throw new IDNotRecognisedException("Unit ID not recognised");
        }

        Unit u = units[uIdx];
        if (u.getStatus() == UnitStatus.EN_ROUTE || u.getStatus() == UnitStatus.AT_SCENE) {
            throw new IllegalStateException("Unit is busy and cannot be decommissioned");
        }

        //remove ownership from its station (with validation that the station exists itself)
        int sIdx = indexOfStation(u.getHomeStationId());
        if (sIdx != -1) {
            stations[sIdx].removeUnitId(unitId);
        }

        //removed from the units array by shifting to ensure there is not a gap in the array
        for (int i = uIdx; i < unitCount - 1; i++) {
            units[i] = units[i + 1];
        }
        units[unitCount - 1] = null;
        unitCount--;
    }

    @Override
    public void transferUnit(int unitId, int newStationId) throws IDNotRecognisedException, IllegalStateException {

        //exception handling for the unit id, station id, status of the unit and capacity of the station    
        int uIdx = indexOfUnit(unitId);
        if (uIdx == -1) {
            throw new IDNotRecognisedException("Unit ID not recognised");
        }
        int newSIdx = indexOfStation(newStationId);
        if (newSIdx == -1) {
            throw new IDNotRecognisedException("Station ID not recognised");
        }

        //unit must be idle to transfer and handle if not 
        Unit u = units[uIdx];
        if (u.getStatus() != UnitStatus.IDLE) {
            throw new IllegalStateException("Unit must be IDLE to transfer");
        }

        //check the destination station has space and handle if not
        Station newS = stations[newSIdx];
        if (!newS.hasSpace()) {
            throw new IllegalStateException("Destination station has no free capacity");
        }

        // Remove from old station (if it exists)
        int oldStationId = u.getHomeStationId();
        int oldSIdx = indexOfStation(oldStationId);

        // If the old station exists, remove the unit from it
        if (oldSIdx != -1) {
            stations[oldSIdx].removeUnitId(unitId);
        }

        //apply the transfer by adding the unit to the new station and updating the unit's home station and location to match the new station
        newS.addUnitId(unitId);
        u.setHomeStationId(newStationId);
        u.setLocation(newS.getX(), newS.getY());
    }

    @Override
    public void setUnitOutOfService(int unitId, boolean outOfService) throws IDNotRecognisedException, IllegalStateException {

        //exception handling for the unit id and the status of the unit when setting out of service or returning to service
        int uIdx = indexOfUnit(unitId);
        if (uIdx == -1) {
            throw new IDNotRecognisedException("Unit ID not recognised");
        }

        //if setting out of service, unit must be idle. If returning to service, unit must currently be out of service.
        Unit u = units[uIdx];

        if (outOfService) {
            if (u.getStatus() != UnitStatus.IDLE) {
                throw new IllegalStateException("Unit must be IDLE to go out of service");
            }
            u.setStatus(UnitStatus.OUT_OF_SERVICE);
        } else {
            //return to IDLE only if currently out of service
            if (u.getStatus() == UnitStatus.OUT_OF_SERVICE) {
                u.setStatus(UnitStatus.IDLE);
            }
        }
    }

    @Override
    public String viewUnit(int unitId) throws IDNotRecognisedException {
        //exception handling for the unit id
        Unit u = getUnitById(unitId);
        if (u == null) {
            throw new IDNotRecognisedException("Unit ID not recognised");
        }
        return formatUnitLine(u);
    }





    //method for incidents 

    @Override
    public int reportIncident(IncidentType type, int severity, int x, int y) throws InvalidSeverityException, InvalidLocationException {

        //exception handling for the incident type, severity and location, and maximum incidents
        if (type == null) {
            // Not declared in interface, but safer than silently breaking state.
            throw new IllegalArgumentException("Incident type must not be null");
        }
        if (severity < 1 || severity > 5) {
            throw new InvalidSeverityException("Severity must be 1..5");
        }
        if (!cityMap.inBounds(x, y) || cityMap.isBlocked(x, y)) {
            throw new InvalidLocationException("Incident location invalid (out of bounds or blocked)");
        }
        if (incidentCount >= MAX_INCIDENTS) {
            throw new IllegalStateException("Maximum incidents reached");
        }

        //determine the incident id for the new incident as 1 above the previous
        int id = nextIncidentId++;
        Incident inc = new Incident(id, type, severity, x, y);
        //apply the new incident to the incidents array
        incidents[incidentCount++] = inc;
        return id;
    }

    @Override
    public void cancelIncident(int incidentId) throws IDNotRecognisedException, IllegalStateException {

        //exception handling for the incident id and the status of the incident
        Incident inc = getIncidentById(incidentId);
        if (inc == null) {
            throw new IDNotRecognisedException("Incident ID not recognised");
        }

        //can only cancel if the incident is currently reported or dispatched, and if dispatched, release the unit immediately
        IncidentStatus st = inc.getStatus();
        if (st != IncidentStatus.REPORTED && st != IncidentStatus.DISPATCHED) {
            throw new IllegalStateException("Incident cannot be cancelled in its current state");
        }

        //if dispatched, release the unit immediately by setting it to idle and unassigning it from the incident, and handle if the unit is not found for some reason
        if (st == IncidentStatus.DISPATCHED) {
            int uId = inc.getAssignedUnitId();
            Unit u = getUnitById(uId);
            if (u != null) {
                u.setStatus(UnitStatus.IDLE);
                u.clearIncident();
            }
            inc.unassignUnit();
        }

        inc.setStatus(IncidentStatus.CANCELLED);
    }

    @Override
    public void escalateIncident(int incidentId, int newSeverity) throws IDNotRecognisedException, InvalidSeverityException, IllegalStateException {

        //exception handling for the incident id, new severity and status of the incident
        Incident inc = getIncidentById(incidentId);
        if (inc == null) {
            throw new IDNotRecognisedException("Incident ID not recognised");
        }

        if (newSeverity < 1 || newSeverity > 5) {
            throw new InvalidSeverityException("Severity must be 1..5");
        }

        if (inc.getStatus() == IncidentStatus.RESOLVED || inc.getStatus() == IncidentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot escalate a resolved/cancelled incident");
        }

        //apply the new severity to the incident
        inc.setSeverity(newSeverity);
    }

    @Override
    public String viewIncident(int incidentId) throws IDNotRecognisedException {

        //exception handling for the incident id
        Incident inc = getIncidentById(incidentId);
        if (inc == null) {
            throw new IDNotRecognisedException("Incident ID not recognised");
        }
        return formatIncidentLine(inc);
    }



    //methods for dispatching and ticking

    @Override
    public void dispatch() {
        // for each reported incident, find the best available unit and dispatch it
        // applying the changes to the unit and incident status and assignments. 
        // If no units are available, leave the incident as reported.
        
        int[] incIds = getIncidentIds();
        for (int i = 0; i < incIds.length; i++) {
            Incident inc = getIncidentById(incIds[i]);
            if (inc == null) continue;

            if (inc.getStatus() != IncidentStatus.REPORTED) {
                continue;
            }

            Unit best = chooseBestUnitFor(inc);
            if (best == null) {
                continue; //done if there are none available
            }

            //apply the dispatch by assigning the unit to the incident and setting the appropriate statuses
            inc.assignUnit(best.getUnitId());
            inc.setStatus(IncidentStatus.DISPATCHED);

            best.assignIncident(inc.getIncidentId());
            best.setStatus(UnitStatus.EN_ROUTE);
        }
    }


    //tick methods 
    @Override
    public void tick() {
        tick++;

        
        int[] unitIds = getUnitIds();
        for (int i = 0; i < unitIds.length; i++) {
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;

            if (u.getStatus() == UnitStatus.EN_ROUTE) {
                Incident target = getIncidentById(u.getAssignedIncidentId());
                if (target != null) {
                    moveOneStep(u, target.getX(), target.getY());
                }
            }
        }

        
        for (int i = 0; i < unitIds.length; i++) {
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;

            if (u.getStatus() == UnitStatus.EN_ROUTE) {
                Incident target = getIncidentById(u.getAssignedIncidentId());
                if (target != null && u.getX() == target.getX() && u.getY() == target.getY()) {
                    u.setStatus(UnitStatus.AT_SCENE);
                    target.setStatus(IncidentStatus.IN_PROGRESS);
                    u.startWork();
                }
            }
        }

        
        for (int i = 0; i < unitIds.length; i++) {
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;

            if (u.getStatus() == UnitStatus.AT_SCENE) {
                u.tickWork();
            }
        }

        
        int[] incIds = getIncidentIds();
        for (int i = 0; i < incIds.length; i++) {
            Incident inc = getIncidentById(incIds[i]);
            if (inc == null) continue;

            if (inc.getStatus() == IncidentStatus.IN_PROGRESS) {
                Unit u = getUnitById(inc.getAssignedUnitId());
                if (u != null && u.getStatus() == UnitStatus.AT_SCENE && u.isWorkComplete()) {
                    inc.setStatus(IncidentStatus.RESOLVED);
                    u.setStatus(UnitStatus.IDLE);
                    u.clearIncident();
                    
                }
            }
        }
    }



    //helper methods used throughout 

        private Unit chooseBestUnitFor(Incident inc) {
        Unit best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i < unitCount; i++) {
            Unit u = units[i];
            if (u == null) continue;

            if (u.getStatus() != UnitStatus.IDLE) continue;
            if (!u.canHandle(inc.getType())) continue;
            if (u.getAssignedIncidentId() != -1) continue;

            int dist = manhattan(u.getX(), u.getY(), inc.getX(), inc.getY());

            if (best == null) {
                best = u;
                bestDist = dist;
                continue;
            }

            
            if (dist < bestDist) {
                best = u;
                bestDist = dist;
            } else if (dist == bestDist) {
                if (u.getUnitId() < best.getUnitId()) {
                    best = u;
                } else if (u.getUnitId() == best.getUnitId()) {
                    if (u.getHomeStationId() < best.getHomeStationId()) {
                        best = u;
                    }
                }
            }
        }

        return best;
    }

    private void moveOneStep(Unit u, int targetX, int targetY) {
        

        int x = u.getX();
        int y = u.getY();

        int[][] moves = new int[][] {
            { x, y - 1 }, // N
            { x + 1, y }, // E
            { x, y + 1 }, // S
            { x - 1, y }  // W
        };

        int currentDist = manhattan(x, y, targetX, targetY);

        
        for (int i = 0; i < moves.length; i++) {
            int nx = moves[i][0];
            int ny = moves[i][1];
            if (!cityMap.isLegalCell(nx, ny)) continue;

            int nd = manhattan(nx, ny, targetX, targetY);
            if (nd < currentDist) {
                u.setLocation(nx, ny);
                return;
            }
        }

        
        for (int i = 0; i < moves.length; i++) {
            int nx = moves[i][0];
            int ny = moves[i][1];
            if (!cityMap.isLegalCell(nx, ny)) continue;

            u.setLocation(nx, ny);
            return;
        }

        
    }

    private String formatIncidentLine(Incident inc) {
        String unitStr = (inc.getAssignedUnitId() <= 0) ? "-" : String.valueOf(inc.getAssignedUnitId());
        return "I#" + inc.getIncidentId()
            + " TYPE=" + inc.getType()
            + " SEV=" + inc.getSeverity()
            + " LOC=(" + inc.getX() + "," + inc.getY() + ")"
            + " STATUS=" + inc.getStatus()
            + " UNIT=" + unitStr;
    }

    private String formatUnitLine(Unit u) {
        String incStr = (u.getAssignedIncidentId() <= 0) ? "-" : String.valueOf(u.getAssignedIncidentId());

        String base = "U#" + u.getUnitId()
            + " TYPE=" + u.getType()
            + " HOME=" + u.getHomeStationId()
            + " LOC=(" + u.getX() + "," + u.getY() + ")"
            + " STATUS=" + u.getStatus()
            + " INCIDENT=" + incStr;

        if (u.getStatus() == UnitStatus.AT_SCENE) {
            base += " WORK=" + u.getWorkTicksRemaining();
        }
        return base;
    }

    //helper methods for getting attributes by their id

    private Station getStationById(int stationId) {
        int idx = indexOfStation(stationId);
        return (idx == -1) ? null : stations[idx];
    }

    private Unit getUnitById(int unitId) {
        int idx = indexOfUnit(unitId);
        return (idx == -1) ? null : units[idx];
    }

    private Incident getIncidentById(int incidentId) {
        int idx = indexOfIncident(incidentId);
        return (idx == -1) ? null : incidents[idx];
    }

    //helper methods for getting the index of an attribute in its array by its id, returning -1 if not found
    private int indexOfStation(int stationId) {
        for (int i = 0; i < stationCount; i++) {
            if (stations[i] != null && stations[i].getStationId() == stationId) return i;
        }
        return -1;
    }

    private int indexOfUnit(int unitId) {
        for (int i = 0; i < unitCount; i++) {
            if (units[i] != null && units[i].getUnitId() == unitId) return i;
        }
        return -1;
    }

    private int indexOfIncident(int incidentId) {
        for (int i = 0; i < incidentCount; i++) {
            if (incidents[i] != null && incidents[i].getIncidentId() == incidentId) return i;
        }
        return -1;
    }

    //movement rule 
    private static int manhattan(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        if (dx < 0) dx = -dx;
        int dy = y1 - y2;
        if (dy < 0) dy = -dy;
        return dx + dy;
    }

}

