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

import com.atlassian.confluence.security.delegate.PagePermissionsDelegate;

import java.util.List;
import java.util.Iterator;

public class NewPagePermissionsDelegate extends PagePermissionsDelegate {
   protected UserAccessor userAccessor;

   public boolean canView(User user, Page target) {
      return this.hasSpaceLevelPermission("VIEWSPACE", user, target) && this.hasContentLevelViewPermission(user, target);
   }

   public boolean canEdit(User user, Page target) {
      return this.hasSpaceLevelPermission("VIEWSPACE", user, target) && this.hasSpaceLevelPermission("EDITSPACE", user, target) && this.hasContentLevelEditPermission(user, target);
   }

   // @Override
   protected boolean hasSpaceLevelPermission(String permission, com.atlassian.user.User user, Object target) {
      return Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, permission);
   }

   // @Override
   private boolean hasContentLevelViewPermission(com.atlassian.user.User user, Object target) {
      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.VIEW_PERMISSION);
   }

   // @Override
   private boolean hasContentLevelEditPermission(com.atlassian.user.User user, Object target) {
      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.EDIT_PERMISSION);
   }

   public void setUserAccessor(UserAccessor userAccessor) {
      this.userAccessor = userAccessor;
   }
}