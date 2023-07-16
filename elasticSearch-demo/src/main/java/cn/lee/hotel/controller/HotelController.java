package cn.lee.hotel.controller;

import cn.lee.hotel.pojo.PageResult;
import cn.lee.hotel.pojo.RequestParams;
import cn.lee.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;



    @PostMapping("list")
    public PageResult search(@RequestBody RequestParams params){
        return hotelService.search(params);
    }

    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody RequestParams params){
        return hotelService.filters(params);
    }

    @GetMapping("/suggestion")
    public List<String> suggest(@RequestParam("key") String key){
        return hotelService.suggest(key);
    }

}
