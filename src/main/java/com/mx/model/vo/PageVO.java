package com.mx.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 分页出参。total 是当前筛选条件下的总条数。 */
@Data
public class PageVO<T> {

    private List<T> list = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;
}
