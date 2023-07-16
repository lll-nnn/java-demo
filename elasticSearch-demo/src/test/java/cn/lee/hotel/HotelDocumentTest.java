package cn.lee.hotel;

import cn.lee.hotel.pojo.Hotel;
import cn.lee.hotel.pojo.HotelDoc;
import cn.lee.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class HotelDocumentTest {

    private RestHighLevelClient client;

    @Autowired
    private IHotelService service;

    /**
     * 添加和更新都可
     * @throws IOException
     */
    @Test
    void addAndUpdateDocument() throws IOException {
        Hotel hotel = service.getById(38609L);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void getDocument() throws IOException {
        GetRequest request = new GetRequest("hotel", "38609");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void updateDocument() throws IOException {
        UpdateRequest request = new UpdateRequest("hotel", "38609");
        Hotel hotel = service.getById(38609L);
        hotel.setCity("河南");
        hotel.setBrand("document");
        HotelDoc hotelDoc = new HotelDoc(hotel);
        IndexRequest request1 = new IndexRequest("hotel").id(hotel.getId().toString());
        request1.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        request.doc(request1);
//        request.doc(
//                "name","hello world",
//                "price",9999
//        );
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void deleteDocument() throws IOException {
        DeleteRequest request = new DeleteRequest("hotel", "38609");
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void bulkAdd() throws IOException {
        BulkRequest request = new BulkRequest();
        List<Hotel> hotels = service.list();
        for (Hotel hotel : hotels) {
            IndexRequest indexRequest = new IndexRequest("hotel").id(hotel.getId().toString());
            HotelDoc hotelDoc = new HotelDoc(hotel);
            indexRequest.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            request.add(indexRequest);
        }
        client.bulk(request, RequestOptions.DEFAULT);
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
