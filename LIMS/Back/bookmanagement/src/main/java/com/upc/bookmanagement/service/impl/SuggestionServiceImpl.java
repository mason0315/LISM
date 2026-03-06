package com.upc.bookmanagement.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Suggestion;
import com.upc.bookmanagement.mapper.SuggestionMapper;
import com.upc.bookmanagement.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    public final SuggestionMapper suggestionMapper;

    @Override
    public PageInfo<Suggestion> findAllSuggestions(Integer pageNum, Integer pageSize, String userId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Suggestion> suggestions;
        if (userId != null && !userId.isEmpty()) {
            suggestions = suggestionMapper.getSuggestionsByUserId(userId);
        } else {
            suggestions = suggestionMapper.getAllSuggestions();
        }
        return PageInfo.of(suggestions);
    }

    @Override
    public boolean addSuggestion(Suggestion suggestion) {
        return suggestionMapper.insert(suggestion) > 0;
    }

    @Override
    public boolean deleteSuggestion(Integer id) {
        return suggestionMapper.deleteById(id) > 0;
    }
}
