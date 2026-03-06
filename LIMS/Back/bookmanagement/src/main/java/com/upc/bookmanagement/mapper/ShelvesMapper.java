package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.Shelves;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShelvesMapper extends BaseMapper<Shelves> {

    List<Shelves> getAllShelves();

    List<Shelves> getShelvesById(Integer shelveId);

    List<Shelves> getShelvesByTitle(String  title);

    int deleteShelvesByTitle(String title);

    List<String> getBooksByShelveId(Integer shelveId);
    int addBookToShelve(@org.apache.ibatis.annotations.Param("shelveId") Integer shelveId, @org.apache.ibatis.annotations.Param("title") String title, @org.apache.ibatis.annotations.Param("category") String category);
    int removeBookFromShelve(@org.apache.ibatis.annotations.Param("shelveId") Integer shelveId, @org.apache.ibatis.annotations.Param("title") String title);
    List<String> getAllCategories();
}
