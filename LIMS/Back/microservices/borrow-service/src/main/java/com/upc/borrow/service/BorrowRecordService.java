package com.upc.borrow.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.BorrowRecord;

/**
 * 借阅记录服务接口
 */
public interface BorrowRecordService {

    /**
     * 查询所有借阅记录
     *
     * @param title    书名
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 借阅记录分页列表
     */
    PageInfo<BorrowRecord> findAll(String title, Integer userId, int pageNum, int pageSize);

    /**
     * 根据用户ID查询借阅记录
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 借阅记录分页列表
     */
    PageInfo<BorrowRecord> findByUserId(Integer userId, int pageNum, int pageSize);

    /**
     * 添加借阅记录
     *
     * @param borrowRecord 借阅记录
     * @return 是否成功
     */
    boolean addRecord(BorrowRecord borrowRecord);

    /**
     * 删除借阅记录
     *
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean deleteRecord(Integer recordId);

    /**
     * 更新借阅记录
     *
     * @param borrowRecord 借阅记录
     * @return 是否成功
     */
    boolean updateRecord(BorrowRecord borrowRecord);

    /**
     * 归还图书
     *
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean returnBook(Integer recordId);

    /**
     * 续借图书
     *
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean renewBook(Integer recordId);

    /**
     * 根据ID查询借阅记录
     *
     * @param recordId 记录ID
     * @return 借阅记录
     */
    BorrowRecord getBorrowRecordById(Integer recordId);

    /**
     * 查询用户当前借阅数量
     *
     * @param userId 用户ID
     * @return 借阅数量
     */
    Integer getUserBorrowCount(Integer userId);
}
