package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EtaCalculatorTest {

    @Test
    void calculatesArrivalWindowWithNoDelay() {
        int[] result = EtaCalculator.calculate(0);

        assertArrayEquals(new int[]{50, 70}, result);
    }

    @Test
    void calculatesArrivalWindowWithStageThree() {
        int[] result = EtaCalculator.calculate(3);

        assertArrayEquals(new int[]{95, 115}, result);
    }
}
