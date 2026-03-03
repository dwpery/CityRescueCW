package cityrescue;

import cityrescue.CityRescue.Unit;
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
 * Core API for the CityRescue simulation.
 * <p>
 * Implementations manage:
 * <ul>
 *   <li>a 2D grid (the city map) with obstacles</li>
 *   <li>stations (named locations with a maximum unit capacity)</li>
 *   <li>units (e.g. ambulance / police / fire) belonging to stations</li>
 *   <li>incidents (reported at locations with type and severity)</li>
 *   <li>time progression via {@link #tick()} and automated allocation via {@link #dispatch()}</li>
 * </ul>
 * </p>
 * <p>
 * This interface also contains supporting model types as nested classes (map, station, incident, units).
 * </p>
 */
public interface CityRescue {
    void initialise(int width, int height) throws InvalidGridException;
    int[] getGridSize();

    void addObstacle(int x, int y) throws InvalidLocationException;
    void removeObstacle(int x, int y) throws InvalidLocationException;

    int addStation(String name, int x, int y) throws InvalidNameException, InvalidLocationException;
    void removeStation(int stationId) throws IDNotRecognisedException, IllegalStateException;
    void setStationCapacity(int stationId, int maxUnits) throws IDNotRecognisedException, InvalidCapacityException;
    int[] getStationIds();

    int addUnit(int stationId, UnitType type) throws IDNotRecognisedException, InvalidUnitException, IllegalStateException;
    void decommissionUnit(int unitId) throws IDNotRecognisedException, IllegalStateException;
    void transferUnit(int unitId, int newStationId) throws IDNotRecognisedException, IllegalStateException;
    void setUnitOutOfService(int unitId, boolean outOfService) throws IDNotRecognisedException, IllegalStateException;
    int[] getUnitIds();
    String viewUnit(int unitId) throws IDNotRecognisedException;

    int reportIncident(IncidentType type, int severity, int x, int y) throws InvalidSeverityException, InvalidLocationException;
    void cancelIncident(int incidentId) throws IDNotRecognisedException, IllegalStateException;
    void escalateIncident(int incidentId, int newSeverity) throws IDNotRecognisedException, InvalidSeverityException, IllegalStateException;
    int[] getIncidentIds();
    String viewIncident(int incidentId) throws IDNotRecognisedException;

    void dispatch();
    void tick();
    String getStatus();

    /**
     * A simple in-memory representation of the city grid.
     * <p>
     * The map tracks its dimensions and which cells are blocked by obstacles.
     * Coordinates are assumed to be zero-based: {@code 0 <= x < width}, {@code 0 <= y < height}.
     * </p>
     * <p>
     * This class does not perform path-finding; it only exposes bounds/blocking queries and obstacle mutation.
     * </p>
     */
    public class CityMap{

        private final int width, height;
        private final boolean [][] blocked;
        private int obstacleCount;

        /**
         * Creates a new city map with the given dimensions.
         *
         * @param width grid width (must be &gt; 0)
         * @param height grid height (must be &gt; 0)
         * @throws IllegalArgumentException if width or height are not positive
         */
        public CityMap(int width, int height) {

            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Width and height must be > 0");
            }

        
            this.width = width;
            this.height = height;
            this.blocked = new boolean[width][height];
            this.obstacleCount = 0;
        }

        //getters

        //size of the map
        public int getWidth(){
            return width;
        }
        public int getHeight(){
            return height;
        }

        //get the number of obstacles
        public int getObstacleCount(){
            return obstacleCount;
        }


        //methods

        /**
         * Checks whether the supplied coordinates are within the map bounds.
         *
         * @param x x-coordinate (0-based)
         * @param y y-coordinate (0-based)
         * @return true if (x,y) is inside the grid; otherwise false
         */
        public boolean inBounds (int x, int y){
            return x >= 0 && x < width && y >= 0 && y < height;
        }

        /**
         * Checks whether a cell is currently blocked by an obstacle.
         * <p>
         * If the coordinates are out of bounds this returns {@code false}.
         * </p>
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @return true if the cell is blocked; false otherwise (including out of bounds)
         */
        public boolean isBlocked (int x, int y){
            if (!inBounds(x, y)){
                return false;
            }
            return blocked[x][y];
        }

        /**
         * Convenience check for a cell that is both in bounds and not blocked.
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @return true if the cell can be occupied; false otherwise
         */
        public boolean isLegalCell (int x, int y){
            return inBounds(x, y) && !blocked[x][y];
        }


        /**
         * Adds an obstacle at the given location if not already present.
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @throws IllegalArgumentException if the location is out of bounds
         */
        public void addObstacle (int x, int y){
            if(!inBounds(x,y)){
                throw new IllegalArgumentException("Out of bounds");
            }
            if(!blocked[x][y]){
                blocked[x][y] = true;
                obstacleCount++;
            }
        }

        /**
         * Removes an obstacle at the given location if present.
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @throws IllegalArgumentException if the location is out of bounds
         */
        public void removeObstacle(int x, int y){
            if (!inBounds(x, y)){
                throw new IllegalArgumentException("Out of bounds");
            }
            if(blocked[x][y]){
                blocked[x][y] = false;
                obstacleCount--;
            }

        }
    }

    /**
     * Represents a response station (e.g. fire station, police station, ambulance depot).
     * <p>
     * A station has a fixed identity and location, plus a configurable maximum capacity.
     * It tracks the unit IDs currently attached to it.
     * </p>
     * <p>
     * This class only manages IDs; it does not own the unit objects themselves.
     * </p>
     */
    public class Station {

        private final int stationId;
        private final String name;
        private final int x, y;

        private int capacity;
        private int[] unitIds;
        private int unitCount;

        /**
         * Creates a station.
         *
         * @param stationId unique station identifier (must be &gt; 0)
         * @param name non-blank station name
         * @param x x-coordinate of the station location
         * @param y y-coordinate of the station location
         * @param capacity maximum number of units this station can hold (must be &gt; 0)
         * @throws IllegalArgumentException if stationId/capacity are not positive, or name is blank
         */
        public Station(int stationId, String name, int x, int y, int capacity) {

            //exception handling
            if (stationId <= 0) {
                throw new IllegalArgumentException("Station id must be > 0");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Station name must not be blank");
            }
            if (capacity <= 0) {
                throw new IllegalArgumentException("Station capacity must be > 0");
            }

            this.stationId = stationId;
            this.name = name;
            this.x = x;
            this.y = y;

            this.capacity = capacity;
            this.unitIds = new int[capacity];
            this.unitCount = 0;
        }


        //getters for the class
        public int getStationId(){
            return stationId;
        }
        public String getName(){
            return name;
        }
        public int getX(){
            return x;
        }
        public int getY(){
            return y;
        }
        public int[] getUnitIds(){
            return unitIds;
        }
        public int getCapacity(){
            return capacity;
        }
        public int getUnitCount(){
            return unitCount;
        }


        //methods for the class

        /**
         * Changes the station capacity and resizes the internal unit-id list.
         *
         * @param newCapacity new maximum number of units (must be &gt; 0 and &gt;= current unit count)
         * @throws IllegalArgumentException if newCapacity is invalid
         */
        public void setCapacity(int newCapacity){

            //exception handling
            if (newCapacity <= 0) {
                throw new IllegalArgumentException("Capacity must be > 0");
            }
            if (newCapacity < unitCount) {
                throw new IllegalArgumentException("Capacity cannot be less than current unit count");
            }
            if (newCapacity == this.capacity) {
                return;
            }

            //using a temporary array to store the contents of unitID and updating the size
            int[] resized = new int[newCapacity];
            for (int i = 0; i < unitCount; i++) {
                resized[i] = unitIds[i];
            }

            //putting the contents back in the array
            this.unitIds = resized;
            this.capacity = newCapacity;

        }

        /**
         * @return true if this station can accept at least one more unit; false otherwise
         */
        public boolean hasSpace(){
            return unitCount < capacity;
        }

        /**
         * Adds a unit ID to this station (if not already present).
         *
         * @param unitId unit identifier (must be &gt; 0)
         * @throws IllegalArgumentException if unitId is not positive
         * @throws IllegalStateException if station is already at capacity
         */
        public void addUnitId(int unitId){

            //exception handling
            if (unitId <= 0) {
            throw new IllegalArgumentException("Unit id must be > 0");
            }
            if (!hasSpace()) {
            throw new IllegalStateException("Station capacity exceeded");
            }

            //just in case a unit is accidentally added again
            if (ownsUnit(unitId)) {
            return;
            }

            //adding the unit
            unitIds[unitCount] = unitId;
            unitCount++;

        }

        /**
         * Removes a unit ID from this station if present.
         * <p>
         * Keeps the internal array compact by shifting elements left.
         * </p>
         *
         * @param unitId unit identifier to remove
         */
        public void removeUnitId(int unitId){

            int idx = indexOfUnit(unitId);
            if (idx == -1) {
            return;
            }

            //shift array left to keep array tidy
            for (int i = idx; i < unitCount - 1; i++) {
            unitIds[i] = unitIds[i + 1];
            }
            unitCount--;
        }

        /**
         * Checks whether this station currently lists the supplied unit id.
         *
         * @param unitId unit identifier
         * @return true if the unit id is stored for this station; false otherwise
         */
        public boolean ownsUnit(int unitId){
            return indexOfUnit(unitId) != -1;
        }

        //helper method for index of the units
        private int indexOfUnit(int unitId) {
            for (int i = 0; i < unitCount; i++) {
                if (unitIds[i] == unitId) {
                    return i;
                }
            }
            return -1;
        }

    }


    /**
     * Represents an incident reported in the city.
     * <p>
     * Each incident has a type, severity (1..5), location, and lifecycle status.
     * A single unit ID may be assigned to the incident at a time.
     * </p>
     */
    public class Incident {

        //attributes
        private final int incidentId;
        private final IncidentType type;
        private int severity;
        private final int x, y;
        private IncidentStatus status;
        private int assignedUnitId;


        /**
         * Creates a new incident in {@link IncidentStatus#REPORTED} state with no assigned unit.
         *
         * @param incidentId unique incident identifier (must be &gt; 0)
         * @param type incident type (must not be null)
         * @param severity incident severity (must be in range 1..5)
         * @param x x-coordinate where the incident occurred
         * @param y y-coordinate where the incident occurred
         * @throws IllegalArgumentException if incidentId is invalid, type is null, or severity is out of range
         */
        public Incident(int incidentId, IncidentType type, int severity, int x, int y) {

            //exception handling
            if (incidentId <= 0) {
                throw new IllegalArgumentException("Incident id must be > 0");
            }
            if (type == null) {
                throw new IllegalArgumentException("Incident type must not be null");
            }
            validateSeverity(severity);

            this.incidentId = incidentId;
            this.type = type;
            this.severity = severity;
            this.x = x;
            this.y = y;
            this.status = IncidentStatus.REPORTED;
            this.assignedUnitId = -1; // no unit assigned initially

        }


        //getters for the class 
        public int getIncidentId(){
            return incidentId;
        }
        public int getSeverity(){
            return severity;
        }
        public IncidentType getType(){
            return type;
        }
        public IncidentStatus getStatus(){
            return status;
        }
        public int getX(){
            return x;
        }
        public int getY(){
            return y;
        }
        public int getAssignedUnitId(){
            return assignedUnitId;
        }


        //methods

        /**
         * Updates incident severity.
         *
         * @param newSeverity new severity (1..5)
         * @throws IllegalArgumentException if newSeverity is out of range
         */
        public void setSeverity(int newSeverity){

            validateSeverity(newSeverity);
            this.severity = newSeverity;

        }

        /**
         * Updates the incident status.
         *
         * @param newStatus new status (must not be null)
         * @throws IllegalArgumentException if newStatus is null
         */
        public void setStatus(IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Incident status must not be null");
        }
        this.status = newStatus;
        }

        /**
         * Assigns a unit to this incident.
         *
         * @param unitId unit identifier (must be &gt; 0)
         * @throws IllegalArgumentException if unitId is not positive
         */
        public void assignUnit (int unitId){
            if(unitId <= 0){
                throw new IllegalArgumentException("Unit id must be > 0");
            }
            this.assignedUnitId = unitId;
        }

        /**
         * Clears the assigned unit (sets assigned unit id to -1).
         */
        public void unassignUnit(){
             this.assignedUnitId = -1;
        }


        //helper method

        /**
         * Validates a severity value.
         *
         * @param sev severity to check
         * @throws IllegalArgumentException if sev is not in range 1..5
         */
        private static void validateSeverity(int sev) {
        if (sev < 1 || sev > 5) {
            throw new IllegalArgumentException("Severity must be in range 1..5");
        }
        }
    }

    /**
     * Base class for all response units (ambulance, police car, fire engine, etc.).
     * <p>
     * A unit has:
     * <ul>
     *   <li>an immutable id and type</li>
     *   <li>a home station id</li>
     *   <li>a current location</li>
     *   <li>a status and (optionally) an assigned incident</li>
     *   <li>a simple "work" timer once on scene</li>
     * </ul>
     * </p>
     * <p>
     * Subclasses define which incident types they can handle and how long they take at the scene.
     * </p>
     */
    public abstract class Unit {

        //attributes
        private final int unitId;
        private final UnitType type;

        private int homeStationId;

        private int x, y;

        private UnitStatus status;

        private int assignedIncidentId;
        private int workTicksRemaining;


        /**
         * Creates a unit.
         *
         * @param unitId unique unit identifier
         * @param type unit type
         * @param homeStationId station id the unit belongs to
         * @param x initial x-coordinate
         * @param y initial y-coordinate
         */
        public Unit(int unitId, UnitType type, int homeStationId, int x, int y) {
            this.unitId = unitId;
            this.type = type;
            this.homeStationId = homeStationId;
            this.x = x;
            this.y = y;
            this.status = UnitStatus.IDLE;
            this.assignedIncidentId = -1; // no incident assigned
            this.workTicksRemaining = 0;

        }

        //getters for the class

        public int getUnitId(){
            return unitId;
        }
        public UnitType getType(){
            return type;
        }
        public int getHomeStationId(){
            return homeStationId;
        }
        public int getX(){
            return x;
        }
        public int getY(){
            return y;
        }
        public UnitStatus getStatus(){
            return status;
        }
        public int getAssignedIncidentId(){
            return assignedIncidentId;
        }

        /**
         * @return true if this unit is currently idle (dispatchable); false otherwise
         */
        public boolean isAvailableForDispatch(){
            return status == UnitStatus.IDLE;
        }
        public int getWorkTicksRemaining(){
            return workTicksRemaining;
        }

        //methods

        /**
         * Sets the unit's home station id.
         *
         * @param stationId station identifier (must be &gt; 0)
         * @throws IllegalArgumentException if stationId is not positive
         */
        public void setHomeStationId (int stationId){
            if (stationId <= 0) {
            throw new IllegalArgumentException("Home station id must be > 0");
            }
            this.homeStationId = stationId;
        }

        /**
         * Updates the unit's current location.
         *
         * @param x new x-coordinate
         * @param y new y-coordinate
         */
        public void setLocation(int x, int y){
            this.x = x;
            this.y = y;
        }

        /**
         * Updates the unit status.
         *
         * @param newStatus new status (must not be null)
         * @throws IllegalArgumentException if newStatus is null
         */
        public void setStatus(UnitStatus newStatus){
            if (newStatus == null) {
            throw new IllegalArgumentException("Unit status must not be null");
            }
            this.status = newStatus;
        }

        /**
         * Assigns an incident to this unit and resets any running work timer.
         *
         * @param incidentId incident identifier (must be &gt; 0)
         * @throws IllegalArgumentException if incidentId is not positive
         */
        public void assignIncident(int incidentId){
            if (incidentId <= 0) {
            throw new IllegalArgumentException("Incident id must be > 0");
            }
            this.assignedIncidentId = incidentId;
            // When assigned, work should not be running yet
            this.workTicksRemaining = 0;
        }

        /**
         * Clears the incident assignment and work timer.
         */
        public void clearIncident(){
            this.assignedIncidentId = -1;
            this.workTicksRemaining = 0;
        }

        /**
         * Starts work on scene by setting {@code workTicksRemaining} to {@link #ticksAtScene()}.
         */
        public void startWork(){
            this.workTicksRemaining = ticksAtScene();
        }

        /**
         * Advances work by one tick (decrements {@code workTicksRemaining} if above zero).
         */
        public void tickWork(){
            if (workTicksRemaining > 0) {
            workTicksRemaining--;
            }
        }

        /**
         * @return true if the unit has completed its on-scene work; false otherwise
         */
        public boolean isWorkComplete(){
            return workTicksRemaining <= 0;
        }

        //polymorphism

        /**
         * Indicates whether this unit can respond to (and resolve) incidents of the given type.
         *
         * @param incidentType incident type to check
         * @return true if this unit can handle the incident type; false otherwise
         */
        public abstract boolean canHandle(IncidentType incidentType);

        /**
         * Defines how many ticks this unit takes to resolve an incident once on scene.
         *
         * @return number of ticks required at the scene
         */
        public abstract int ticksAtScene();

    }

    /**
     * A medical response unit.
     * <p>
     * Can handle {@link IncidentType#MEDICAL} incidents and takes 2 ticks at the scene.
     * </p>
     */
    public final class Ambulance extends Unit{

        /**
         * Creates an ambulance unit at a starting location.
         *
         * @param unitId unique unit identifier
         * @param homeStationId owning station id
         * @param startX starting x-coordinate
         * @param startY starting y-coordinate
         */
        public Ambulance(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.AMBULANCE, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType incidentType){
            return incidentType == IncidentType.MEDICAL;
        }

        @Override
        public int ticksAtScene(){
            return 2; //as specified 
        }

    }

    /**
     * A law-enforcement response unit.
     * <p>
     * Can handle {@link IncidentType#CRIME} incidents and takes 3 ticks at the scene.
     * </p>
     */
    public final class PoliceCar extends Unit {

        /**
         * Creates a police car unit at a starting location.
         *
         * @param unitId unique unit identifier
         * @param homeStationId owning station id
         * @param startX starting x-coordinate
         * @param startY starting y-coordinate
         */
        public PoliceCar(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.POLICE_CAR, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType type) {
            return type == IncidentType.CRIME;
        }

        @Override
        public int ticksAtScene() {
            return 3; //as specified
        }
    }

    /**
     * A fire response unit.
     * <p>
     * Can handle {@link IncidentType#FIRE} incidents and takes 4 ticks at the scene.
     * </p>
     */
    public final class FireEngine extends Unit {

        /**
         * Creates a fire engine unit at a starting location.
         *
         * @param unitId unique unit identifier
         * @param homeStationId owning station id
         * @param startX starting x-coordinate
         * @param startY starting y-coordinate
         */
        public FireEngine(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.FIRE_ENGINE, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType type) {
            return type == IncidentType.FIRE;
        }

        @Override
        public int ticksAtScene() {
            return 4; //as specified 
        }
    }

}