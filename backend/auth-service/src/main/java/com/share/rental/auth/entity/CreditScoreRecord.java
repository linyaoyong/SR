package com.share.rental.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_score_records")
public class CreditScoreRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orderId;
    private Integer changeValue;
    private Integer beforeScore;
    private Integer afterScore;
    private String reasonType;
    private String reason;
    private LocalDateTime createTime;
}
