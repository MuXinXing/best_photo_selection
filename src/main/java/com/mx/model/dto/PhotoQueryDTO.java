package com.mx.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 照片列表查询入参。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhotoQueryDTO extends PageQueryDTO {

    private String status = "all";
    private String sort = "name";
    private String order = "asc";
}
