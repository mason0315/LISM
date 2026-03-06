package com.upc.book.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Book;

import java.util.List;

/**
 * 图书服务接口
 */
public interface BookService {

    /**
     * 查询所有图书
     *
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @param orderByClause 排序条件
     * @return 图书分页列表
     */
    PageInfo<Book> findAllBook(Integer pageNum, Integer pageSize, String orderByClause);

    /**
     * 添加图书
     *
     * @param book 图书信息
     * @return 是否成功
     */
    boolean addBook(Book book);

    /**
     * 更新图书
     *
     * @param book 图书信息
     * @return 是否成功
     */
    boolean updateBook(Book book);

    /**
     * 删除图书
     *
     * @param title 书名
     * @return 是否成功
     */
    boolean deleteBook(String title);

    /**
     * 根据关键词搜索图书
     *
     * @param keyword  关键词
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 图书分页列表
     */
    PageInfo<Book> searchBooksByKeyword(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 根据书名查询图书
     *
     * @param title 书名
     * @return 图书信息
     */
    Book findByTitle(String title);

    /**
     * 减少可借数量
     *
     * @param title 书名
     * @return 是否成功
     */
    boolean decreaseAvaBooks(String title);

    /**
     * 增加可借数量
     *
     * @param title 书名
     * @return 是否成功
     */
    boolean increaseAvaBooks(String title);

    /**
     * 验证图书是否存在
     *
     * @param title 书名
     * @return 是否存在
     */
    boolean checkBookExists(String title);

    /**
     * 获取所有图书分类
     *
     * @return 分类列表
     */
    List<String> getAllCategories();

    /**
     * 根据分类查询图书
     *
     * @param category 分类
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 图书分页列表
     */
    PageInfo<Book> getBooksByCategory(String category, Integer pageNum, Integer pageSize);
}
