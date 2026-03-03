import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cityrescue.CityRescue;
import cityrescue.CityRescueImpl;
import cityrescue.enums.IncidentType;
import cityrescue.enums.UnitType;

public class PublicMoreLifecycleAndDispatchTest {

    private CityRescue cr;

    @BeforeEach
    void setUp() throws Exception {
        cr = new CityRescueImpl();
        cr.initialise(7, 7);
    }

    @Test
    void cancelReportedIncident_setsCancelled_andRemainsUnassigned() throws Exception {
        int i = cr.reportIncident(IncidentType.CRIME, 2, 3, 3);

        assertTrue(cr.viewIncident(i).contains("STATUS=REPORTED"));
        assertTrue(cr.viewIncident(i).contains("UNIT=-"));

        cr.cancelIncident(i);

        String after = cr.viewIncident(i);
        assertTrue(after.contains("STATUS=CANCELLED"));
        assertTrue(after.contains("UNIT=-"));
    }

    @Test
    void cancelDispatchedIncident_allowsSameUnitToBeDispatchedToAnotherIncident() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);

        int i1 = cr.reportIncident(IncidentType.CRIME, 2, 0, 5);
        int i2 = cr.reportIncident(IncidentType.CRIME, 2, 0, 6);

        cr.dispatch();
        assertTrue(cr.viewIncident(i1).contains("UNIT=" + u));

        // Cancel i1 while DISPATCHED; unit becomes IDLE
        cr.cancelIncident(i1);
        assertTrue(cr.viewUnit(u).contains("STATUS=IDLE"));
        assertTrue(cr.viewUnit(u).contains("INCIDENT=-"));

        // Now dispatch again; unit should be assigned to i2
        cr.dispatch();
        assertTrue(cr.viewIncident(i2).contains("UNIT=" + u));
    }

    @Test
    void escalateWhileDispatched_isAllowed_andDoesNotUnassignUnit() throws Exception {
        int s = cr.addStation("A", 1, 1);
        int u = cr.addUnit(s, UnitType.FIRE_ENGINE);
        int i = cr.reportIncident(IncidentType.FIRE, 1, 4, 4);

        cr.dispatch();

        String before = cr.viewIncident(i);
        assertTrue(before.contains("STATUS=DISPATCHED"));
        assertTrue(before.contains("UNIT=" + u));

        cr.escalateIncident(i, 5);

        String after = cr.viewIncident(i);
        assertTrue(after.contains("SEV=5"));
        assertTrue(after.contains("STATUS=DISPATCHED"));
        assertTrue(after.contains("UNIT=" + u));
    }

    @Test
    void dispatch_skipsNonReportedIncidents() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u1 = cr.addUnit(s, UnitType.AMBULANCE);
        int u2 = cr.addUnit(s, UnitType.AMBULANCE);

        int i1 = cr.reportIncident(IncidentType.MEDICAL, 2, 0, 1);
        int i2 = cr.reportIncident(IncidentType.MEDICAL, 2, 0, 2);

        cr.dispatch(); // assigns u1->i1, u2->i2 (likely)

        // Cancel i1 (becomes CANCELLED)
        cr.cancelIncident(i1);

        // Create a new incident, then dispatch again. Should not re-dispatch i1.
        int i3 = cr.reportIncident(IncidentType.MEDICAL, 2, 0, 3);
        cr.dispatch();

        assertTrue(cr.viewIncident(i1).contains("STATUS=CANCELLED"));
        assertFalse(cr.viewIncident(i1).contains("STATUS=DISPATCHED"));

        // i3 should get a unit if one is available
        String i3View = cr.viewIncident(i3);
        assertFalse(i3View.contains("UNIT=-"));
    }

    @Test
    void dispatch_respectsTypeCompatibility() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int police = cr.addUnit(s, UnitType.POLICE_CAR);

        int fire = cr.reportIncident(IncidentType.FIRE, 2, 3, 3);

        cr.dispatch();

        // Police can't handle FIRE, so must remain unassigned
        assertTrue(cr.viewIncident(fire).contains("UNIT=-"));
        assertTrue(cr.viewUnit(police).contains("STATUS=IDLE"));
    }

    @Test
    void output_hasBasicTokens_forStatusUnitIncident() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);
        int i = cr.reportIncident(IncidentType.CRIME, 2, 1, 1);

        String status = cr.getStatus();
        assertTrue(status.contains("TICK="));
        assertTrue(status.contains("STATIONS="));
        assertTrue(status.contains("UNITS="));
        assertTrue(status.contains("INCIDENTS="));
        assertTrue(status.contains("OBSTACLES="));

        String uView = cr.viewUnit(u);
        assertTrue(uView.contains("U#"));
        assertTrue(uView.contains("TYPE="));
        assertTrue(uView.contains("HOME="));
        assertTrue(uView.contains("LOC=("));
        assertTrue(uView.contains("STATUS="));
        assertTrue(uView.contains("INCIDENT="));

        String iView = cr.viewIncident(i);
        assertTrue(iView.contains("I#"));
        assertTrue(iView.contains("TYPE="));
        assertTrue(iView.contains("SEV="));
        assertTrue(iView.contains("LOC=("));
        assertTrue(iView.contains("STATUS="));
        assertTrue(iView.contains("UNIT="));
    }
}