package com.upc.shelves.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Shelves;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShelvesMapper extends BaseMapper<Shelves> {

    @Select("SELECT * FROM shelves WHERE location = #{location}")
    List<Shelves> findByLocation(@Param("location") String location);
}
