package com.upc.bookmanagement.controller;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Book;
import com.upc.bookmanagement.service.BookService;
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
@RequestMapping("/book")
@Tag(name = "图书管理",description = "图书管理接口")
public class BookController {

    private final BookService bookService;

    @GetMapping("/AllBook")
    public Result<PageInfo<Book>> findAllBook(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String order) {

        // 构建排序参数
        String orderByClause = "";
        if ("love".equals(orderBy) && "desc".equals(order)) {
            orderByClause = "love DESC";
        }

        PageInfo<Book> bookList = bookService.findAllBook(pageNum, pageSize, orderByClause);
        return Result.success(bookList);
    }



    @Operation(summary = "添加图书")
    @PostMapping("/addBook")
    public Result<String> addBook(@RequestBody Book book){
        boolean flag = bookService.addBook(book);
        if (flag){
            return Result.success("添加成功");
        }else {
            return Result.fail(500,"添加失败");
        }
    }

    @Operation(summary = "更新图书")
    @PostMapping("/updateBook")
    public Result<String> updateBook(@RequestBody Book book){
        boolean flag = bookService.updateBook(book);
        if (flag){
            return Result.success("更新成功");
        }else {
            return Result.fail(500,"更新失败");
        }
    }

    @Operation(summary = "删除图书")
    @Parameter(name = "title",description = "书名",required = true)
    @DeleteMapping("/{title}")
    public Result<String> deleteBook(@PathVariable String title){
        boolean flag = bookService.deleteBook(title);
        if (flag){
            return Result.success("删除成功");
        }else {
            return Result.fail(500,"删除失败");
        }
    }
    @Operation(summary = "根据关键词模糊搜索图书")
    @GetMapping("/search")
    public Result<PageInfo<Book>> searchBooks(@RequestParam String keyword, @RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        PageInfo<Book> bookList = bookService.searchBooksByKeyword(keyword, pageNum, pageSize);
        return Result.success(bookList);
    }

    @Operation(summary = "根据书名查询图书")
    @Parameter(name = "title",description = "书名",required = true)
    @GetMapping("/findByTitle/{title}")
    public Result<Book> findByTitle(@PathVariable String title) {
        Book book = bookService.findByTitle(title);
        if(book != null){
            return Result.success(book);
        }
        else{
            return Result.fail(500,"查询失败");
        }
    }
    @Operation(summary = "减少指定书籍的可借数量")
    @PutMapping("/decreaseAvaBooks/{title}")
    public Result<String> decreaseAvaBooks(@PathVariable String title) {
        boolean flag = bookService.decreaseAvaBooks(title);
        if (flag) {
            return Result.success("可借数量减少成功");
        } else {
            return Result.fail(500, "可借数量减少失败");
        }
    }
    @Operation(summary = "获取所有图书")
    @GetMapping("/getAllBooks")
    public Result<PageInfo<Book>> getAllBooks(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        PageInfo<Book> bookList = bookService.findAllBook();
        return Result.success(bookList);
    }
}

