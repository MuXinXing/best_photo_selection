package com.mx.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 照片分页结果，附带全库状态数量，供筛选栏使用。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhotoPageVO extends PageVO<PhotoVO> {

    /** 当前片仓照片总数，不受状态筛选影响。 */
    private long libraryTotal;
    private long selected;
    private long rejected;
    private long unmarked;
}
