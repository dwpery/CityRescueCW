
public class Main {


    //enums 

    public enum IncidentType {
    MEDICAL,
    FIRE,
    CRIME
    }

    public enum UnitType {
    AMBULANCE,
    FIRE_ENGINE,
    POLICE_CAR
    }

    public enum IncidentStatus {
    REPORTED,
    DISPATCHED,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
    }

    public enum UnitStatus {
    IDLE,
    EN_ROUTE,
    AT_SCENE,
    OUT_OF_SERVICE
    }


    //classes for city rescue 

    public class CityMap{

        int width, height;
        boolean [][] blocked;

        //constructor
        public CityMap(int width, int height) {
            this.width = width;
            this.height = height;
            this.blocked = new boolean[width][height];
        }


        //methods 
        public boolean inBounds(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return false;
            }
            return true;
        }

        public boolean isBlocked(int x, int y) {
            if (!inBounds(x, y)) {
                return true; // out of bounds is considered blocked
            }
            return blocked[x][y];
        }

        public void setBlocked(int x, int y, boolean isBlocked) {
            if (inBounds(x, y)) {
                blocked[x][y] = isBlocked;
            }
        }   

        public boolean isLegalCell(int x, int y) {
            return inBounds(x, y) && !isBlocked(x, y);
        }

        boolean isLegalMove(int fromX, int fromY, int toX, int toY) {
            
            return true;
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
        public int getId(){
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
        public int getCapacity(){
            return capactity;
        }
        public int getUnitCount(){
            return unitCount;
        }


        //methods
        public boolean hasSpace() {
            return unitCount < capactity;
        }

        public void setCapactity (int newCap){

        }

        public void addUnitId (int unitID){

        }

        public int[] getUnitOdsAccending(){

            return null;
        }

    }



    public class Incident {
        int incidentId;
        IncidentType type;
        int severity;
        int x, y;
        IncidentStatus status;
        int assignedUnitId;
        int workRemaining;

        public Incident(int incidentId, IncidentType type, int severity, int x, int y) {
            this.incidentId = incidentId;
            this.type = type;
            this.severity = severity;
            this.x = x;
            this.y = y;
            this.status = IncidentStatus.REPORTED;
            this.assignedUnitId = -1; // no unit assigned initially
            this.workRemaining = 100; // arbitrary work units
        }

        //getters for all the attributes
        public int getId(){
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
        public int getWorkRemaining(){
            return workRemaining;
        }

        //methods
        public void assignUnit(int unitId){
            
        }

        public void startWork(){
            
        }

        public void decrementWork(){

        }

        public boolean isworkComplete(){
            return false;
        }

        public void resolve(){
            
        }

        public void cancel(){
            
        }

        public void setSeverity(int newSeverity){
            
        }



    }

    public abstract class Unit {

        int unitId;
        UnitType type;
        int homeStationId;
        int x, y;
        UnitStatus status;
        int assignedIncidentId;
        int targetX, targetY;
        int workRemaining;

        public Unit(int unitId, UnitType type, int homeStationId, int x, int y) {
            this.unitId = unitId;
            this.type = type;
            this.homeStationId = homeStationId;
            this.x = x;
            this.y = y;
            this.status = UnitStatus.IDLE;
            this.assignedIncidentId = -1; // no incident assigned
            this.targetX = -1;
            this.targetY = -1;
            this.workRemaining = 0;
        }

        //getters for all the attributes

        public int getId(){
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
        public void setOutOFService(){
            
        }

        public void dispatchToIncident(int incidentId, int targetX, int targetY){
            
        }

        public void stepTowardsTarget(CityMap map){
            
        }
        
        public boolean hasArrived(){
            return x == targetX && y == targetY;
        }

        public void arrivedOnScene(){
            
        }
        public void decrementWork(){
            
        }

        public boolean isWorkComplete(){
            return workRemaining <= 0;
        }

        public void clearAssignmentToIdle(){

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
