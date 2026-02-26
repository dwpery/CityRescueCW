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

    


    //classes for city rescue 

    public class CityMap{

        private final int width, height;
        private final boolean [][] blocked;
        private int obstacleCount;

        //constructor
        public CityMap(int width, int height) {
            
            //if (width <= 0 || height <= 0) {
            //throw new InvalidGridException("Invalid grid size: " + width + "x" + height);
            //}
            
            //for some reason this doesnt work so just gonna leave like this for now 

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

        //checking biunds
        public boolean inBounds (int x, int y){
            return x >= 0 && x < width && y >= 0 && y < height;
        }

        //checking whether a cell is blocked 
        public boolean isBlocked (int x, int y){
            if (!inBounds(x, y)){
                return false;
            }
            return blocked[x][y];
        }

        //if the location is in bounds and not blocked 
        public boolean isLegalCell (int x, int y){
            return inBounds(x, y) && !blocked[x][y];
        }


        //for adding and removing obstacles from the map (the remove might not be needed but good to have regardless)

        public void addObstacle (int x, int y){
            if(!inBounds(x,y)){
                //throw new InvaildLocationException("Out of bounds");

                //for some reason doesn't like being done locally but in github is fine?????
            }
            if(!blocked[x][y]){
                blocked[x][y] = true;
                obstacleCount++;
            }
        }
        
        public void removeObstacle(int x, int y){
            if (!inBounds(x, y)){
                //throw new InvaildLocationException("Out of bounds");
                //again dont work here 
            }
            if(blocked[x][y]){
                blocked[x][y] = false;
                obstacleCount--;
            }

        }



    }

    public class Station {

        private final int stationId;
        private final String name;
        private final int x, y;

        private int capacity;
        private int[] unitIds;
        private int unitCount;

        //constructor
        public Station(int stationId, String name, int x, int y, int capacity) {

            //exception handling that dont work still
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


        //getters 
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


        //methods 

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

        public boolean hasSpace(){
            return unitCount < capacity;
        }

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

        //method for remoding a unit 
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

        //helper method for index of the units 
        private int indexOfUnit(int unitId) {
        for (int i = 0; i < unitCount; i++) {
            if (unitIds[i] == unitId) {
                return i;
            }
        }
        return -1;
    }

        public boolean ownsUnit(int unitId){
            return indexOfUnit(unitId) != -1;
        }

    }



    public class Incident {
        private final int incidentId;
        private final IncidentType type;
        private int severity;
        private final int x, y;
        private IncidentStatus status;
        private int assignedUnitId;
        

        public Incident(int incidentId, IncidentType type, int severity, int x, int y) {
            
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

        //getters
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

        //for setting the severity of an incident 
        public void setSeverity(int newSeverity){

            validateSeverity(newSeverity);
            this.severity = newSeverity;
            
        }

        public void setStatus(IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Incident status must not be null");
        }
        this.status = newStatus;
        }

        public void assignUnit (int unitId){
            if(unitId <= 0){
                throw new IllegalArgumentException("Unit id must be > 0");
            }
            this.assignedUnitId = unitId;
        }
        
        public void unassignUnit(){
             this.assignedUnitId = -1;
        }


        //helpers

        private static void validateSeverity(int sev) {
        if (sev < 1 || sev > 5) {
            throw new IllegalArgumentException("Severity must be in range 1..5");
        }
        }
    }

    public abstract class Unit {

        private final int unitId;
        private final UnitType type;

        private int homeStationId;

        private int x, y;

        private UnitStatus status;
        
        private int assignedIncidentId;
        private int workTicksRemaining;
        

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

        //getters for all the attributes

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
        public boolean isAvailableForDispatch(){
            return status == UnitStatus.IDLE;
        }
        public int getWorkTicksRemaining(){
            return workTicksRemaining;
        }

        //methods
        
        //methods for setting up
        public void setHomeStationId (int stationId){
            if (stationId <= 0) {
            throw new IllegalArgumentException("Home station id must be > 0");
            }
            this.homeStationId = stationId;
        }

        public void setLocation(int x, int y){
            this.x = x;
            this.y = y;
        }

        public void setStatus(UnitStatus newStatus){
            if (newStatus == null) {
            throw new IllegalArgumentException("Unit status must not be null");
            }
            this.status = newStatus;
        }


        public void assignIncident(int incidentId){
            if (incidentId <= 0) {
            throw new IllegalArgumentException("Incident id must be > 0");
            }
            this.assignedIncidentId = incidentId;
            // When assigned, work should not be running yet
            this.workTicksRemaining = 0;
        }
        public void clearIncident(){
            this.assignedIncidentId = -1;
            this.workTicksRemaining = 0;
        }

        //methods for when on the scene

        public void startWork(){
            this.workTicksRemaining = ticksAtScene();
        }

        public void tickWork(){
            if (workTicksRemaining > 0) {
            workTicksRemaining--;
            }
        }

        public boolean isWorkComplete(){
            return workTicksRemaining <= 0;
        }

        //polymorphism hooks 

        public abstract boolean canHandle(IncidentType incidentType);

        public abstract int ticksAtScene();





    }

    public final class Ambulance extends Unit{

        public Ambulance(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.AMBULANCE, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType incidentType){
            return incidentType == IncidentType.MEDICAL;
        }

        @Override
        public int ticksAtScene(){
            return 2;
        }


    }

    public final class PoliceCar extends Unit {

        public PoliceCar(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.POLICE_CAR, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType type) {
            return type == IncidentType.CRIME;
        }

        @Override
        public int ticksAtScene() {
            return 3; 
        }
    }

    public final class FireEngine extends Unit {

        public FireEngine(int unitId, int homeStationId, int startX, int startY) {
            super(unitId, UnitType.FIRE_ENGINE, homeStationId, startX, startY);
        }

        @Override
        public boolean canHandle(IncidentType type) {
            return type == IncidentType.FIRE;
        }

        @Override
        public int ticksAtScene() {
            return 4; //check this is the right tick ect
        }
    }

    
    
}
