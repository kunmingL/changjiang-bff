package com.changjiang.bff.entity;
import lombok.Data;
import java.util.List;

/**
 * 分页结果类
 * 主要职责：
 * 1. 封装分页查询结果
 * 2. 提供分页信息访问接口
 * 3. 支持分页数据序列化
 */
@Data
public class PageResult<T> {
    /** 
     * 当前页数据
     * 存储查询结果列表
     */
    private List<T> content;
    
    /** 
     * 总记录数
     * 用于分页计算
     */
    private long total;
    
    /** 
     * 当前页码
     * 从1开始
     */
    private int pageNum;
    
    /** 
     * 每页大小
     * 默认10条
     */
    private int pageSize;
    
    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> content, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setContent(content);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
} 