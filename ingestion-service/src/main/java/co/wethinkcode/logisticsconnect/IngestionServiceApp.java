package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IngestionServiceApp {

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));
        List<String[]> hubs = readCsv();
        System.out.println("Loaded hubs: " + hubs.size());
        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }

    private static List<String[]> readCsv() throws Exception {
        Set<String> seenHubIds = new HashSet<>();
        List<String[]> cleanedHubs = new ArrayList<>();
        InputStream file = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv");
        if (file == null) {
            throw new IllegalStateException("file not found");
        }

        try (InputStreamReader reader = new InputStreamReader(file, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            String[] header = csvReader.readNext();
            System.out.println("CSV header: " + Arrays.toString(header));
            String[] row = csvReader.readNext();
            int recordCounter = 0;

            while (row != null) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = row[i].trim();
                }

                boolean first = seenHubIds.add(row[0]);
                if (!row[0].isEmpty() && first) {
                    if (!row[1].isEmpty()) {
                        String province = row[1];
                        String firstLetter = province.substring(0, 1).toUpperCase();
                        String otherLetters = province.substring(1).toLowerCase();
                        row[1] = firstLetter + otherLetters;
                    } else {
                        row[1] = "unknown";
                    }
                    row[3] = row[3].toLowerCase();
                    if (row[3].isEmpty() || row[3].equals("unknown") || row[3].equals("n/a")) {
                        row[3] = "unknown";
                    } else if (row[3].equals("y") || row[3].equals("yes") || row[3].equals("1") || row[3].equals("true")) {
                        row[3] = "true";
                    } else if (row[3].equals("n") || row[3].equals("no") || row[3].equals("0") || row[3].equals("false")) {
                        row[3] = "false";
                    } else {
                        row[3] = "unknown";
                    }
                    cleanedHubs.add(row);
                    System.out.println(Arrays.toString(row));
                }
                row = csvReader.readNext();
                recordCounter += 1;
            }
            System.out.println("Total records read: " + recordCounter);
            System.out.println("Unique records stored: " + cleanedHubs.size());
            }
        return cleanedHubs;
        }
    }