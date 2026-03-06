package com.upc.suggestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Suggestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SuggestionMapper extends BaseMapper<Suggestion> {

    @Select("SELECT * FROM suggestion WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Suggestion> findByUserId(@Param("userId") Integer userId);
}
