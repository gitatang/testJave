
package com.md.poseidon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.md.poseidon.entity.BaseEntity;


import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 游戏服务异常信息表
 *
 * @author zwq
 * @date 2025-06-06 14:36:57
 */
@Data
@TableName("info_service_error")
public class InfoServiceErrorEntity extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 日志ID
	 */
	@TableId(value ="`id`", type = IdType.AUTO)
	private Long id;

	/**
	 * 平台ID，0表示无关联平台
	 */
	@TableField("`platformid`")
	private Integer platformid;

	/**
	 * 渠道标识
	 */
	@TableField("`channel`")
	private String channel;

	/**
	 * 用户ID，0表示无关联用户
	 */
	@TableField("`userid`")
	private Long userid;

	/**
	 * 日志级别(ERROR|1,FATAL|2)
	 */
	@TableField("`level`")
	private Integer level;

	/**
	 * 服务名称
	 */
	@TableField("`servicename`")
	private String servicename;

	/**
	 * 服务ip地址
	 */
	@TableField("`serviceip`")
	private String serviceip;

	/**
	 * 功能模块
	 */
	@TableField("`modulename`")
	private String modulename;

	/**
	 * 错误资源唯一标识，用于TG推送判断相同错误信息
	 */
	@TableField("`resourceid`")
	private String resourceid;

	/**
	 * 错误码
	 */
	@TableField("`errorcode`")
	private String errorcode;

	/**
	 * 错误简短描述
	 */
	@TableField("`shortdesc`")
	private String shortdesc;

	/**
	 * 错误详细内容
	 */
	@TableField("`content`")
	private String content;

	/**
	 * 通知状态(0=未通知,1=已通知)
	 */
	@TableField("`notifystatus`")
	private Integer notifystatus;

	/**
	 * 上报时间
	 */
	@TableField("`reporttime`")
	private Date reporttime;

	/**
	 * 创建时间
	 */
	@TableField("`createtime`")
	private Date createtime;

	/**
	 * 更新时间
	 */
	@TableField("`updatetime`")
	private Date updatetime;


	@TableField(exist = false)
	private Date start;

	@TableField(exist = false)
	private Date end;
}
