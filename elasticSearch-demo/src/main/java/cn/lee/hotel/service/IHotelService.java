package cn.lee.hotel.service;

import cn.lee.hotel.pojo.Hotel;
import cn.lee.hotel.pojo.PageResult;
import cn.lee.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    PageResult search(RequestParams params);

    Map<String, List<String>> filters(RequestParams params);

    List<String> suggest(String key);
}
