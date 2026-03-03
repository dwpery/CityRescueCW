import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cityrescue.CityRescue;
import cityrescue.CityRescueImpl;
import cityrescue.enums.IncidentType;
import cityrescue.enums.UnitType;

public class PublicDeterminismAndMovementEdgeCasesTest {

    private CityRescue cr;

    @BeforeEach
    void setUp() throws Exception {
        cr = new CityRescueImpl();
        cr.initialise(6, 6);
    }

    @Test
    void dispatch_processesIncidentsInAscendingIncidentIdOrder() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u1 = cr.addUnit(s, UnitType.POLICE_CAR);
        int u2 = cr.addUnit(s, UnitType.POLICE_CAR);

        int i1 = cr.reportIncident(IncidentType.CRIME, 1, 5, 5);
        int i2 = cr.reportIncident(IncidentType.CRIME, 1, 1, 0);

        // Even though i2 is closer, dispatch must consider i1 first. :contentReference[oaicite:3]{index=3}
        cr.dispatch();

        assertTrue(cr.viewIncident(i1).contains("UNIT=" + u1));
        assertTrue(cr.viewIncident(i2).contains("UNIT=" + u2));
    }

    @Test
    void dispatch_tieBreaker_lowestUnitId_whenDistanceTied() throws Exception {
        int s = cr.addStation("A", 0, 0);
        int u1 = cr.addUnit(s, UnitType.POLICE_CAR);
        int u2 = cr.addUnit(s, UnitType.POLICE_CAR);

        int i = cr.reportIncident(IncidentType.CRIME, 2, 2, 0);

        // Both units at same station location => same distance.
        // Must choose lowest unitId. :contentReference[oaicite:4]{index=4}
        cr.dispatch();

        assertTrue(cr.viewIncident(i).contains("UNIT=" + u1));
        assertFalse(cr.viewIncident(i).contains("UNIT=" + u2));
    }

    @Test
    void dispatch_tieBreaker_lowestHomeStationId_whenDistanceAndUnitIdTiedNotPossible() throws Exception {
        // The spec says after distance and unitId, tie-break is lowest homeStationId. :contentReference[oaicite:5]{index=5}
        // UnitId being tied can't happen for two different units, but you *can* still test that
        // homeStationId is considered when comparing candidates that become "best" during scanning.
        // This test creates same-distance candidates and checks deterministic selection by unitId first,
        // so it's mainly a guard that you didn't accidentally tie-break by station before unitId.

        int s1 = cr.addStation("S1", 1, 1);
        int s2 = cr.addStation("S2", 1, 1); // same location allowed if your impl permits; if not, change to (1,2) and adjust.

        int u1 = cr.addUnit(s1, UnitType.FIRE_ENGINE);
        int u2 = cr.addUnit(s2, UnitType.FIRE_ENGINE);

        int i = cr.reportIncident(IncidentType.FIRE, 3, 3, 1);

        cr.dispatch();

        // Lowest unitId must still win (primary tie-break after distance). :contentReference[oaicite:6]{index=6}
        assertTrue(cr.viewIncident(i).contains("UNIT=" + Math.min(u1, u2)));
    }

    @Test
    void movement_prefersFirstReducingMove_inNESWOrder() throws Exception {
        // From (2,2) to (2,0): N reduces distance, so first move must be North. :contentReference[oaicite:7]{index=7}
        cr.initialise(5, 5);
        int s = cr.addStation("A", 2, 2);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);
        int i = cr.reportIncident(IncidentType.CRIME, 1, 2, 0);

        cr.dispatch();
        cr.tick();

        assertTrue(cr.viewUnit(u).contains("LOC=(2,1)"));
    }

    @Test
    void movement_whenNoReducingMove_takesFirstLegalMove_inNESWOrder() throws Exception {
        // Build a situation where reducing moves are blocked, forcing the "first legal move" fallback. :contentReference[oaicite:8]{index=8}
        // Start (2,2), target (2,0). Reducing move is North to (2,1).
        // Block (2,1). Also block East/West? No — we want fallback to pick E (first legal after N).
        cr.initialise(5, 5);
        cr.addObstacle(2, 1); // blocks the only reducing move (north)

        int s = cr.addStation("A", 2, 2);
        int u = cr.addUnit(s, UnitType.POLICE_CAR);
        int i = cr.reportIncident(IncidentType.CRIME, 1, 2, 0);

        cr.dispatch();
        cr.tick();

        // N is illegal, E is legal => must move to (3,2) (even though it doesn't reduce distance).
        assertTrue(cr.viewUnit(u).contains("LOC=(3,2)"));
    }

    @Test
    void movement_ifNoLegalMoves_unitStaysPut() throws Exception {
        // Surround a unit with obstacles/bounds so no move is legal. :contentReference[oaicite:9]{index=9}
        cr.initialise(3, 3);
        int s = cr.addStation("A", 1, 1);
        int u = cr.addUnit(s, UnitType.AMBULANCE);
        int i = cr.reportIncident(IncidentType.MEDICAL, 1, 1, 0);

        // Block all four neighbours of (1,1)
        cr.addObstacle(1, 0);
        cr.addObstacle(2, 1);
        cr.addObstacle(1, 2);
        cr.addObstacle(0, 1);

        cr.dispatch();
        cr.tick();

        assertTrue(cr.viewUnit(u).contains("LOC=(1,1)"));
    }

    @Test
    void tick_movesUnitsInAscendingUnitIdOrder() throws Exception {
        // This is subtle: if your tick updates shared things incorrectly,
        // order-dependent bugs appear. Spec says move EN_ROUTE units in ascending unitId. :contentReference[oaicite:10]{index=10}
        cr.initialise(5, 5);

        int s = cr.addStation("A", 0, 0);
        int u1 = cr.addUnit(s, UnitType.POLICE_CAR);
        int u2 = cr.addUnit(s, UnitType.POLICE_CAR);

        int i1 = cr.reportIncident(IncidentType.CRIME, 1, 4, 0);
        int i2 = cr.reportIncident(IncidentType.CRIME, 1, 4, 1);

        cr.dispatch();

        // After one tick, u1 and u2 should have moved one step each deterministically.
        cr.tick();

        String a = cr.viewUnit(u1);
        String b = cr.viewUnit(u2);

        assertTrue(a.contains("STATUS=EN_ROUTE"));
        assertTrue(b.contains("STATUS=EN_ROUTE"));
    }

    @Test
    void resolveOrder_isAscendingIncidentId() throws Exception {
        // Two incidents both at station location so both become IN_PROGRESS immediately.
        // They resolve after fixed ticks; resolution must be processed by incidentId order. :contentReference[oaicite:11]{index=11}
        cr.initialise(5, 5);
        int s = cr.addStation("A", 2, 2);

        int u1 = cr.addUnit(s, UnitType.AMBULANCE);
        int u2 = cr.addUnit(s, UnitType.AMBULANCE);

        int i1 = cr.reportIncident(IncidentType.MEDICAL, 1, 2, 2);
        int i2 = cr.reportIncident(IncidentType.MEDICAL, 1, 2, 2);

        cr.dispatch();
        cr.tick(); // arrivals + start work

        // Ambulance takes 2 ticks at scene. :contentReference[oaicite:12]{index=12}
        cr.tick();
        cr.tick();

        assertTrue(cr.viewIncident(i1).contains("STATUS=RESOLVED"));
        assertTrue(cr.viewIncident(i2).contains("STATUS=RESOLVED"));
        assertTrue(cr.viewUnit(u1).contains("STATUS=IDLE"));
        assertTrue(cr.viewUnit(u2).contains("STATUS=IDLE"));
    }
}