package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HubCsvReader {
    private static String normalizeProvince(String province) {
        String normalized = province.toLowerCase();

        normalized = normalized.replace(" ", "");
        normalized = normalized.replace("-", "");
        return normalized;
    }
    private static String normalizeSortingCenter(String sortingCenter) {
        sortingCenter = sortingCenter.replaceAll("\\s+", " ").trim();

        String[] words = sortingCenter.toLowerCase().split(" ");
        String result = "";

        for (String word : words) {
            if (!word.isEmpty()) {
                result += word.substring(0, 1).toUpperCase()
                        + word.substring(1) + " ";
            }
        }

        return result.trim();
    }

    public static List<Hub> readCsv() throws Exception {
        Set<String> seenHubIds = new HashSet<>();
        List<Hub> cleanedHubs = new ArrayList<>();
        InputStream file = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv");
        if (file == null) {
            throw new IllegalStateException("file not found");
        }

        try (InputStreamReader reader = new InputStreamReader(file, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            String[] row = csvReader.readNext();

            while (row != null) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = row[i].trim();
                }

                row[0] = row[0].toUpperCase();
                boolean first = seenHubIds.add(row[0]);
                Boolean activeStatus;

                if (!row[0].isEmpty() && first) {
                    if (!row[1].isEmpty()) {
                        row[1] = normalizeProvince(row[1]);

                        switch (row[1]) {
                            case "kwazulunatal" -> row[1] = "KwaZulu-Natal";
                            case "gauteng" -> row[1] = "Gauteng";
                            case "northwest" -> row[1] = "North West";
                            case "mpumalanga" -> row[1] = "Mpumalanga";
                            case "freestate" -> row[1] = "Free State";
                            case "westerncape" -> row[1] = "Western Cape";
                            case "limpopo" -> row[1] = "Limpopo";
                            case "easterncape" -> row[1] = "Eastern Cape";
                            case "northerncape" -> row[1] = "Northern Cape";
                            default -> row[1] = "unknown";
                        }
                    } else {
                        row[1] = "unknown";
                    }

                    row[3] = row[3].toLowerCase();

                    if (row[3].isEmpty() || row[3].equals("unknown") || row[3].equals("n/a")) {
                        activeStatus = null;
                    } else if (row[3].equals("y") || row[3].equals("yes") || row[3].equals("1") || row[3].equals("true")) {
                        activeStatus = true;
                    } else if (row[3].equals("n") || row[3].equals("no") || row[3].equals("0") || row[3].equals("false")) {
                        activeStatus = false;
                    } else {
                        activeStatus = null;
                    }
                    row[2] = normalizeSortingCenter(row[2]);
                    Hub newHub = new Hub(row[0], row[1], row[2], activeStatus);
                    cleanedHubs.add(newHub);

                }

                row = csvReader.readNext();
            }
        }
        return cleanedHubs;
    }
}
