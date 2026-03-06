package com.upc.suggestion.controller;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Suggestion;
import com.upc.common.result.Result;
import com.upc.suggestion.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
@Tag(name = "留言管理", description = "用户留言反馈管理接口")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @Operation(summary = "查询所有留言")
    @GetMapping("/all")
    public Result<List<Suggestion>> findAll() {
        List<Suggestion> list = suggestionService.findAll();
        return Result.success(list);
    }

    @Operation(summary = "分页查询留言")
    @GetMapping("/page")
    public Result<PageInfo<Suggestion>> findByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageInfo<Suggestion> pageInfo = suggestionService.findByPage(pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "根据ID查询留言")
    @GetMapping("/{suggestionId}")
    public Result<Suggestion> findById(@PathVariable Integer suggestionId) {
        Suggestion suggestion = suggestionService.findById(suggestionId);
        if (suggestion != null) {
            return Result.success(suggestion);
        } else {
            return Result.fail(404, "留言不存在");
        }
    }

    @Operation(summary = "添加留言")
    @PostMapping("/add")
    public Result<String> add(@RequestBody Suggestion suggestion) {
        boolean success = suggestionService.add(suggestion);
        if (success) {
            return Result.success("留言添加成功");
        } else {
            return Result.fail(500, "留言添加失败");
        }
    }

    @Operation(summary = "更新留言")
    @PutMapping("/update")
    public Result<String> update(@RequestBody Suggestion suggestion) {
        boolean success = suggestionService.update(suggestion);
        if (success) {
            return Result.success("留言更新成功");
        } else {
            return Result.fail(500, "留言更新失败");
        }
    }

    @Operation(summary = "删除留言")
    @DeleteMapping("/{suggestionId}")
    public Result<String> delete(@PathVariable Integer suggestionId) {
        boolean success = suggestionService.delete(suggestionId);
        if (success) {
            return Result.success("留言删除成功");
        } else {
            return Result.fail(500, "留言删除失败");
        }
    }

    @Operation(summary = "根据用户ID查询留言")
    @GetMapping("/byUser/{userId}")
    public Result<List<Suggestion>> findByUserId(@PathVariable Integer userId) {
        List<Suggestion> list = suggestionService.findByUserId(userId);
        return Result.success(list);
    }

    @Operation(summary = "回复留言")
    @PutMapping("/reply/{suggestionId}")
    public Result<String> reply(@PathVariable Integer suggestionId, @RequestParam String reply) {
        boolean success = suggestionService.reply(suggestionId, reply);
        if (success) {
            return Result.success("回复成功");
        } else {
            return Result.fail(500, "回复失败");
        }
    }
}
