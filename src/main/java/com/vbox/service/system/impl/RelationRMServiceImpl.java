package com.vbox.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.persistent.entity.RelationRoleMenu;
import com.vbox.persistent.repo.RelationRMMapper;
import com.vbox.service.system.RelationRMService;
import org.springframework.stereotype.Service;

@Service
public class RelationRMServiceImpl extends ServiceImpl<RelationRMMapper, RelationRoleMenu> implements RelationRMService {


}
