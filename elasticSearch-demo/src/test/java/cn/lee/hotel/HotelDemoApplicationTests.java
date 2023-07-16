package cn.lee.hotel;

import cn.lee.hotel.service.IHotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HotelDemoApplicationTests {

    @Autowired
    private IHotelService service;

    @Test
    void contextLoads() {
//        System.out.println(service.filters());
    }

}
