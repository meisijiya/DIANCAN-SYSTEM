package com.scaffold.modules.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.feedback.entity.UserFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}
