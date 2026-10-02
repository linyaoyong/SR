package com.share.rental.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.rental.auth.entity.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {
}
