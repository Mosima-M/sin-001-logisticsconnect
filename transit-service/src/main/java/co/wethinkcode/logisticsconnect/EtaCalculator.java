package co.wethinkcode.logisticsconnect;

public class EtaCalculator {

    public static int[] calculate(int stage) {
        int baseMinutes = 60;
        int delayMinutes = stage * 15;
        int estimatedMinutes = baseMinutes + delayMinutes;

        int earliestMinutes = estimatedMinutes - 10;
        int latestMinutes = estimatedMinutes + 10;

        return new int[]{earliestMinutes, latestMinutes};
    }
}
