package com.share.rental.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item_images")
public class ItemImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String url;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private Integer deleted;
}
