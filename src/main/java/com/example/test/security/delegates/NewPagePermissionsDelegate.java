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

   @Override
   public boolean canView(User user, Page target) {
      return this.hasSpaceLevelPermission("VIEWSPACE", user, target) && this.hasContentLevelViewPermission(user, target);
   }

   @Override
   public boolean canEdit(User user, Page target) {
      return this.hasSpaceLevelPermission("VIEWSPACE", user, target) && this.hasSpaceLevelPermission("EDITSPACE", user, target) && this.hasContentLevelEditPermission(user, target);
   }

   @Override
   public boolean canRemove(User user, Page target) {
      if (!hasSpaceLevelPermission("REMOVEPAGE", user, target) && 
         !canRemoveOwn(target, user))
         return false; 
      if (!canView(user, target))
         return false; 
      return (hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target) || hasContentLevelEditPermission(user, target));
   }

   @Override
   public boolean canMove(User user, Page source, Object target, String movePoint) {
      boolean canEdit = canEdit(user, source);
      Page targetPage = (Page)target;
      boolean canCreateInSpace = canCreate(user, targetPage.getSpace());
      if (canEdit && canCreateInSpace) {
         if (!source.isDraft() && !targetPage.getSpace().equals(source.getSpace()) && 
            !canRemoveHierarchy(user, source))
            return false; 
         if ("below".equals(movePoint) || "above".equals(movePoint)) {
            if (targetPage.getParent() != null)
               return hasContentLevelViewPermission(user, targetPage.getParent()); 
            return true;
         } 
         if ("append".equals(movePoint))
            return hasContentLevelViewPermission(user, targetPage); 
      } 
      return false;
   }

   @Override
   public boolean canRemoveHierarchy(User user, Page target) {
      if (!canView(user, target))
         return false; 
      if (!hasSpaceLevelPermission("REMOVEPAGE", user, target)) {
         if (!canRemoveOwn(target, user))
            return false; 
         for (Page page : target.getChildren()) {
            if (!canRemoveOwn(page, user) || !canRemoveHierarchy(user, page))
               return false; 
         } 
      } 
      return (hasSpaceLevelPermission("SETSPACEPERMISSIONS", user, target) || hasContentLevelEditPermission(user, target));
   }

   @Override
   public boolean canExport(User user, Page target) {
      return canView(user, target);
   }

   private boolean canRemoveOwn(Object target, User user) {
      Page page = (Page)target;
      boolean isCreator = (page != null && page.getCreator() != null && user != null && user.getName() != null && user.getName().equals(page.getCreator().getName()));
      return (isCreator && hasSpaceLevelPermission("REMOVEOWNCONTENT", user, target));
   }

   protected boolean hasSpaceLevelPermission(String permission, com.atlassian.user.User user, Object target) {
      return Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, permission);
   }

   private boolean hasContentLevelViewPermission(com.atlassian.user.User user, Object target) {
      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.VIEW_PERMISSION);
   }

   private boolean hasContentLevelEditPermission(com.atlassian.user.User user, Object target) {
      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.EDIT_PERMISSION);
   }

   public void setUserAccessor(UserAccessor userAccessor) {
      this.userAccessor = userAccessor;
   }
}