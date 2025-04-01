package com.example.test.security.delegates;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;

import com.atlassian.confluence.pages.Page;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.security.delegate.SpacePermissionsDelegate;

import java.util.List;
import java.util.Iterator;

public class NewSpacePermissionsDelegate extends SpacePermissionsDelegate{
    protected UserAccessor userAccessor;

    public boolean canView(User user, Space target) {
        return Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "VIEWSPACE");
    }

    public boolean canEdit(User user, Space target) {
        return this.canView(user, target) && Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "SETSPACEPERMISSIONS");
    }

    public boolean canSetPermissions(User user, Space target) {
        return this.canView(user, target) && Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "SETSPACEPERMISSIONS");
    }

    public boolean canRemove(User user, Space target) {
        return this.canView(user, target) && Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "SETSPACEPERMISSIONS");
    }

    public boolean canExport(User user, Space target) {
        return this.canView(user, target) && Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "EXPORTSPACE");
    }

    public boolean canAdminister(User user, Space target) {
        return this.canView(user, target) && Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, "SETSPACEPERMISSIONS");
    }

    public void setUserAccessor(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
     }
}
