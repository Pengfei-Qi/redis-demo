package com.union.redisdemo.controller;


import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.ShopType;
import com.union.redisdemo.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        // List<ShopType> typeList = typeService
        //         .query().orderByAsc("sort").list();
        return typeService.queryList();
    }
}
