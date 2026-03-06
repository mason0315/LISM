package com.upc.shelves.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Shelves;

import java.util.List;

public interface ShelvesService {
    List<Shelves> findAll();
    PageInfo<Shelves> findByPage(int pageNum, int pageSize);
    Shelves findById(Integer shelvesId);
    boolean add(Shelves shelves);
    boolean update(Shelves shelves);
    boolean delete(Integer shelvesId);
    List<Shelves> findByLocation(String location);
}
