package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    List<Comment> getCommentsByTitle(@Param("title") String title);
} 