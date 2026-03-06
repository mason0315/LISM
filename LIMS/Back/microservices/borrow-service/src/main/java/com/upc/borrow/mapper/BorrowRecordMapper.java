package com.upc.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 借阅记录Mapper接口
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 查询所有借阅记录
     *
     * @param title  书名
     * @param userId 用户ID
     * @return 借阅记录列表
     */
    List<BorrowRecord> findAll(@Param("title") String title, @Param("userId") Integer userId);

    /**
     * 根据用户ID查询借阅记录
     *
     * @param userId 用户ID
     * @return 借阅记录列表
     */
    @Select("SELECT * FROM borrow_record WHERE user_id = #{userId} ORDER BY borrow_date DESC")
    List<BorrowRecord> findByUserId(@Param("userId") Integer userId);

    /**
     * 统计用户当前借阅数量
     *
     * @param userId 用户ID
     * @return 借阅数量
     */
    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND status = 0")
    Integer countUserBorrowing(@Param("userId") Integer userId);
}
