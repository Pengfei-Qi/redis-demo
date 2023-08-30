package com.union.redisdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.ShopType;


public interface IShopTypeService extends IService<ShopType> {

    Result queryList();
}
