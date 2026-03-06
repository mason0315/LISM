package com.upc.suggestion.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Suggestion;

import java.util.List;

public interface SuggestionService {
    List<Suggestion> findAll();
    PageInfo<Suggestion> findByPage(int pageNum, int pageSize);
    Suggestion findById(Integer suggestionId);
    boolean add(Suggestion suggestion);
    boolean update(Suggestion suggestion);
    boolean delete(Integer suggestionId);
    List<Suggestion> findByUserId(Integer userId);
    boolean reply(Integer suggestionId, String reply);
}
