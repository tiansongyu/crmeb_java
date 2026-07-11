package com.zbkj.service.service.impl;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.user.UserGroup;
import com.zbkj.common.model.user.UserTag;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.UserSearchRequest;
import com.zbkj.common.response.UserResponse;
import com.zbkj.service.dao.UserDao;
import com.zbkj.service.service.UserGroupService;
import com.zbkj.service.service.UserTagService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

    private UserServiceImpl service;

    @Mock
    private UserDao userDao;

    @Mock
    private UserGroupService userGroupService;

    @Mock
    private UserTagService userTagService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userDao", userDao);
        ReflectionTestUtils.setField(service, "userGroupService", userGroupService);
        ReflectionTestUtils.setField(service, "userTagService", userTagService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void adminListLoadsRelatedNamesInBatches() {
        User first = user(1, "1", "2", 9);
        User second = user(2, "1", "2", 9);
        User spread = new User();
        spread.setUid(9);
        spread.setNickname("推荐人");
        UserGroup group = new UserGroup();
        group.setId(1);
        group.setGroupName("VIP");
        UserTag tag = new UserTag();
        tag.setId(2);
        tag.setName("活跃");

        when(userDao.findAdminList(any(Map.class))).thenReturn(Arrays.asList(first, second));
        when(userGroupService.listByIds(any(Collection.class))).thenReturn(Collections.singletonList(group));
        when(userTagService.listByIds(any(Collection.class))).thenReturn(Collections.singletonList(tag));
        when(userDao.selectBatchIds(any(Collection.class))).thenReturn(Collections.singletonList(spread));

        PageParamRequest page = new PageParamRequest();
        page.setPage(1);
        page.setLimit(20);
        PageInfo<UserResponse> result = service.getList(new UserSearchRequest(), page);

        List<UserResponse> users = result.getList();
        assertEquals(2, users.size());
        assertEquals("VIP", users.get(0).getGroupName());
        assertEquals("活跃", users.get(0).getTagName());
        assertEquals("推荐人", users.get(0).getSpreadNickname());
        verify(userDao).selectBatchIds(any(Collection.class));
        verify(userDao, never()).selectById(any());
        verify(userGroupService, never()).getGroupNameInId(any());
        verify(userTagService, never()).getGroupNameInId(any());
    }

    private User user(int uid, String groupId, String tagId, int spreadUid) {
        User user = new User();
        user.setUid(uid);
        user.setGroupId(groupId);
        user.setTagId(tagId);
        user.setSpreadUid(spreadUid);
        user.setPhone("13800138000");
        return user;
    }
}
