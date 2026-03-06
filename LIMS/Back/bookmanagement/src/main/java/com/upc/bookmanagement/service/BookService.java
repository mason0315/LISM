package com.upc.bookmanagement.service;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Book;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */
public interface BookService {
    /**
     * 查询所有图书
     */
    PageInfo<Book> findAllBook(Integer pageNum, Integer pageSize, String orderByClause);


    /**
     * 添加图书
     */
    boolean addBook(Book book);

    /**
     * 修改图书
     */
    boolean updateBook(Book book);

    /**
     * 删除图书
     */
    boolean deleteBook(String title);

    PageInfo<Book> searchBooksByKeyword(String keyword, Integer pageNum, Integer pageSize);

    Book findByTitle(String title);
    /**
     * 减少指定书籍的可借数量
     */
    boolean decreaseAvaBooks(String title);
    PageInfo<Book> findAllBook();
}
