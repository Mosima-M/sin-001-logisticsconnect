package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class IngestionServiceApp {

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));
        readCsv();
        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }
    private static void readCsv() throws Exception{

            InputStream file = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv");
            if( file == null){
                throw new IllegalStateException("file not found");
            }

            try(InputStreamReader reader = new InputStreamReader(file, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReader(reader)){
//            csvReader.readNext();
            System.out.println(Arrays.toString(csvReader.readNext()));
    }
    }
}
