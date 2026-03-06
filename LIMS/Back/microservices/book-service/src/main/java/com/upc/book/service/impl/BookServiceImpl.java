package com.upc.book.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.book.mapper.BookMapper;
import com.upc.book.service.BookService;
import com.upc.common.entity.Book;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 图书服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;

    @Override
    public PageInfo<Book> findAllBook(Integer pageNum, Integer pageSize, String orderByClause) {
        PageHelper.startPage(pageNum, pageSize);
        if (orderByClause != null && !orderByClause.isEmpty()) {
            PageHelper.orderBy(orderByClause);
        }
        List<Book> books = bookMapper.selectList(null);
        return new PageInfo<>(books);
    }

    @Override
    public boolean addBook(Book book) {
        // 检查图书是否已存在
        Book existBook = bookMapper.selectById(book.getTitle());
        if (existBook != null) {
            log.warn("添加图书失败：图书已存在 - {}", book.getTitle());
            return false;
        }

        // 初始化可借数量和库存
        if (book.getAvaBooks() == null) {
            book.setAvaBooks(book.getStock());
        }

        int result = bookMapper.insert(book);
        return result > 0;
    }

    @Override
    public boolean updateBook(Book book) {
        int result = bookMapper.updateById(book);
        return result > 0;
    }

    @Override
    public boolean deleteBook(String title) {
        int result = bookMapper.deleteById(title);
        return result > 0;
    }

    @Override
    public PageInfo<Book> searchBooksByKeyword(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<String> keywords = Arrays.asList(keyword.split("\\s+"));
        List<Book> books = bookMapper.searchBySubstrings(keywords);
        return new PageInfo<>(books);
    }

    @Override
    public Book findByTitle(String title) {
        return bookMapper.selectById(title);
    }

    @Override
    public boolean decreaseAvaBooks(String title) {
        int result = bookMapper.decreaseAvaBooksByTitle(title);
        return result > 0;
    }

    @Override
    public boolean increaseAvaBooks(String title) {
        int result = bookMapper.increaseAvaBooksByTitle(title);
        return result > 0;
    }

    @Override
    public boolean checkBookExists(String title) {
        Book book = bookMapper.selectById(title);
        return book != null;
    }

    @Override
    public List<String> getAllCategories() {
        return bookMapper.selectAllCategories();
    }

    @Override
    public PageInfo<Book> getBooksByCategory(String category, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Book> books = bookMapper.selectByCategory(category);
        return new PageInfo<>(books);
    }
}
