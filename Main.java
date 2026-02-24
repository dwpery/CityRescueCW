
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

        public CityMap(int width, int height) {
            this.width = width;
            this.height = height;
            this.blocked = new boolean[width][height];
        }


    }

    public class Station {

        int stationId;
        String name;
        int x, y;
        int capactity;
        int[] unitIds;
        int unitCount;

        public Station(int stationId, String name, int x, int y, int capacity) {
            this.stationId = stationId;
            this.name = name;
            this.x = x;
            this.y = y;
            this.capactity = capacity;
            this.unitIds = new int[capacity];
            this.unitCount = 0;
        }

    }



    public class Incident {
        int incidentId;
        IncidentType type;
        int x, y;
        IncidentStatus status;
        int[] assignedUnitIds;
        int workRemaining;

        public Incident(int incidentId, IncidentType type, int x, int y) {
            this.incidentId = incidentId;
            this.type = type;
            this.x = x;
            this.y = y;
            this.status = IncidentStatus.REPORTED;
            this.assignedUnitIds = new int[10]; // max 10 units per incident
            this.workRemaining = 100; // arbitrary work units
        }

    }

    public class Unit {

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

    }

    public class Ambulance extends Unit{

        public Ambulance(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.AMBULANCE, homeStationId, x, y);
        }


    }

    public class FireEngine extends Unit {

        public FireEngine(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.FIRE_ENGINE, homeStationId, x, y);
        }

    }
    public class PoliceCar extends Unit {

        public PoliceCar(int unitId, int homeStationId, int x, int y) {
            super(unitId, UnitType.POLICE_CAR, homeStationId, x, y);
        }
    }


    public static void main(String[] args){
        
        

    }
    
}
