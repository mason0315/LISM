package com.upc.borrow.controller;

import com.github.pagehelper.PageInfo;
import com.upc.borrow.service.BorrowRecordService;
import com.upc.common.entity.BorrowRecord;
import com.upc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅记录控制器
 */
@RestController
@RequestMapping("/borrowrecord")
@RequiredArgsConstructor
@Tag(name = "借阅信息管理", description = "借阅记录管理接口")
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;

    @Operation(summary = "查询所有借阅信息（分页）")
    @GetMapping("/allrecord")
    public Result<PageInfo<BorrowRecord>> findAll(
            @RequestParam String title,
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findAll(title, userId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @Operation(summary = "根据用户ID查询借阅信息（分页）")
    @GetMapping("/myrecord")
    public Result<PageInfo<BorrowRecord>> findByUserId(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageInfo<BorrowRecord> pageInfo = borrowRecordService.findByUserId(userId, pageNum, pageSize);
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

    @Operation(summary = "归还图书")
    @PutMapping("/return/{recordId}")
    public Result<String> returnBook(@PathVariable Integer recordId) {
        if (borrowRecordService.returnBook(recordId)) {
            return Result.success("归还成功");
        } else {
            return Result.fail(500, "归还失败");
        }
    }

    @Operation(summary = "续借图书")
    @PutMapping("/renew/{recordId}")
    public Result<String> renewBook(@PathVariable Integer recordId) {
        if (borrowRecordService.renewBook(recordId)) {
            return Result.success("续借成功");
        } else {
            return Result.fail(500, "续借失败");
        }
    }

    @Operation(summary = "查询借阅记录详情")
    @GetMapping("/{recordId}")
    public Result<BorrowRecord> getBorrowRecordById(@PathVariable Integer recordId) {
        BorrowRecord record = borrowRecordService.getBorrowRecordById(recordId);
        if (record != null) {
            return Result.success(record);
        } else {
            return Result.fail(404, "记录不存在");
        }
    }

    @Operation(summary = "查询用户当前借阅数量")
    @GetMapping("/count/{userId}")
    public Result<Integer> getUserBorrowCount(@PathVariable Integer userId) {
        Integer count = borrowRecordService.getUserBorrowCount(userId);
        return Result.success(count);
    }
}
