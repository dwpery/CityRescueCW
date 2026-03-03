import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cityrescue.CityRescue;
import cityrescue.CityRescueImpl;
import cityrescue.enums.IncidentType;
import cityrescue.enums.UnitType;
import cityrescue.exceptions.IDNotRecognisedException;
import cityrescue.exceptions.InvalidCapacityException;
import cityrescue.exceptions.InvalidGridException;
import cityrescue.exceptions.InvalidLocationException;
import cityrescue.exceptions.InvalidNameException;
import cityrescue.exceptions.InvalidSeverityException;

public class PublicEdgeCasesTest {

    private CityRescue cr;

    @BeforeEach
    void setUp() throws Exception {
        cr = new CityRescueImpl();
        cr.initialise(5, 5);
    }

    // -------------------------
    // initialise + obstacles
    // -------------------------

    @Test
    void initialise_invalidDimensions_throws() {
        assertThrows(InvalidGridException.class, () -> cr.initialise(0, 5));
        assertThrows(InvalidGridException.class, () -> cr.initialise(5, 0));
        assertThrows(InvalidGridException.class, () -> cr.initialise(-1, 5));
    }

    @Test
    void removeObstacle_outOfBounds_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(InvalidLocationException.class, () -> cr.removeObstacle(99, 99));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void addObstacle_twice_isIdempotent_obstacleCountDoesNotDouble() throws Exception {
        cr.addObstacle(2, 2);
        String s1 = cr.getStatus();
        cr.addObstacle(2, 2);
        String s2 = cr.getStatus();
        assertEquals(s1, s2);
    }

    @Test
    void removeObstacle_twice_isIdempotent_obstacleCountDoesNotGoNegative() throws Exception {
        cr.addObstacle(1, 1);
        cr.removeObstacle(1, 1);
        String s1 = cr.getStatus();
        cr.removeObstacle(1, 1);
        String s2 = cr.getStatus();
        assertEquals(s1, s2);
        assertTrue(s2.contains("OBSTACLES=0"));
    }

    // -------------------------
    // stations
    // -------------------------

    @Test
    void addStation_blankName_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(InvalidNameException.class, () -> cr.addStation("   ", 1, 1));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void addStation_onBlockedCell_throws_andStateUnchanged() throws Exception {
        cr.addObstacle(1, 1);
        String before = cr.getStatus();
        assertThrows(InvalidLocationException.class, () -> cr.addStation("A", 1, 1));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void removeStation_withUnits_throws_andStateUnchanged() throws Exception {
        int s = cr.addStation("A", 0, 0);
        cr.addUnit(s, UnitType.AMBULANCE);

        String before = cr.getStatus();
        assertThrows(IllegalStateException.class, () -> cr.removeStation(s));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void setStationCapacity_smallerThanUnitCount_throws_andStateUnchanged() throws Exception {
        int s = cr.addStation("A", 0, 0);
        cr.addUnit(s, UnitType.AMBULANCE);
        cr.addUnit(s, UnitType.POLICE_CAR);

        String before = cr.getStatus();
        assertThrows(InvalidCapacityException.class, () -> cr.setStationCapacity(s, 1));
        assertEquals(before, cr.getStatus());
    }

    // -------------------------
    // units
    // -------------------------

    @Test
    void addUnit_unknownStation_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(IDNotRecognisedException.class, () -> cr.addUnit(999, UnitType.AMBULANCE));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void setUnitOutOfService_onlyAllowedWhenIdle() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.AMBULANCE);
        int i = cr.reportIncident(IncidentType.MEDICAL, 1, 0, 1);
        cr.dispatch();

        // Now EN_ROUTE, cannot be set out of service
        String before = cr.getStatus();
        assertThrows(IllegalStateException.class, () -> cr.setUnitOutOfService(u, true));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void outOfServiceUnit_isNotEligibleForDispatch() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);

        cr.setUnitOutOfService(u, true);

        int i = cr.reportIncident(IncidentType.CRIME, 3, 2, 2);
        cr.dispatch();

