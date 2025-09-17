package org.dinky.data.model.job;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author qilianchao@gyyx.cn
 * @date 2025-09-17
 * @Description
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobInstanceExt extends JobInstance {

    // 任务备注
    private String note;
}