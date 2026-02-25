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

        int width, height;
        boolean [][] blocked;
        int obstacleCount;

        //constructor
        public CityMap(int width, int height) {
            this.width = width;
            this.height = height;
            this.blocked = new boolean[width][height];
        }

        //getters

        public int getWidth(){
            return width;
        }
        public int getHeight(){
            return height;
        }


        //methods 
        public boolean inBounds (int x, int y){
            return false;
        }

        public boolean isBlocked (int x, int y){
            return false;
        }


        public void addObstacle (int x, int y){

        }
        
        public void removeObstacle(int x, int y){

        }


        public boolean isLegalCell (int x, int y){
            return false;
        }
        
        public int getObstacleCount(){
            return obstacleCount;
        }


    }

    public class Station {

        int stationId;
        String name;
        int x, y;
        int capactity;
        int[] unitIds;
        int unitCount;

        //constructor
        public Station(int stationId, String name, int x, int y, int capacity) {
            this.stationId = stationId;
            this.name = name;
            this.x = x;
            this.y = y;
            this.capactity = capacity;
            this.unitIds = new int[capacity];
            this.unitCount = 0;
        }


        //getters and setters
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
            return capactity;
        }
        public int getUnitCount(){
            return unitCount;
        }


        //methods 

        public void setCapactity(){

        }

        public boolean hasSpace(){
            return false;
        }

        public void addUnitId(int unitId){

        }

        public void removeUnitId(int unitId){

        } 

        public boolean ownsUnit(int unitId){
            return false;
        }

    }



    public class Incident {
        int incidentId;
        IncidentType type;
        int severity;
        int x, y;
        IncidentStatus status;
        int assignedUnitId;
        

        public Incident(int incidentId, IncidentType type, int severity, int x, int y) {
            this.incidentId = incidentId;
            this.type = type;
            this.severity = severity;
            this.x = x;
            this.y = y;
            this.status = IncidentStatus.REPORTED;
            this.assignedUnitId = -1; // no unit assigned initially
            
        }

        //getters for all the attributes
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
        public void setSeverity(int severity){

        }

        public void setStatus(IncidentStatus status){

        }

        public void assignUnit (int unitId){

        }
        
        public void unassignUnit(){

        }


    }

    public abstract class Unit {

        int unitId;
        UnitType type;
        int homeStationId;
        int x, y;
        UnitStatus status;
        int assignedIncidentId;
        

        public Unit(int unitId, UnitType type, int homeStationId, int x, int y) {
            this.unitId = unitId;
            this.type = type;
            this.homeStationId = homeStationId;
            this.x = x;
            this.y = y;
            this.status = UnitStatus.IDLE;
            this.assignedIncidentId = -1; // no incident assigned
            
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

        //methods
        
        public void setHomeStationId (int stationId){

        }

        public void setLocation(int targetX, int targetY){

        }

        public void setStatus(UnitStatus status){

        }


        public void assignIncident(int incidentId){

        }
        public void clearIncident(){

        }

        public void startWork(){

        }

        public void tickWork(){

        }

        public boolean isWorkComplete(){
            return false;
        }

        //abstract methods

        public abstract boolean canHandle(Incident incident);

        public abstract int ticksAtScene();





    }

    public class Ambulance extends Unit{

        public Ambulance(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.AMBULANCE, homeStationId, x, y);
        }

        @Override
        public boolean canHandle(Incident incident){
            return incident.getType() == IncidentType.MEDICAL;
        }

        @Override
        public int ticksAtScene(){
            return 0; //change this
        }


    }

    public class FireEngine extends Unit {

        public FireEngine(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.FIRE_ENGINE, homeStationId, x, y);
        }

        @Override
        public boolean canHandle(Incident incident){
            return incident.getType() == IncidentType.FIRE;
        }

        @Override
        public int ticksAtScene(){
            return 0; //change this
        }

    }
    public class PoliceCar extends Unit {

        public PoliceCar(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.POLICE_CAR, homeStationId, x, y);
        }

        @Override
        public boolean canHandle(Incident incident){
            return incident.getType() == IncidentType.CRIME;
        }

        @Override
        public int ticksAtScene(){
            return 0; //change this
        }
    }
}
