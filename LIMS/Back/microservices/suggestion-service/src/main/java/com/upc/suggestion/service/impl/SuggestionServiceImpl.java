package com.upc.suggestion.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Suggestion;
import com.upc.common.entity.Users;
import com.upc.common.feign.UserFeignClient;
import com.upc.common.result.Result;
import com.upc.suggestion.mapper.SuggestionMapper;
import com.upc.suggestion.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final SuggestionMapper suggestionMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public List<Suggestion> findAll() {
        List<Suggestion> list = suggestionMapper.selectList(null);
        list.forEach(this::fillUserInfo);
        return list;
    }

    @Override
    public PageInfo<Suggestion> findByPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Suggestion> list = suggestionMapper.selectList(null);
        list.forEach(this::fillUserInfo);
        return new PageInfo<>(list);
    }

    @Override
    public Suggestion findById(Integer suggestionId) {
        Suggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion != null) {
            fillUserInfo(suggestion);
        }
        return suggestion;
    }

    @Override
    public boolean add(Suggestion suggestion) {
        // 验证用户是否存在
        if (suggestion.getUserId() != null) {
            Result<Boolean> userExists = userFeignClient.checkUserExists(suggestion.getUserId());
            if (userExists.getData() == null || !userExists.getData()) {
                log.warn("添加留言失败：用户不存在 - {}", suggestion.getUserId());
                return false;
            }
        }

        // 设置创建时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        suggestion.setCreateTime(now);
        suggestion.setStatus(0); // 0-未回复

        int result = suggestionMapper.insert(suggestion);
        return result > 0;
    }

    @Override
    public boolean update(Suggestion suggestion) {
        int result = suggestionMapper.updateById(suggestion);
        return result > 0;
    }

    @Override
    public boolean delete(Integer suggestionId) {
        int result = suggestionMapper.deleteById(suggestionId);
        return result > 0;
    }

    @Override
    public List<Suggestion> findByUserId(Integer userId) {
        List<Suggestion> list = suggestionMapper.findByUserId(userId);
        list.forEach(this::fillUserInfo);
        return list;
    }

    @Override
    public boolean reply(Integer suggestionId, String reply) {
        Suggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            return false;
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        suggestion.setReply(reply);
        suggestion.setReplyTime(now);
        suggestion.setStatus(1); // 1-已回复

        int result = suggestionMapper.updateById(suggestion);
        return result > 0;
    }

    private void fillUserInfo(Suggestion suggestion) {
        if (suggestion.getUserId() != null) {
            try {
                Result<Users> userResult = userFeignClient.getUserById(suggestion.getUserId());
                if (userResult.getData() != null) {
                    suggestion.setUsername(userResult.getData().getUsername());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: {}", e.getMessage());
            }
        }
    }
}
