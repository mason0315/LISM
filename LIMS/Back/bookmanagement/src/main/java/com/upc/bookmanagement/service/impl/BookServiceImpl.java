package com.upc.bookmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Book;
import com.upc.bookmanagement.mapper.BookMapper;
import com.upc.bookmanagement.service.BookService;
import com.upc.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;

    @Override
    public PageInfo<Book> findAllBook(Integer pageNum, Integer pageSize, String orderByClause) {
        PageHelper.startPage(pageNum, pageSize);
        List<Book> bookList = bookMapper.findAllBookOrderByLoveDesc();
        return PageInfo.of(bookList);
    }

    @Override
    public boolean addBook(Book book) {
        return bookMapper.insert(book) > 0;
    }

    @Override
    public boolean updateBook(Book book) {
        return bookMapper.updateById(book) > 0;
    }

    @Override
    public boolean deleteBook(String title) {
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("title", title);
        return bookMapper.delete(wrapper) > 0;
    }

    @Override
    public PageInfo<Book> searchBooksByKeyword(String keyword, Integer pageNum, Integer pageSize) {
        List<String> substrings = SearchUtils.generateSubstrings(keyword, 2);

        if (substrings.isEmpty()) {
            return new PageInfo<>();
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Book> books = bookMapper.searchBySubstrings(substrings);
        return PageInfo.of(books);
    }

    @Override
    public Book findByTitle(String title) {
        Book book = bookMapper.findByTitle(title);
        return book;
    }

    @Override
    public boolean decreaseAvaBooks(String title) {
        int rowsAffected = bookMapper.decreaseAvaBooksByTitle(title);
        return rowsAffected > 0;
    }
    @Override
    public PageInfo<Book> findAllBook() {
        List<Book> bookList = bookMapper.findAllBook();
        return PageInfo.of(bookList);
    }
}
