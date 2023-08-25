package com.union.redisdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.union.redisdemo.entity.Blog;
import com.union.redisdemo.mapper.BlogMapper;
import com.union.redisdemo.service.IBlogService;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
