package com.upc.comment.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Comment;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 根据书名查询评论
     *
     * @param title 书名
     * @return 评论列表
     */
    List<Comment> getCommentsByTitle(String title);

    /**
     * 添加评论
     *
     * @param comment 评论信息
     * @return 是否成功
     */
    boolean addComment(Comment comment);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean deleteComment(Integer commentId);

    /**
     * 根据用户ID查询评论
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 评论分页列表
     */
    PageInfo<Comment> getCommentsByUser(Integer userId, int pageNum, int pageSize);

    /**
     * 根据ID查询评论
     *
     * @param commentId 评论ID
     * @return 评论信息
     */
    Comment getCommentById(Integer commentId);

    /**
     * 更新评论
     *
     * @param comment 评论信息
     * @return 是否成功
     */
    boolean updateComment(Comment comment);

    /**
     * 获取图书平均评分
     *
     * @param title 书名
     * @return 平均评分
     */
    Double getAverageRating(String title);
}
