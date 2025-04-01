package com.example.test.security.delegates;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.templates.PageTemplate;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.security.delegate.PageTemplatePermissionsDelegate;

import java.util.List;
import java.util.Iterator;

public class NewPageTemplatePermissionsDelegate extends PageTemplatePermissionsDelegate {
    protected UserAccessor userAccessor;

    public boolean canView(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("VIEWSPACE", user, target);
    }

    public boolean canEdit(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target);
    }

    public boolean canSetPermissions(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target);
    }

    public boolean canRemove(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target);
    }

    public boolean canExport(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("EXPORTSPACE", user, target);
    }

    public boolean canAdminister(User user, PageTemplate target) {
        return this.hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target);
    }

    public boolean canCreate(User user, Object container) {
        return this.hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, container);
    }

    protected boolean hasSpaceLevelPermission(String permission, com.atlassian.user.User user, Object target) {
        return Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, permission);
    }

    public void setUserAccessor(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }
}