package com.share.rental.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.rental.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
