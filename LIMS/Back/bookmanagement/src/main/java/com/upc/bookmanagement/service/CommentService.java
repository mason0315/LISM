package com.upc.bookmanagement.service;

import com.upc.bookmanagement.entity.Comment;
import java.util.List;

public interface CommentService {
    List<Comment> getCommentsByTitle(String title);
    boolean addComment(Comment comment);
} 