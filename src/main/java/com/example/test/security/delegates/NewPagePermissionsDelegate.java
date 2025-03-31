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
      // //Get the space from the target
      // Space space = this.getSpaceFrom(target);
      // List<SpacePermission> spacePermissions = space.getPermissions();
      // //Loop through all space level permissions of all permission types
      // for(SpacePermission spacePermission : spacePermissions) {
      //    if(spacePermission != null) {
      //       //If the permission is a group permission, and is of the same type as is being compared against, continue
      //       if(spacePermission.isGroupPermission() && spacePermission.getType().equals(permission)) {
      //          if(!this.userAccessor.getGroupNamesForUserName(user.getName()).contains(spacePermission.getGroup())) {
      //             //If the user does not belong to the group being checked in this iteration, immediately break and return false, no further checking needed
      //             return false;
      //          }
      //       }
      //    }
      // }
      // //If the end of the loop is reached, as in the return statement never fired, then the user must belong
      // //to all groups of that permission type attached to a space, allow the user access in this case
      // return true;

      return Utilities.isSpaceRestricted(this.getSpaceFrom(target), user, this.userAccessor, permission);
   }

   // @Override
   private boolean hasContentLevelViewPermission(com.atlassian.user.User user, Object target) {
      // //Loop through all content permissions
      // if(((ContentEntityObject)target).hasPermissions(ContentPermission.VIEW_PERMISSION)) {
      //    for(ContentPermission permission : ((ContentEntityObject)target).getContentPermissionSet(ContentPermission.VIEW_PERMISSION)) {
      //       if(permission.isGroupPermission()) {
      //          if(!this.userAccessor.getGroupNamesForUserName(user.getName()).contains(permission.getGroupName())) {
      //             //If the user does not belong to the group being checked in this iteration, immediately break and return false, no further checking needed
      //             System.out.println("========================================================================================================================");
      //             System.out.println("USER DOES NOT HAVE PERMISSION TO VIEW PAGE");
      //             System.out.println("========================================================================================================================");
      //             return false;
      //          }
      //       }
      //    }
      // }
      // //If the end of the loop is reached, as in the return statement never fired, then the user must belong
      // //to all groups of that permission type attached to a space, allow the user access in this case
      // return true;

      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.VIEW_PERMISSION);
   }

   // @Override
   private boolean hasContentLevelEditPermission(com.atlassian.user.User user, Object target) {
      // //Loop through all content permissions
      // if(((ContentEntityObject)target).hasPermissions(ContentPermission.EDIT_PERMISSION)) {
      //    for(ContentPermission permission : ((ContentEntityObject)target).getContentPermissionSet(ContentPermission.EDIT_PERMISSION)) {
      //       if(permission.isGroupPermission()) {
      //          if(!this.userAccessor.getGroupNamesForUserName(user.getName()).contains(permission.getGroupName())) {
      //             //If the user does not belong to the group being checked in this iteration, immediately break and return false, no further checking needed
      //             System.out.println("========================================================================================================================");
      //             System.out.println("USER DOES NOT HAVE PERMISSION TO EDIT PAGE");
      //             System.out.println("========================================================================================================================");
      //             return false;
      //          }
      //       }
      //    }
      // }
      // //If the end of the loop is reached, as in the return statement never fired, then the user must belong
      // //to all groups of that permission type attached to a space, allow the user access in this case
      // return true;

      return Utilities.isContentRestricted((ContentEntityObject)target, user, this.userAccessor, ContentPermission.EDIT_PERMISSION);
   }

   public void setUserAccessor(UserAccessor userAccessor) {
      this.userAccessor = userAccessor;
   }
}