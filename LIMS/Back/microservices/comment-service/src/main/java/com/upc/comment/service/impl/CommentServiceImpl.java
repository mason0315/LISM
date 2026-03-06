package com.upc.comment.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.comment.mapper.CommentMapper;
import com.upc.comment.service.CommentService;
import com.upc.common.entity.Comment;
import com.upc.common.entity.Users;
import com.upc.common.feign.BookFeignClient;
import com.upc.common.feign.UserFeignClient;
import com.upc.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final BookFeignClient bookFeignClient;
    private final UserFeignClient userFeignClient;

    @Override
    public List<Comment> getCommentsByTitle(String title) {
        List<Comment> comments = commentMapper.findByTitle(title);
        // 填充用户信息
        comments.forEach(this::fillUserInfo);
        return comments;
    }

    @Override
    public boolean addComment(Comment comment) {
        // 验证图书是否存在
        Result<Boolean> bookExists = bookFeignClient.checkBookExists(comment.getTitle());
        if (bookExists.getData() == null || !bookExists.getData()) {
            log.warn("添加评论失败：图书不存在 - {}", comment.getTitle());
            return false;
        }

        // 验证用户是否存在
        Result<Boolean> userExists = userFeignClient.checkUserExists(comment.getUserId());
        if (userExists.getData() == null || !userExists.getData()) {
            log.warn("添加评论失败：用户不存在 - {}", comment.getUserId());
            return false;
        }

        // 设置评论时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        comment.setCreateTime(now);

        int result = commentMapper.insert(comment);
        return result > 0;
    }

    @Override
    public boolean deleteComment(Integer commentId) {
        int result = commentMapper.deleteById(commentId);
        return result > 0;
    }

    @Override
    public PageInfo<Comment> getCommentsByUser(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.findByUserId(userId);
        // 填充用户信息
        comments.forEach(this::fillUserInfo);
        return new PageInfo<>(comments);
    }

    @Override
    public Comment getCommentById(Integer commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            fillUserInfo(comment);
        }
        return comment;
    }

    @Override
    public boolean updateComment(Comment comment) {
        int result = commentMapper.updateById(comment);
        return result > 0;
    }

    @Override
    public Double getAverageRating(String title) {
        Double avgRating = commentMapper.selectAverageRating(title);
        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * 填充评论的用户信息
     */
    private void fillUserInfo(Comment comment) {
        if (comment.getUserId() != null) {
            try {
                Result<Users> userResult = userFeignClient.getUserById(comment.getUserId());
                if (userResult.getData() != null) {
                    Users user = userResult.getData();
                    comment.setUsername(user.getUsername());
                    comment.setUserAvatar(user.getFaceImageUrl());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: {}", e.getMessage());
            }
        }
    }
}
