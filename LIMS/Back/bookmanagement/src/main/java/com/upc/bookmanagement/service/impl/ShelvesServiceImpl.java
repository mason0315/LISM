package com.upc.bookmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Shelves;
import com.upc.bookmanagement.mapper.ShelvesMapper;
import com.upc.bookmanagement.service.ShelvesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelvesServiceImpl implements ShelvesService {

    private final ShelvesMapper shelvesMapper;

    @Override
    public PageInfo<Shelves> getAllShelves(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shelves> shelves = shelvesMapper.getAllShelves();
        return PageInfo.of(shelves);
    }

    @Override
    public PageInfo<Shelves> getShelvesById(Integer shelveId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shelves> shelves = shelvesMapper.getShelvesById(shelveId);
        return PageInfo.of(shelves);
    }


    @Override
    public PageInfo<Shelves> getShelvesByTitle(String title, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shelves> shelves = shelvesMapper.getShelvesByTitle(title);
        return PageInfo.of(shelves);
    }

    @Override
    public boolean addShelves(Shelves shelves) {
        return shelvesMapper.insert(shelves) > 0;
    }

    @Override
    public boolean deleteShelves(Integer shelveId) {
        return shelvesMapper.deleteById(shelveId) > 0;
    }

    @Override
    public boolean deleteShelvesByTitle(String title) {
        return shelvesMapper.deleteShelvesByTitle(title) > 0;
    }

    @Override
    public boolean updateShelves(Shelves shelves) {
        return shelvesMapper.updateById(shelves) > 0;
    }

    @Override
    public List<String> getBooksByShelveId(Integer shelveId) {
        return shelvesMapper.getBooksByShelveId(shelveId);
    }

    @Override
    public boolean addBookToShelve(Integer shelveId, String title, String category) {
        return shelvesMapper.addBookToShelve(shelveId, title, category) > 0;
    }

    @Override
    public boolean removeBookFromShelve(Integer shelveId, String title) {
        return shelvesMapper.removeBookFromShelve(shelveId, title) > 0;
    }

    @Override
    public List<String> getAllCategories() {
        return shelvesMapper.getAllCategories();
    }
}
