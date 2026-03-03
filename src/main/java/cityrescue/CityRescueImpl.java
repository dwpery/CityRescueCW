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
 * Implementation of the {@code CityRescue} interface
 * <p> This class manages the full lifecycle of the city response system, including
 * <ul>
 *   <li>Grid installation and obstacle management</li>
 *   <li>Station createion, removal and capacity control</li>
 *   <li>Unit managemnet, including creation, transfoer, decomission and service status</li>
 *   <li>Incident management, including reporting, escalation, cancelling and resolution </li>
 *   <li>Dispatch logic and time progressing via {@code tick()}</li>
 * </ul>
 * 
 * <p>The implementation uses fixed-size arrays with defined upper limits for stations, units and incidents
 * <p>Identifiers are allocatied in sequence and remain unique for the the duration of the system 
 * 
 * <p>Simulation time advances in steps using {@link #tick()}, in which units move, begin work at scence, and complete incidents
 * 
 * <p>This implementant uses:
 * <ul>
 *   <li>Manhatten distance for dispatch decisions</li>
 *   <li>movement: using a North, East, South, West movement priority </li>
 *   <li>lowest unit ID tie-breaking during dispatch.</li>
 * </ul>
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

    //used for creating new objects with new id's 
    private int nextStationId = 1;
    private int nextUnitId = 1;
    private int nextIncidentId = 1;

    private int tick;

    /**
     * Inistalise the rescue system with a new grid
     * 
     * <p>All previous stored stations, units, incidents and counters are reset, identifier counters are reset to 1.
     * 
     * @param width grid with (positive integer)
     * @param height grid height (positive integer)
     * @throws InvalidGridException if the width or height are not positive integers
     */
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

        nextStationId = 1;
        nextUnitId = 1;
        nextIncidentId = 1;
        tick = 0;
    }

    //getters for grid size, station/unit/incident ids, and status

    /**
     * Return the grid dimensions
     * @return an array containing {width, height}
     */
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

    /**
     * Returns the system status 
     * 
     * <p>The output includes:
     * <ul>
     *  <li>Current tick</li>
     *  <li>Counts of stations, units, incidents and obstacles</li>
     *  <li> All incidents in ascending ID order</li>
     *  <li>All units in ascending ID order</li>
     * </ul>
     * 
     * @return formatted system status string 
     */
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

    /**
    * Adds an obstacle to the grid at the given location.
    *
    * @param x x-coordinate
    * @param y y-coordinate
    * @throws InvalidLocationException if coordinates are out of bounds or the cell is not valid
    */
    @Override
    public void addObstacle(int x, int y) throws InvalidLocationException {
        if (!cityMap.inBounds(x, y)) {
            throw new InvalidLocationException("Obstacle location out of bounds");
        }
        // Deterministic + idempotent
        cityMap.addObstacle(x, y);
    }

    /**
    * Removes an obstacle from the grid at the given location.
    *
    * @param x x-coordinate
    * @param y y-coordinate
    * @throws InvalidLocationException if coordinates are out of bounds
    */
    @Override
    public void removeObstacle(int x, int y) throws InvalidLocationException {
        if (!cityMap.inBounds(x, y)) {
            throw new InvalidLocationException("Obstacle location out of bounds");
        }
        // Deterministic + idempotent
        cityMap.removeObstacle(x, y);       
    }



    //methods for stations, adding, removing and setting the capactiy  

    /**
    * Creates a new station at the specified location.
    *
    * @param name station name (must not be blank)
    * @param x x-coordinate
    * @param y y-coordinate
    * @return the newly assigned station ID
    * @throws InvalidNameException if name is null or blank
    * @throws InvalidLocationException if location is out of bounds or blocked
    * @throws IllegalStateException if maximum station limit is reached
    */
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


    /**
    * Cancels an incident.
    *
    * <p>If the incident is dispatched, the assigned unit is immediately released.
    *
    * @param incidentId incident identifier
    * @throws IDNotRecognisedException if ID is invalid
    * @throws IllegalStateException if incident state does not permit cancellation
    */
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

    /**
     * Sets the maximum capacity of a station.
     * 
     * <p>The new capacity must be positive and cannot be less than the number of units currently at the station.
     * @param stationId station identifier
     * @param maxUnits new maximum capacity
     * @throws IDNotRecognisedException if station ID is invalid
     * @throws InvalidCapacityException if the new capacity is invalid
     */
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

    /**
     * Adds a new unit to a station.
     * 
     * <p>The unit is created with the specified type and assigned to the station. 
     * <p>The unit's initial location is set to the station's location, and its status is set to IDLE.
     * 
     * @param stationId station identifier where the unit will be based
     * @param type type of unit to be added
     * @return the newly assigned unit ID
     * @throws IDNotRecognisedException if station ID is invalid
     * @throws InvalidUnitException if the unit type is null or unrecognised
     * @throws IllegalStateException if the station is at full capacity or if the maximum unit
     */

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
        switch (type) {
            case AMBULANCE -> u = new Ambulance(id, stationId, s.getX(), s.getY());
            case FIRE_ENGINE -> u = new FireEngine(id, stationId, s.getX(), s.getY());
            case POLICE_CAR -> u = new PoliceCar(id, stationId, s.getX(), s.getY());
            default -> //exception handling for an unrecognised unit type
                throw new InvalidUnitException("Unrecognised unit type");
        }

        //apply the new unit to the units array and add the unit to a station 
        units[unitCount++] = u;
        s.addUnitId(id);

        return id;
    }

    /**
     * Decommissions a unit, removing it from the system.
     * 
     * <p>The unit must not be currently en route or at the scene of an incident.
     * 
     * @param unitId unit identifier
     * @throws IDNotRecognisedException if unit ID is invalid
     * @throws IllegalStateException if the unit is currently en route or at the scene of an incident
     */
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

    /**
     * Transfers a unit to a different station.
     * 
     * <p>The unit must be IDLE to transfer, and the destination station must have available capacity.
     * 
     * @param unitId unit identifier
     * @param newStationId destination station identifier
     * @throws IDNotRecognisedException if the unit ID or station ID is invalid
     * @throws IllegalStateException if the unit is not IDLE or if the destination station has no free capacity
     */
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

    /**
     * Sets a unit's out of service status.
     * 
     * <p>When setting a unit out of service, it must be IDLE. When returning a unit to service, it must currently be OUT_OF_SERVICE.
     * 
     * @param unitId unit identifier
     * @param outOfService true to set the unit out of service, false to return it to service
     * @throws IDNotRecognisedException if the unit ID is invalid
     * @throws IllegalStateException if the unit's current status does not permit the requested change
     */
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

    /**
     * Returns a string representation of a unit's status.
     * 
     * <p>The output includes the unit's ID, type, home station ID, current location, status, and assigned incident ID (or "-" if none).
     * 
     * @param unitId unit identifier
     * @return formatted unit status string
     * @throws IDNotRecognisedException if the unit ID is invalid
     */
    @Override
    public String viewUnit(int unitId) throws IDNotRecognisedException {
        //exception handling for the unit id
        Unit u = getUnitById(unitId);
        if (u == null) {
            throw new IDNotRecognisedException("Unit ID not recognised");
        }
        return formatUnitLine(u);
    }


    //methods for incidents 


    /**
     * Reports a new incident at the specified location with the given type and severity.
     * 
     * <p>The incident is created with the specified type, severity and location, and its initial status is set to REPORTED.
     * 
     * @param type type of incident being reported
     * @param severity severity of the incident (1-5)
     * @param x x-coordinate of the incident location
     * @param y y-coordinate of the incident location
     * @return the newly assigned incident ID
     * @throws InvalidSeverityException if the severity is not between 1 and 5
     * @throws InvalidLocationException if the location is out of bounds or blocked
     * @throws IllegalStateException if the maximum incident limit is reached
     */
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

    /**
     * Cancels an incident.
     * 
     * <p>If the incident is dispatched, the assigned unit is immediately released.
     * 
     * @param incidentId incident identifier
     * @throws IDNotRecognisedException if ID is invalid
     * @throws IllegalStateException if incident state does not permit cancellation
     */
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

    /**
     * Escalates an incident by changing its severity.
     * 
     * <p>The incident's severity is updated to the new value.
     * 
     * @param incidentId incident identifier
     * @param newSeverity new severity level (1-5)
     * @throws IDNotRecognisedException if the incident ID is invalid
     * @throws InvalidSeverityException if the new severity is not between 1 and 5
     * @throws IllegalStateException if the incident is already resolved or cancelled
     */
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

    /**
     * Returns a string representation of an incident's status.
     * 
     * <p>The output includes the incident's ID, type, severity, location, status, and assigned unit ID (or "-" if none).
     * 
     * @param incidentId incident identifier
     * @return formatted incident status string
     * @throws IDNotRecognisedException if the incident ID is invalid
     */
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


    /**
     * Dispatches units to incidents based on the current system state.
     * 
     * <p>For each REPORTED incident, the system identifies the best available unit that can handle the incident type, using Manhattan distance for proximity and lowest unit ID for tie-breaking. 
     * <p>If a suitable unit is found, it is assigned to the incident, the incident status is updated to DISPATCHED, and the unit status is updated to EN_ROUTE.
     * 
     * <p>If no suitable unit is available, the incident remains in the REPORTED state.
     * 
     * @param incidentId incident identifier
     * @param newSeverity new severity level (1-5)
     * @throws IDNotRecognisedException if the incident ID is invalid
     * @throws InvalidSeverityException if the new severity is not between 1 and 5
     * @throws IllegalStateException if the incident is already resolved or cancelled
     */
    @Override
    public void dispatch() {
        
        //for each reported incident, find the best available unit and dispatch it
        int[] incIds = getIncidentIds();
        for (int i = 0; i < incIds.length; i++) {
            //get the incident object and check it is valid
            Incident inc = getIncidentById(incIds[i]);
            if (inc == null) continue;

            //only dispatch if the incident is currently reported
            if (inc.getStatus() != IncidentStatus.REPORTED) {
                continue;
            }

            //find the best available unit for the incident using the helper method and handle if there are none available
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

    /**
     * Advances the simulation by one tick, updating the state of units and incidents accordingly.
     * <p>During each tick:
     * <ul>
     *  <li>Units that are EN_ROUTE move one step towards their assigned incident location.</li>
     *  <li>Units that arrive at the incident location change status to AT_SCENE, and the incident status changes to IN_PROGRESS.</li>
     *  <li>Units that are AT_SCENE tick down their work time, and if they complete their work, the incident status changes to RESOLVED and the unit status changes to IDLE.</li>
     * </ul>
     */
    @Override
    public void tick() {
        tick++;

        //tick movement for the units that are on route 
        //get the unit ids
        int[] unitIds = getUnitIds();
        //for each unit that is on route
        for (int i = 0; i < unitIds.length; i++) {
            //get the unit object and check it is valid
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;
            //if the unit is on route, move it one step towards its assigned incident
            if (u.getStatus() == UnitStatus.EN_ROUTE) {
                //get the target incident object and check it is valid
                Incident target = getIncidentById(u.getAssignedIncidentId());
                if (target != null) {
                    //call move on step to move the unit one step towards the target incident
                    moveOneStep(u, target.getX(), target.getY());
                }
            }
        }

        //after movement, check if any units have arrived at an incident 
        for (int i = 0; i < unitIds.length; i++) {
            //get the unit object and check it is valid
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;

            //check if the unit is on route 
            if (u.getStatus() == UnitStatus.EN_ROUTE) {
                //get the target incident object and check it is valid 
                Incident target = getIncidentById(u.getAssignedIncidentId());
                //check if the unit has arrived at the incident location
                if (target != null && u.getX() == target.getX() && u.getY() == target.getY()) {
                    //apply the arrival by setting the status to at scene 
                    u.setStatus(UnitStatus.AT_SCENE);
                    //change the incidient status to in progress 
                    target.setStatus(IncidentStatus.IN_PROGRESS);
                    //call the start work mehtod to start the tick clock for the unit to work on the incident 
                    u.startWork();
                }
            }
        }

        //for each unit that is at the scene, check if they have completed their work
        for (int i = 0; i < unitIds.length; i++) {
            //get the unit object and check it is valid
            Unit u = getUnitById(unitIds[i]);
            if (u == null) continue;

            //if the unit is at the scene, call tickwork methiod to tick down the work clock and if completed allow the unit to move on to another task
            if (u.getStatus() == UnitStatus.AT_SCENE) {
                u.tickWork();
            }
        }

        
        //check if any incidents have been fully resolved, and if so, set the incident to resolved and idle the unit 
        //get the incident ids 
        int[] incIds = getIncidentIds();
        //loop through the incidents and if any are in progress
        for (int i = 0; i < incIds.length; i++) {
            //get the incident object and check it is valid
            Incident inc = getIncidentById(incIds[i]);
            if (inc == null) continue;

            //if the incident is in progress, check if the assigned unit has completes the work 
            if (inc.getStatus() == IncidentStatus.IN_PROGRESS) {
                //get the assigned unit object and check it is valid
                Unit u = getUnitById(inc.getAssignedUnitId());
                //if the unit is at the scene and has completed the work, set the incident to resolved and idle the unit
                if (u != null && u.getStatus() == UnitStatus.AT_SCENE && u.isWorkComplete()) {
                    //set incident status to resolved 
                    inc.setStatus(IncidentStatus.RESOLVED);
                    //set unit status to idle
                    u.setStatus(UnitStatus.IDLE);
                    //clear the incident 
                    u.clearIncident();
                    
                }
            }
        }
    }



    //extra methods created 
    
    /**
     * Helper method to choose the best unit for a given incident based on the criteria of being idle, able to handle the incident type, not already assigned, closest distance using manhatten and tie break rules for unit id and station id.
     * 
     * 
     * @param inc The incident for which to choose the best unit.
     * @return The best unit for the given incident, or null if no suitable unit is found.
     */
    private Unit chooseBestUnitFor(Incident inc) {

        //set best unit to null and best distance to an aribary large number
        Unit best = null;
        int bestDist = Integer.MAX_VALUE;

        //loop through each unit 
        for (int i = 0; i < unitCount; i++) {
            //create a temporary variable to store the current unit and check it is valid
            Unit u = units[i];
            if (u == null) continue;

            //check if the unit is idle, can handle the incident type and is not already assigned to an incident
            //if passes then continue
            if (u.getStatus() != UnitStatus.IDLE) continue;
            if (!u.canHandle(inc.getType())) continue;
            if (u.getAssignedIncidentId() != -1) continue;

            //get the distance of the unit to the indcident using the manhatten method 
            int dist = manhattan(u.getX(), u.getY(), inc.getX(), inc.getY());

            //if this is the first valid unit found, set it as the best by default 
            if (best == null) {
                best = u;
                bestDist = dist;
                continue;
            }

            //if the distance is less than the best distance, update this unit as the best
            if (dist < bestDist) {
                //set this unit as the best and update the best distance
                best = u;
                bestDist = dist;
            
            //check if the distance to the incident is the same, if so apply the tie break rule (choosing unit with the lowest id)
            } else if (dist == bestDist) {
                if (u.getUnitId() < best.getUnitId()) {
                    best = u;
                
                //if the unit id is th same, apply the second tie break rule for chosing the unit with the lowest home station id    
                } else if (u.getUnitId() == best.getUnitId()) {
                    if (u.getHomeStationId() < best.getHomeStationId()) {
                        best = u;
                    }
                }
            }
        }

        return best;
    }

    /**
     * Helper method to move a unit one step towards a target location using the manhatten distance to determine the best move, and if there are multiple moves that are the same distance, choose one that is legal. 
     * If there are no legal moves, the unit does not move.
     * 
     * 
     * @param u The unit to move.
     * @param targetX The target x-coordinate.
     * @param targetY The target y-coordinate.
     */
    private void moveOneStep(Unit u, int targetX, int targetY) {
        
        //local variables to store current x and y values of the unit 
        int x = u.getX();
        int y = u.getY();

        //make a list of possible moves for N, E, S, W
        int[][] moves = new int[][] {
            { x, y - 1 }, // N
            { x + 1, y }, // E
            { x, y + 1 }, // S
            { x - 1, y }  // W
        };

        //calculete the current distance using manhatten to compare with the possible moves 
        int currentDist = manhattan(x, y, targetX, targetY);

        //loop through possible moves
        for (int[] move : moves) {
            //get new x and y position from the possible move
            int nx = move[0];
            int ny = move[1];
            //check the movel is to a legal cell
            if (!cityMap.isLegalCell(nx, ny)) continue;
            //calcuate the new distance and check if it closer
            int nd = manhattan(nx, ny, targetX, targetY);
            //if it is closer, set as the new location of the unit 
            if (nd < currentDist) {
                u.setLocation(nx, ny);
                return;
            }
        }

        //loop for possible moves again to check if any are the same distance but still legal
        for (int[] move : moves) {
            int nx = move[0];
            int ny = move[1];
            if (!cityMap.isLegalCell(nx, ny)) continue;
            u.setLocation(nx, ny);
            return;
        }

        
    }

    /**
     * Helper method to calculate the manhattan distance between two points (x1, y1) and (x2, y2).
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @return The manhattan distance between the two points.
     */
    private static int manhattan(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        if (dx < 0) dx = -dx;
        int dy = y1 - y2;
        if (dy < 0) dy = -dy;
        return dx + dy;
    }

    /**
     * Helper method to format an incident line for the view incident method, including the incident's ID, type, severity, location, status, and assigned unit ID (or "-" if none).
     * @param inc The incident to format.
     */
    private String formatIncidentLine(Incident inc) {
        String unitStr = (inc.getAssignedUnitId() <= 0) ? "-" : String.valueOf(inc.getAssignedUnitId());
        return "I#" + inc.getIncidentId()
            + " TYPE=" + inc.getType()
            + " SEV=" + inc.getSeverity()
            + " LOC=(" + inc.getX() + "," + inc.getY() + ")"
            + " STATUS=" + inc.getStatus()
            + " UNIT=" + unitStr;
    }

    /**
     * Helper method to format a unit line for the view unit method, including the unit's ID, type, home station ID, current location, status, and assigned incident ID (or "-" if none), and if the unit is at scene, include the work ticks remaining.
     * @param u The unit to format.
     */
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

    //get station by id is not used throughout the code but useful for expanding the program 
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


}

