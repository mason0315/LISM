package com.upc.bookmanagement.service;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Shelves;

import java.util.List;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-18
 */
public interface ShelvesService {

    PageInfo<Shelves> getAllShelves(Integer pageNum, Integer pageSize);

    PageInfo<Shelves> getShelvesById(Integer shelveId,Integer pageNum, Integer pageSize);

    PageInfo<Shelves> getShelvesByTitle(String title, Integer pageNum, Integer pageSize);

    boolean addShelves(Shelves shelves);

    boolean deleteShelves(Integer shelveId);

    boolean deleteShelvesByTitle(String title);

    boolean updateShelves(Shelves shelves);
    List<String> getBooksByShelveId(Integer shelveId);
    boolean addBookToShelve(Integer shelveId, String title, String category);
    boolean removeBookFromShelve(Integer shelveId, String title);
    List<String> getAllCategories();
}
