package com.share.rental.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.item.dto.CategoryResponse;
import com.share.rental.item.entity.Category;
import com.share.rental.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    @Autowired
    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> list() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder));
        return categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.getStatus()))
                .collect(Collectors.toList());
    }
}
