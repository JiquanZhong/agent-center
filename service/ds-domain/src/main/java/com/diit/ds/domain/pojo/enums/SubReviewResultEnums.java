package com.diit.ds.domain.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjxbz
 */
@Getter
@AllArgsConstructor
public enum SubReviewResultEnums {

    REVIEWING("审核中"),
    PASS("审核通过"),
    UNPASS("审核未通过");

    private final String value;

}
