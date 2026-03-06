package com.upc.bookmanagement.controller;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Suggestion;
import com.upc.bookmanagement.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-18
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/suggestion")
@Tag(name = "留言管理")
public class SuggestionController {

    public final SuggestionService suggestionService;

    @Operation(summary = "查询所有留言")
    @GetMapping("/getAllSuggestions")
    public Result<PageInfo<Suggestion>> getAllSuggestions(@RequestParam Integer pageNum, @RequestParam Integer pageSize, @RequestParam(required = false) String userId){
        return Result.success(suggestionService.findAllSuggestions(pageNum, pageSize, userId));
    }

    @Operation(summary = "添加留言")
    @PostMapping("/addSuggestion")
    public Result<String> addSuggestion(@RequestBody Suggestion suggestion){
        boolean flag = suggestionService.addSuggestion(suggestion);
        if(flag){
            return Result.success("添加成功");
        }else{
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "删除留言")
    @DeleteMapping("/deleteSuggestion/{id}")
    public Result<String> deleteSuggestion(@PathVariable Integer id){
        boolean flag = suggestionService.deleteSuggestion(id);
        if(flag){
            return Result.success("删除成功");
        }else{
            return Result.fail(500, "删除失败");
        }
    }


}
