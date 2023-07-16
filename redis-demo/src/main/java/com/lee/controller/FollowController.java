package com.lee.controller;


import com.lee.dto.Result;
import com.lee.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    @GetMapping("/or/not/{id}")
    public Result isFollower(@PathVariable("id")Long id){
        return followService.isFollower(id);
    }

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id")Long followUserId, @PathVariable("isFollow")Boolean isFollow){
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id")Long id){
        return followService.commonFollow(id);
    }

}
