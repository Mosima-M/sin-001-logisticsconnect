package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IngestionServiceApp {
    private Hub hub;

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));
        List<Hub> hubs = HubCsvReader.readCsv();
//        System.out.println("Loaded hubs: " + hubs.size());
        app.get("/hubs", ctx -> {
            ctx.json(hubs);
        });
    }

    static List<Hub> readCsv() throws Exception {
        return HubCsvReader.readCsv();
    }

}