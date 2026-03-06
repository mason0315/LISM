package com.upc.bookmanagement.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.BorrowRecord;
import com.upc.bookmanagement.mapper.BorrowRecordMapper;
import com.upc.bookmanagement.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowRecordServiceImpl implements BorrowRecordService {
    private final BorrowRecordMapper borrowRecordMapper;

    @Override
    public PageInfo<BorrowRecord> findAll(String title,Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findAll(title, userId);
        return new PageInfo<>(records);
    }
    @Override 
    public PageInfo<BorrowRecord> findById( Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findById(userId);
        return new PageInfo<>(records);
    }

    @Override
    public boolean addRecord(BorrowRecord borrowRecord){
        return borrowRecordMapper.insert(borrowRecord) > 0;
    }

    @Override
    public boolean deleteRecord(Integer recordId){
        return borrowRecordMapper.deleteById(recordId) > 0;
    }
    @Override
    public boolean updateRecord(BorrowRecord borrowRecord){
        return borrowRecordMapper.updateById(borrowRecord) > 0;
    }

    @Override
    public PageInfo<BorrowRecord> findA(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findA();
        return new PageInfo<>(records);
    }

    @Override
    public PageInfo<BorrowRecord> findBorrowed(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findBorrowed();
        return new PageInfo<>(records);
    }
}
