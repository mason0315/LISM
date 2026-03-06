package com.upc.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书Mapper接口
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 根据关键词子串搜索图书
     *
     * @param substrings 关键词列表
     * @return 图书列表
     */
    List<Book> searchBySubstrings(@Param("substrings") List<String> substrings);

    /**
     * 减少可借数量
     *
     * @param title 书名
     * @return 影响行数
     */
    @Update("UPDATE book_info SET ava_books = ava_books - 1 WHERE title = #{title} AND ava_books > 0")
    int decreaseAvaBooksByTitle(@Param("title") String title);

    /**
     * 增加可借数量
     *
     * @param title 书名
     * @return 影响行数
     */
    @Update("UPDATE book_info SET ava_books = ava_books + 1 WHERE title = #{title}")
    int increaseAvaBooksByTitle(@Param("title") String title);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    @Select("SELECT DISTINCT category FROM book_info WHERE category IS NOT NULL AND category != ''")
    List<String> selectAllCategories();

    /**
     * 根据分类查询图书
     *
     * @param category 分类
     * @return 图书列表
     */
    @Select("SELECT * FROM book_info WHERE category = #{category}")
    List<Book> selectByCategory(@Param("category") String category);
}
