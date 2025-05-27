package com.diit.ds.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjxbz
 */

@Getter
@AllArgsConstructor
public enum OrderConditionEnums {
    CREATE_DATE_ASC("create_date",true),
    CREATE_DATE_DESC("create_date",false),
    FAVORITE_COUNT_ASC("favorite_count",true),
    FAVORITE_COUNT_DESC("favorite_count",false),

    ;
    private final String value;

    private boolean code;

}
