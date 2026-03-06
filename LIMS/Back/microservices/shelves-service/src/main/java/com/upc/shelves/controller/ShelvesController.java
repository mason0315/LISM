package com.upc.shelves.controller;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Shelves;
import com.upc.common.result.Result;
import com.upc.shelves.service.ShelvesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shelves")
@RequiredArgsConstructor
@Tag(name = "货架管理", description = "货架信息管理接口")
public class ShelvesController {

    private final ShelvesService shelvesService;

    @Operation(summary = "查询所有货架")
    @GetMapping("/all")
    public Result<List<Shelves>> findAll() {
        List<Shelves> list = shelvesService.findAll();
        return Result.success(list);
    }

    @Operation(summary = "分页查询货架")
    @GetMapping("/page")
    public Result<PageInfo<Shelves>> findByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageInfo<Shelves> pageInfo = shelvesService.findByPage(pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "根据ID查询货架")
    @GetMapping("/{shelvesId}")
    public Result<Shelves> findById(@PathVariable Integer shelvesId) {
        Shelves shelves = shelvesService.findById(shelvesId);
        if (shelves != null) {
            return Result.success(shelves);
        } else {
            return Result.fail(404, "货架不存在");
        }
    }

    @Operation(summary = "添加货架")
    @PostMapping("/add")
    public Result<String> add(@RequestBody Shelves shelves) {
        boolean success = shelvesService.add(shelves);
        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "更新货架")
    @PutMapping("/update")
    public Result<String> update(@RequestBody Shelves shelves) {
        boolean success = shelvesService.update(shelves);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "删除货架")
    @DeleteMapping("/{shelvesId}")
    public Result<String> delete(@PathVariable Integer shelvesId) {
        boolean success = shelvesService.delete(shelvesId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "根据位置查询货架")
    @GetMapping("/byLocation")
    public Result<List<Shelves>> findByLocation(@RequestParam String location) {
        List<Shelves> list = shelvesService.findByLocation(location);
        return Result.success(list);
    }
}
