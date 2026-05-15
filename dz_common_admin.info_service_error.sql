 -- dz_common_admin.info_service_error definition

CREATE TABLE `info_service_error` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `platformid` int(11) NOT NULL DEFAULT '0' COMMENT '平台ID，0表示无关联平台',
  `channel` varchar(50) NOT NULL COMMENT '渠道标识',
  `userid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID，0表示无关联用户',
  `level` tinyint(4) NOT NULL DEFAULT '1' COMMENT '日志级别(ERROR|1,FATAL|2)',
  `servicename` varchar(100) NOT NULL COMMENT '服务名称',
  `serviceip` varchar(40) NOT NULL COMMENT '服务ip地址',
  `modulename` varchar(100) NOT NULL COMMENT '功能模块',
  `resourceid` varchar(50) NOT NULL COMMENT '错误资源唯一标识，用于TG推送判断相同错误信息',
  `errorcode` varchar(50) NOT NULL COMMENT '错误码',
  `shortdesc` varchar(1000) NOT NULL COMMENT '错误简短描述',
  `content` text NOT NULL COMMENT '错误详细内容',
  `notifystatus` tinyint(4) NOT NULL DEFAULT '0' COMMENT '通知状态(0=未通知,1=已通知)',
  `ym` int(11) NOT NULL DEFAULT '0' COMMENT '索引识别',
  `reporttime` datetime NOT NULL COMMENT '上报时间',
  `createtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reporttime` (`reporttime`),
  KEY `idx_createtime` (`createtime`),
  KEY `idx_servicename` (`servicename`),
  KEY `idx_level` (`level`),
  KEY `idx_notifystatus` (`notifystatus`),
  KEY `ym_index` (`ym`),
  KEY `redid_index` (`resourceid`)
) ENGINE=InnoDB AUTO_INCREMENT=779872 DEFAULT CHARSET=utf8mb4 COMMENT='游戏服务异常信息表';