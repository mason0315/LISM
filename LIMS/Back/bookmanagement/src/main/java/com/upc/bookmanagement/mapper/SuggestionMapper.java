package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.Suggestion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SuggestionMapper extends BaseMapper<Suggestion> {

    List<Suggestion> getAllSuggestions();

    List<Suggestion> getSuggestionsByUserId(String userId);

}
