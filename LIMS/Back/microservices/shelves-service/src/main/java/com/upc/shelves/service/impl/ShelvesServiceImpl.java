package com.upc.shelves.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Shelves;
import com.upc.shelves.mapper.ShelvesMapper;
import com.upc.shelves.service.ShelvesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelvesServiceImpl implements ShelvesService {

    private final ShelvesMapper shelvesMapper;

    @Override
    public List<Shelves> findAll() {
        return shelvesMapper.selectList(null);
    }

    @Override
    public PageInfo<Shelves> findByPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shelves> list = shelvesMapper.selectList(null);
        return new PageInfo<>(list);
    }

    @Override
    public Shelves findById(Integer shelvesId) {
        return shelvesMapper.selectById(shelvesId);
    }

    @Override
    public boolean add(Shelves shelves) {
        int result = shelvesMapper.insert(shelves);
        return result > 0;
    }

    @Override
    public boolean update(Shelves shelves) {
        int result = shelvesMapper.updateById(shelves);
        return result > 0;
    }

    @Override
    public boolean delete(Integer shelvesId) {
        int result = shelvesMapper.deleteById(shelvesId);
        return result > 0;
    }

    @Override
    public List<Shelves> findByLocation(String location) {
        return shelvesMapper.findByLocation(location);
    }
}
