package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngestionServiceAppTest {

    @Test
    void shouldLoadAllHubsFromCsv() throws Exception {
        List<Hub> hubs = HubCsvReader.readCsv();

        assertEquals(18, hubs.size());
    }

    @Test
    void shouldNormalizeHubIdsToUppercase() throws Exception {
        List<Hub> hubs = HubCsvReader.readCsv();

        Hub hub = hubs.get(1);

        assertEquals("H-501", hub.getHubId());
    }

    @Test
    void shouldNormalizeProvinceNames() throws Exception {
        List<Hub> hubs = HubCsvReader.readCsv();

        Hub hub = hubs.get(3);

        assertEquals("KwaZulu-Natal", hub.getProvince());
    }

    @Test
    void shouldNormalizeSortingCenter() throws Exception {
        List<Hub> hubs = HubCsvReader.readCsv();

        Hub hub = hubs.get(5);

        assertEquals("Cape Town Port", hub.getSortingCenter());
    }

    @Test
    void shouldConvertActiveStatusToBoolean() throws Exception {
        List<Hub> hubs = HubCsvReader.readCsv();

        assertEquals(Boolean.TRUE, hubs.get(0).getActive());
        assertEquals(Boolean.FALSE, hubs.get(2).getActive());
        assertNull(hubs.get(11).getActive());
    }
}
