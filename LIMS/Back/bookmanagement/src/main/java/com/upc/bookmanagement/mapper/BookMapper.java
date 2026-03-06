package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    List<Book> findAllBookOrderByLoveDesc();

    List<Book> searchBySubstrings(@Param("substrings") List<String> substrings);

    Book findByTitle(String title);

    int decreaseAvaBooksByTitle(String title);
    List<Book> findAllBook();
}
