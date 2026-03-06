package com.upc.bookmanagement.controller;

import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Comment;
import com.upc.bookmanagement.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/byTitle")
    public Result<List<Comment>> getCommentsByTitle(@RequestParam String title) {
        List<Comment> comments = commentService.getCommentsByTitle(title);
        return Result.success(comments);
    }

    @PostMapping("/add")
    public Result<String> addComment(@RequestBody Comment comment) {
        boolean success = commentService.addComment(comment);
        if (success) {
            return Result.success("评论添加成功");
        } else {
            return Result.fail(500, "评论添加失败");
        }
    }
} 