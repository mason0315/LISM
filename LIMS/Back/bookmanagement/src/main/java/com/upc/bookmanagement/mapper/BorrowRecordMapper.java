package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    List<BorrowRecord> findAll(String title,Integer userId);

    List<BorrowRecord> findById(Integer userId);

    List<BorrowRecord> findA();

    List<BorrowRecord> findBorrowed();
}

