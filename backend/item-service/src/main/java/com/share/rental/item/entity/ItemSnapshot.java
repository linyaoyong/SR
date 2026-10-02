package com.share.rental.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("item_snapshots")
public class ItemSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long ownerId;
    private String title;
    private String description;
    private String categoryName;
    private String imageUrls;
    private Integer priceType;
    private BigDecimal dailyPrice;
    private BigDecimal depositAmount;
    private Integer supportDelivery;
    private Integer supportMeetup;
    private String snapshotJson;
    private LocalDateTime createTime;
}
