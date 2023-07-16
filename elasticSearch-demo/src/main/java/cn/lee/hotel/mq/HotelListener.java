package cn.lee.hotel.mq;

import cn.lee.hotel.constants.MqConstants;
import cn.lee.hotel.pojo.Hotel;
import cn.lee.hotel.pojo.HotelDoc;
import cn.lee.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HotelListener {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

    @RabbitListener(queues = MqConstants.HOTEL_INSERT_QUEUE)
    public void listenInsertOrUpdate(Long id) throws IOException {
        Hotel hotel = hotelService.getById(id);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        IndexRequest indexRequest = new IndexRequest("hotel").id(hotel.getId().toString());
        indexRequest.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        client.index(indexRequest, RequestOptions.DEFAULT);
    }

    @RabbitListener(queues = MqConstants.HOTEL_DELETE_QUEUE)
    public void listenDelete(Long id) throws IOException {
        DeleteRequest request = new DeleteRequest("hotel", id.toString());
        client.delete(request, RequestOptions.DEFAULT);
    }

}
