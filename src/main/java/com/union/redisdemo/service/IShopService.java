package com.union.redisdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.Shop;


public interface IShopService extends IService<Shop> {

    Result queryById(Long id);
}
