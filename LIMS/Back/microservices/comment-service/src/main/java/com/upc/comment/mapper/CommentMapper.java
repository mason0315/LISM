package com.upc.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据书名查询评论
     *
     * @param title 书名
     * @return 评论列表
     */
    @Select("SELECT * FROM comment_info WHERE title = #{title} ORDER BY create_time DESC")
    List<Comment> findByTitle(@Param("title") String title);

    /**
     * 根据用户ID查询评论
     *
     * @param userId 用户ID
     * @return 评论列表
     */
    @Select("SELECT * FROM comment_info WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Comment> findByUserId(@Param("userId") Integer userId);

    /**
     * 查询图书平均评分
     *
     * @param title 书名
     * @return 平均评分
     */
    @Select("SELECT AVG(rating) FROM comment_info WHERE title = #{title}")
    Double selectAverageRating(@Param("title") String title);
}
