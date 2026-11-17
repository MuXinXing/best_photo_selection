package com.mx.model.vo;

import lombok.Data;

/** 片仓照片数量统计。 */
@Data
public class StatisticsVO {

    private long total;
    private long selected;
    private long rejected;
    private long unmarked;
}
