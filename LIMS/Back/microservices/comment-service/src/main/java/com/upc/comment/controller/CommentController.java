package com.upc.comment.controller;

import com.github.pagehelper.PageInfo;
import com.upc.comment.service.CommentService;
import com.upc.common.entity.Comment;
import com.upc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "图书评论管理接口")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "根据书名查询评论")
    @GetMapping("/byTitle")
    public Result<List<Comment>> getCommentsByTitle(@RequestParam String title) {
        List<Comment> comments = commentService.getCommentsByTitle(title);
        return Result.success(comments);
    }

    @Operation(summary = "添加评论")
    @PostMapping("/add")
    public Result<String> addComment(@RequestBody Comment comment) {
        boolean success = commentService.addComment(comment);
        if (success) {
            return Result.success("评论添加成功");
        } else {
            return Result.fail(500, "评论添加失败");
        }
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{commentId}")
    public Result<String> deleteComment(@PathVariable Integer commentId) {
        boolean success = commentService.deleteComment(commentId);
        if (success) {
            return Result.success("评论删除成功");
        } else {
            return Result.fail(500, "评论删除失败");
        }
    }

    @Operation(summary = "根据用户ID查询评论")
    @GetMapping("/byUser")
    public Result<PageInfo<Comment>> getCommentsByUser(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageInfo<Comment> comments = commentService.getCommentsByUser(userId, pageNum, pageSize);
        return Result.success(comments);
    }

    @Operation(summary = "查询评论详情")
    @GetMapping("/{commentId}")
    public Result<Comment> getCommentById(@PathVariable Integer commentId) {
        Comment comment = commentService.getCommentById(commentId);
        if (comment != null) {
            return Result.success(comment);
        } else {
            return Result.fail(404, "评论不存在");
        }
    }

    @Operation(summary = "更新评论")
    @PutMapping("/update")
    public Result<String> updateComment(@RequestBody Comment comment) {
        boolean success = commentService.updateComment(comment);
        if (success) {
            return Result.success("评论更新成功");
        } else {
            return Result.fail(500, "评论更新失败");
        }
    }

    @Operation(summary = "获取图书平均评分")
    @GetMapping("/averageRating/{title}")
    public Result<Double> getAverageRating(@PathVariable String title) {
        Double rating = commentService.getAverageRating(title);
        return Result.success(rating);
    }
}