        assertTrue(cr.viewIncident(i).contains("UNIT=-"));
        assertTrue(cr.viewUnit(u).contains("STATUS=OUT_OF_SERVICE"));
    }

    @Test
    void decommissionUnit_busy_enRoute_orAtScene_throws_andStateUnchanged() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.AMBULANCE);
        int i = cr.reportIncident(IncidentType.MEDICAL, 1, 0, 2);

        cr.dispatch(); // u EN_ROUTE

        String before = cr.getStatus();
        assertThrows(IllegalStateException.class, () -> cr.decommissionUnit(u));
        assertEquals(before, cr.getStatus());

        // Move to arrive to scene
        cr.tick();
        cr.tick(); // should now be AT_SCENE (or earlier depending on route)
        if (cr.viewUnit(u).contains("STATUS=AT_SCENE")) {
            String before2 = cr.getStatus();
            assertThrows(IllegalStateException.class, () -> cr.decommissionUnit(u));
            assertEquals(before2, cr.getStatus());
        }
    }

    @Test
    void transferUnit_requiresIdle_and_destinationHasSpace() throws Exception {
        int s1 = cr.addStation("A", 0, 0);
        int s2 = cr.addStation("B", 4, 4);
        cr.setStationCapacity(s2, 1);

        int u1 = cr.addUnit(s1, UnitType.FIRE_ENGINE);
        int u2 = cr.addUnit(s1, UnitType.FIRE_ENGINE);

        // Fill destination
        cr.transferUnit(u1, s2);

        // Now destination full; transferring another should fail and not move anything
        String before = cr.getStatus();
        assertThrows(IllegalStateException.class, () -> cr.transferUnit(u2, s2));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void idsAreNeverReused_afterRemoval() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u1 = cr.addUnit(s, UnitType.AMBULANCE);
        cr.decommissionUnit(u1);

        int u2 = cr.addUnit(s, UnitType.AMBULANCE);
        assertTrue(u2 > u1, "Unit IDs must be monotonically increasing and never reused");
    }

    // -------------------------
    // incidents
    // -------------------------

    @Test
    void reportIncident_invalidSeverity_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(InvalidSeverityException.class, () -> cr.reportIncident(IncidentType.FIRE, 0, 1, 1));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void reportIncident_onBlockedCell_throws_andStateUnchanged() throws Exception {
        cr.addObstacle(2, 2);
        String before = cr.getStatus();
        assertThrows(InvalidLocationException.class, () -> cr.reportIncident(IncidentType.FIRE, 3, 2, 2));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void cancelIncident_releasedUnitBecomesIdle_andStaysWhereItIs() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);
        int i = cr.reportIncident(IncidentType.CRIME, 2, 0, 3);
        cr.dispatch();

        // Move once so unit is no longer at station
        cr.tick();
        String unitAfterMove = cr.viewUnit(u);
        assertTrue(unitAfterMove.contains("STATUS=EN_ROUTE"));

        // Cancel while DISPATCHED -> unit IDLE at current location, does not return home
        cr.cancelIncident(i);

        String unitAfterCancel = cr.viewUnit(u);
        assertTrue(unitAfterCancel.contains("STATUS=IDLE"));
        assertTrue(unitAfterCancel.contains("INCIDENT=-"));

        // Location should remain same as just after move
        String locPart = unitAfterMove.substring(unitAfterMove.indexOf("LOC="), unitAfterMove.indexOf(" STATUS="));
        assertTrue(unitAfterCancel.contains(locPart));
    }

    @Test
    void cancelIncident_onlyAllowedReportedOrDispatched() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u = cr.addUnit(s, UnitType.AMBULANCE);
        int i = cr.reportIncident(IncidentType.MEDICAL, 2, 0, 1);
        cr.dispatch();

        // Move to arrive and start work
        cr.tick();

        // Now IN_PROGRESS, cancellation must throw and not change state
        if (cr.viewIncident(i).contains("STATUS=IN_PROGRESS")) {
            String before = cr.getStatus();
            assertThrows(IllegalStateException.class, () -> cr.cancelIncident(i));
            assertEquals(before, cr.getStatus());
        }
    }

    @Test
    void escalateIncident_notAllowedResolvedOrCancelled() throws Exception {
        int s = cr.addStation("A", 0, 0);
        cr.addUnit(s, UnitType.AMBULANCE);
        int i = cr.reportIncident(IncidentType.MEDICAL, 1, 0, 1);

        // Cancelled case
        cr.cancelIncident(i);
        String before = cr.getStatus();
        assertThrows(IllegalStateException.class, () -> cr.escalateIncident(i, 5));
        assertEquals(before, cr.getStatus());
    }
}