import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import cityrescue.exceptions.InvalidLocationException;
import cityrescue.exceptions.InvalidSeverityException;

public class PublicMoreStateSafetyTest {

    private CityRescue cr;

    @BeforeEach
    void setUp() throws Exception {
        cr = new CityRescueImpl();
        cr.initialise(6, 6);
    }

    @Test
    void stationCapacity_zeroOrNegative_throws_andStateUnchanged() throws Exception {
        int s = cr.addStation("A", 1, 1);

        String before = cr.getStatus();
        assertThrows(InvalidCapacityException.class, () -> cr.setStationCapacity(s, 0));
        assertEquals(before, cr.getStatus());

        before = cr.getStatus();
        assertThrows(InvalidCapacityException.class, () -> cr.setStationCapacity(s, -10));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void removeStation_unknownId_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(IDNotRecognisedException.class, () -> cr.removeStation(9999));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void viewUnit_unknownId_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(IDNotRecognisedException.class, () -> cr.viewUnit(9999));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void viewIncident_unknownId_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(IDNotRecognisedException.class, () -> cr.viewIncident(9999));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void escalate_invalidSeverity_throws_andStateUnchanged() throws Exception {
        int s = cr.addStation("A", 0, 0);
        cr.addUnit(s, UnitType.FIRE_ENGINE);
        int i = cr.reportIncident(IncidentType.FIRE, 3, 3, 3);

        String before = cr.getStatus();
        assertThrows(InvalidSeverityException.class, () -> cr.escalateIncident(i, 6));
        assertEquals(before, cr.getStatus());

        before = cr.getStatus();
        assertThrows(InvalidSeverityException.class, () -> cr.escalateIncident(i, 0));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void cancel_unknownIncident_throws_andStateUnchanged() throws Exception {
        String before = cr.getStatus();
        assertThrows(IDNotRecognisedException.class, () -> cr.cancelIncident(9999));
        assertEquals(before, cr.getStatus());
    }

    @Test
    void addObstacle_thenTryAddStationOutOfBounds_throws_andDoesNotRemoveObstacle() throws Exception {
        cr.addObstacle(2, 2);
        String before = cr.getStatus();

        assertThrows(InvalidLocationException.class, () -> cr.addStation("B", -1, 0));
        assertEquals(before, cr.getStatus());
        assertTrue(cr.getStatus().contains("OBSTACLES=1"));
    }

    @Test
    void idListsAreSortedAscending_evenAfterRemovals() throws Exception {
        int s1 = cr.addStation("S1", 0, 0);
        int s2 = cr.addStation("S2", 1, 0);
        int s3 = cr.addStation("S3", 2, 0);

        // Remove middle station (only allowed if no units)
        cr.removeStation(s2);

        int[] stationIds = cr.getStationIds();
        assertArrayEquals(new int[] { s1, s3 }, stationIds);

        // Units: add 3, remove one, add another; returned IDs must be sorted
        int u1 = cr.addUnit(s1, UnitType.AMBULANCE);
        int u2 = cr.addUnit(s1, UnitType.AMBULANCE);
        int u3 = cr.addUnit(s1, UnitType.AMBULANCE);

        cr.decommissionUnit(u2);

        int u4 = cr.addUnit(s1, UnitType.AMBULANCE);

        int[] unitIds = cr.getUnitIds();
        // Should be ascending, and u2 missing
        assertArrayEquals(new int[] { u1, u3, u4 }, unitIds);
        assertTrue(u4 > u3);
    }

    @Test
    void getGridSize_returnsInitialisedSize() {
        assertArrayEquals(new int[] { 6, 6 }, cr.getGridSize());
    }
}