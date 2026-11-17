package com.mx.model.dto;

import lombok.Data;

/** 分页入参。页码从 1 开始，每页默认 10 条、最多 50 条。 */
@Data
public class PageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;
}
