package com.upc.bookmanagement.controller;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Shelves;
import com.upc.bookmanagement.service.ShelvesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-18
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/shelves")
@Tag(name = "货架管理")
public class ShelvesController {

    private final ShelvesService shelvesService;

    @Operation(summary = "查询所有货架")
    @GetMapping("/getAllShelves")
    public Result<PageInfo<Shelves>> getAllShelves(@RequestParam Integer pageNum, @RequestParam Integer pageSize){
        return Result.success(shelvesService.getAllShelves(pageNum, pageSize));
    }

    @Operation(summary = "根据货架ID查询货架")
    @GetMapping("/getShelvesById/{shelveId}")
    public Result<PageInfo<Shelves>> getShelvesById(@PathVariable Integer shelveId, @RequestParam Integer pageNum, @RequestParam Integer pageSize){
        return Result.success(shelvesService.getShelvesById(shelveId, pageNum, pageSize));
    }

    @Operation(summary = "根据书名查询货架")
    @GetMapping("/getShelvesByTitle/{title}")
    public Result<PageInfo<Shelves>> getShelvesByTitle(@PathVariable String title, @RequestParam Integer pageNum, @RequestParam Integer pageSize){
        return Result.success(shelvesService.getShelvesByTitle(title, pageNum, pageSize));
    }

    @Operation(summary = "添加货架")
    @PostMapping("/addShelves")
    public Result<String> addShelves(@RequestBody Shelves shelves){
        boolean flag = shelvesService.addShelves(shelves);
        if(flag){
            return Result.success("添加成功");
        }else{
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "删除货架")
    @DeleteMapping("/deleteShelves")
    public Result<String> deleteShelves(@RequestParam Integer shelveId){
        boolean flag = shelvesService.deleteShelves(shelveId);
        if(flag){
            return Result.success("删除成功");
        }else{
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "根据书名删除货架")
    @DeleteMapping("/deleteShelvesByTitle/{title}")
    public Result<String> deleteShelvesByTitle(@PathVariable String title){
        boolean flag = shelvesService.deleteShelvesByTitle(title);
        if(flag){
            return Result.success("删除成功");
        }else{
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "编辑货架")
    @PutMapping("/updateShelves")
    public Result<String> updateShelves(@RequestBody Shelves shelves) {
        boolean flag = shelvesService.updateShelves(shelves);
        return flag ? Result.success("编辑成功") : Result.fail(500, "编辑失败");
    }

    @Operation(summary = "获取某货架下所有图书")
    @GetMapping("/getBooksByShelveId/{shelveId}")
    public Result<List<String>> getBooksByShelveId(@PathVariable Integer shelveId) {
        return Result.success(shelvesService.getBooksByShelveId(shelveId));
    }

    @Operation(summary = "为货架添加图书")
    @PostMapping("/addBookToShelve")
    public Result<String> addBookToShelve(@RequestParam Integer shelveId, @RequestParam String title, @RequestParam String category) {
        boolean flag = shelvesService.addBookToShelve(shelveId, title, category);
        return flag ? Result.success("添加成功") : Result.fail(500, "添加失败");
    }

    @Operation(summary = "从货架移除图书")
    @DeleteMapping("/removeBookFromShelve")
    public Result<String> removeBookFromShelve(@RequestParam Integer shelveId, @RequestParam String title) {
        boolean flag = shelvesService.removeBookFromShelve(shelveId, title);
        return flag ? Result.success("移除成功") : Result.fail(500, "移除失败");
    }

    // 分类管理接口（如需新建分类表可后续实现）
    @Operation(summary = "获取所有货架分类")
    @GetMapping("/getAllCategories")
    public Result<List<String>> getAllCategories() {
        return Result.success(shelvesService.getAllCategories());
    }
}
