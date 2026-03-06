package com.upc.book.controller;

import com.github.pagehelper.PageInfo;
import com.upc.book.service.BookService;
import com.upc.common.entity.Book;
import com.upc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书管理控制器
 */
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
@Tag(name = "图书管理", description = "图书管理接口")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "查询所有图书")
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
    public Result<String> addBook(@RequestBody Book book) {
        boolean flag = bookService.addBook(book);
        if (flag) {
            return Result.success("添加成功");
        } else {
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "更新图书")
    @PostMapping("/updateBook")
    public Result<String> updateBook(@RequestBody Book book) {
        boolean flag = bookService.updateBook(book);
        if (flag) {
            return Result.success("更新成功");
        } else {
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "删除图书")
    @Parameter(name = "title", description = "书名", required = true)
    @DeleteMapping("/{title}")
    public Result<String> deleteBook(@PathVariable String title) {
        boolean flag = bookService.deleteBook(title);
        if (flag) {
            return Result.success("删除成功");
        } else {
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "根据关键词模糊搜索图书")
    @GetMapping("/search")
    public Result<PageInfo<Book>> searchBooks(
            @RequestParam String keyword,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        PageInfo<Book> bookList = bookService.searchBooksByKeyword(keyword, pageNum, pageSize);
        return Result.success(bookList);
    }

    @Operation(summary = "根据书名查询图书")
    @Parameter(name = "title", description = "书名", required = true)
    @GetMapping("/findByTitle/{title}")
    public Result<Book> findByTitle(@PathVariable String title) {
        Book book = bookService.findByTitle(title);
        if (book != null) {
            return Result.success(book);
        } else {
            return Result.fail(404, "图书不存在");
        }
    }

    @Operation(summary = "减少指定书籍的可借数量")
    @PutMapping("/decreaseAvaBooks/{title}")
    public Result<String> decreaseAvaBooks(@PathVariable String title) {
        boolean flag = bookService.decreaseAvaBooks(title);
        if (flag) {
            return Result.success("操作成功");
        } else {
            return Result.fail(500, "操作失败");
        }
    }

    @Operation(summary = "增加指定书籍的可借数量")
    @PutMapping("/increaseAvaBooks/{title}")
    public Result<String> increaseAvaBooks(@PathVariable String title) {
        boolean flag = bookService.increaseAvaBooks(title);
        if (flag) {
            return Result.success("操作成功");
        } else {
            return Result.fail(500, "操作失败");
        }
    }

    @Operation(summary = "验证图书是否存在")
    @GetMapping("/exists/{title}")
    public Result<Boolean> checkBookExists(@PathVariable String title) {
        boolean exists = bookService.checkBookExists(title);
        return Result.success(exists);
    }

    @Operation(summary = "获取所有图书分类")
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        List<String> categories = bookService.getAllCategories();
        return Result.success(categories);
    }

    @Operation(summary = "根据分类查询图书")
    @GetMapping("/byCategory/{category}")
    public Result<PageInfo<Book>> getBooksByCategory(
            @PathVariable String category,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        PageInfo<Book> bookList = bookService.getBooksByCategory(category, pageNum, pageSize);
        return Result.success(bookList);
    }
}
