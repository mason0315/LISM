package com.upc.bookmanagement.controller;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.BorrowRecord;
import com.upc.bookmanagement.service.BorrowRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/borrowrecord")
@Tag(name = "借阅信息管理")

public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;


    @Operation(summary = "查询所有借阅信息（分页）")
    @Parameter(name = "title", description = "书名", required = true)
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "pageNum", description = "当前页码", required = true)
    @Parameter(name = "pageSize", description = "每页大小", required = true)
    @GetMapping("/allrecord")
    public Result<PageInfo<BorrowRecord>> findAll(
            @RequestParam String title,
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findAll(title,userId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "根据用户ID查询借阅信息（分页）")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "pageNum", description = "当前页码", required = true)
    @Parameter(name = "pageSize", description = "每页大小", required = true)
    @GetMapping("/myrecord")
    public Result<PageInfo<BorrowRecord>> findByTitle(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findById( userId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "添加借阅信息")
    @PostMapping("/addRecord")
    public Result<String> addRecord(@RequestBody BorrowRecord borrowRecord) {
        if (borrowRecordService.addRecord(borrowRecord)) {
            return Result.success("添加成功");
        } else {
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "删除借阅信息")
    @DeleteMapping("/deleteRecord")
    public Result<String> deleteRecord(@RequestParam Integer recordId) {
        if (borrowRecordService.deleteRecord(recordId)) {
            return Result.success("删除成功");
        } else {
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "更新借阅信息")
    @PutMapping("/updateRecord")
    public Result<String> updateRecord(@RequestBody BorrowRecord borrowRecord) {
        if (borrowRecordService.updateRecord(borrowRecord)) {
            return Result.success("更新成功");
        } else {
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "查询")
    @GetMapping("/findA")
    public Result<PageInfo<BorrowRecord>> findA(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findA(pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "查询未归还")
    @GetMapping("/findBorrowed")
    public Result<PageInfo<BorrowRecord>> findBorrowed(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findBorrowed(pageNum, pageSize);
        return Result.success(pageInfo);
    }

}
