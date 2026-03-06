package com.upc.bookmanagement.service;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Suggestion;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-18
 */
public interface SuggestionService {

    PageInfo<Suggestion> findAllSuggestions(Integer pageNum, Integer pageSize, String userId);

    boolean addSuggestion(Suggestion suggestion);

    boolean deleteSuggestion(Integer id);
}
