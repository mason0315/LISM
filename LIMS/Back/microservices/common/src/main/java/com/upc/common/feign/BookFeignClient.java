package com.upc.common.feign;

import com.upc.common.entity.Book;
import com.upc.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 图书服务Feign客户端
 */
@FeignClient(name = "book-service", path = "/book")
public interface BookFeignClient {

    /**
     * 根据书名查询图书
     */
    @GetMapping("/findByTitle/{title}")
    Result<Book> getBookByTitle(@PathVariable("title") String title);

    /**
     * 验证图书是否存在
     */
    @GetMapping("/exists/{title}")
    Result<Boolean> checkBookExists(@PathVariable("title") String title);

    /**
     * 减少可借数量
     */
    @PutMapping("/decreaseAvaBooks/{title}")
    Result<Boolean> decreaseAvaBooks(@PathVariable("title") String title);

    /**
     * 增加可借数量
     */
    @PutMapping("/increaseAvaBooks/{title}")
    Result<Boolean> increaseAvaBooks(@PathVariable("title") String title);
}
