package com.upc.bookmanagement.service;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.BorrowRecord;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */
public interface BorrowRecordService {


    PageInfo<BorrowRecord> findAll(String title,Integer userId, int pageNum, int pageSize);

    PageInfo<BorrowRecord> findById( Integer userId, int pageNum, int pageSize);


    boolean addRecord(BorrowRecord borrowRecord);

    boolean deleteRecord(Integer recordId);
    boolean updateRecord(BorrowRecord borrowRecord);
    PageInfo<BorrowRecord> findA(int pageNum, int pageSize);
    PageInfo<BorrowRecord> findBorrowed(int pageNum, int pageSize);
}
