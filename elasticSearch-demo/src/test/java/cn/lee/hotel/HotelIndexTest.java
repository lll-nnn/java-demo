package cn.lee.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cn.lee.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

public class HotelIndexTest {

    private RestHighLevelClient client;

    @Test
    void testConnect() {
        System.out.println(client);
    }

    @Test
    void createHotelIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("hotel");

        request.source(MAPPING_TEMPLATE, XContentType.JSON);

        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void deleteHotelIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");
        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void isExistHotelIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("hotel");
        System.out.println(client.indices().exists(request, RequestOptions.DEFAULT));
    }

    @BeforeEach
    void beforeAll() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.41.134:9200")
        ));
    }

    @AfterEach
    void afterAll() throws IOException {
        this.client.close();
    }
}
