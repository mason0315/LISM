package com.upc.bookmanagement.service.impl;

import com.upc.bookmanagement.entity.Comment;
import com.upc.bookmanagement.mapper.CommentMapper;
import com.upc.bookmanagement.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;

    @Override
    public List<Comment> getCommentsByTitle(String title) {
        return commentMapper.getCommentsByTitle(title);
    }

    @Override
    public boolean addComment(Comment comment) {
        return commentMapper.insert(comment) > 0;
    }
} 