package com.upc.borrow.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.borrow.mapper.BorrowRecordMapper;
import com.upc.borrow.service.BorrowRecordService;
import com.upc.common.entity.BorrowRecord;
import com.upc.common.feign.BookFeignClient;
import com.upc.common.feign.UserFeignClient;
import com.upc.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 借阅记录服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRecordServiceImpl implements BorrowRecordService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookFeignClient bookFeignClient;
    private final UserFeignClient userFeignClient;

    @Override
    public PageInfo<BorrowRecord> findAll(String title, Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findAll(title, userId);
        // 填充关联信息
        records.forEach(this::fillRecordInfo);
        return new PageInfo<>(records);
    }

    @Override
    public PageInfo<BorrowRecord> findByUserId(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowRecordMapper.findByUserId(userId);
        // 填充关联信息
        records.forEach(this::fillRecordInfo);
        return new PageInfo<>(records);
    }

    @Override
    @Transactional
    public boolean addRecord(BorrowRecord borrowRecord) {
        // 验证用户是否存在
        Result<Boolean> userExists = userFeignClient.checkUserExists(borrowRecord.getUserId());
        if (userExists.getData() == null || !userExists.getData()) {
            log.warn("添加借阅记录失败：用户不存在 - {}", borrowRecord.getUserId());
            return false;
        }

        // 验证图书是否存在
        Result<Boolean> bookExists = bookFeignClient.checkBookExists(borrowRecord.getTitle());
        if (bookExists.getData() == null || !bookExists.getData()) {
            log.warn("添加借阅记录失败：图书不存在 - {}", borrowRecord.getTitle());
            return false;
        }

        // 减少图书可借数量
        Result<Boolean> decreaseResult = bookFeignClient.decreaseAvaBooks(borrowRecord.getTitle());
        if (decreaseResult.getData() == null || !decreaseResult.getData()) {
            log.warn("添加借阅记录失败：图书库存不足 - {}", borrowRecord.getTitle());
            return false;
        }

        // 设置借阅日期和应还日期（默认30天）
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String dueDate = LocalDate.now().plusDays(30).toString();

        borrowRecord.setBorrowDate(now);
        borrowRecord.setDueDate(dueDate);
        borrowRecord.setStatus(0); // 0-借阅中
        borrowRecord.setRenewCount(0);

        int result = borrowRecordMapper.insert(borrowRecord);
        return result > 0;
    }

    @Override
    public boolean deleteRecord(Integer recordId) {
        int result = borrowRecordMapper.deleteById(recordId);
        return result > 0;
    }

    @Override
    public boolean updateRecord(BorrowRecord borrowRecord) {
        int result = borrowRecordMapper.updateById(borrowRecord);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean returnBook(Integer recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            log.warn("归还图书失败：记录不存在 - {}", recordId);
            return false;
        }

        if (record.getStatus() == 1) {
            log.warn("归还图书失败：图书已归还 - {}", recordId);
            return false;
        }

        // 增加图书可借数量
        Result<Boolean> increaseResult = bookFeignClient.increaseAvaBooks(record.getTitle());
        if (increaseResult.getData() == null || !increaseResult.getData()) {
            log.warn("归还图书失败：更新图书库存失败 - {}", record.getTitle());
            return false;
        }

        // 更新借阅记录
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        record.setReturnDate(now);
        record.setStatus(1); // 1-已归还

        int result = borrowRecordMapper.updateById(record);
        return result > 0;
    }

    @Override
    public boolean renewBook(Integer recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            log.warn("续借图书失败：记录不存在 - {}", recordId);
            return false;
        }

        if (record.getStatus() != 0) {
            log.warn("续借图书失败：图书不在借阅中 - {}", recordId);
            return false;
        }

        // 最多续借2次
        if (record.getRenewCount() >= 2) {
            log.warn("续借图书失败：已达到最大续借次数 - {}", recordId);
            return false;
        }

        // 延长应还日期15天
        LocalDate dueDate = LocalDate.parse(record.getDueDate());
        String newDueDate = dueDate.plusDays(15).toString();

        record.setDueDate(newDueDate);
        record.setRenewCount(record.getRenewCount() + 1);

        int result = borrowRecordMapper.updateById(record);
        return result > 0;
    }

    @Override
    public BorrowRecord getBorrowRecordById(Integer recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record != null) {
            fillRecordInfo(record);
        }
        return record;
    }

    @Override
    public Integer getUserBorrowCount(Integer userId) {
        return borrowRecordMapper.countUserBorrowing(userId);
    }

    /**
     * 填充借阅记录的关联信息
     */
    private void fillRecordInfo(BorrowRecord record) {
        // 获取用户信息
        if (record.getUserId() != null) {
            try {
                Result<com.upc.common.entity.Users> userResult = userFeignClient.getUserById(record.getUserId());
                if (userResult.getData() != null) {
                    record.setUsername(userResult.getData().getUsername());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: {}", e.getMessage());
            }
        }

        // 获取图书信息
        if (record.getTitle() != null) {
            try {
                Result<com.upc.common.entity.Book> bookResult = bookFeignClient.getBookByTitle(record.getTitle());
                if (bookResult.getData() != null) {
                    record.setBookInfo(bookResult.getData());
                }
            } catch (Exception e) {
                log.warn("获取图书信息失败: {}", e.getMessage());
            }
        }
    }
}
