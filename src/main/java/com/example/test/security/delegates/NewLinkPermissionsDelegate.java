package com.example.test.security.delegates;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.links.AbstractLink;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;

import com.atlassian.confluence.pages.Page;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.security.delegate.LinkPermissionsDelegate;

import java.util.List;
import java.util.Iterator;

public class NewLinkPermissionsDelegate extends LinkPermissionsDelegate {
   protected UserAccessor userAccessor;

   public boolean canView(User user, AbstractLink target) {
      // //return this.hasSpaceLevelPermission("VIEWSPACE", user, target) && this.hasContentLevelViewPermission(user, target);
      // ContentEntityObject content = (ContentEntityObject)this.getContent(target);
      // //Loop through all content permissions
      // if(content.hasPermissions(ContentPermission.VIEW_PERMISSION)) {
      //    for(ContentPermission permission : content.getContentPermissionSet(ContentPermission.VIEW_PERMISSION)) {
      //       if(permission.isGroupPermission()) {
      //          if(!this.userAccessor.getGroupNamesForUserName(user.getName()).contains(permission.getGroupName())) {
      //             //If the user does not belong to the group being checked in this iteration, immediately break and return false, no further checking needed
      //             System.out.println("========================================================================================================================");
      //             System.out.println("USER DOES NOT HAVE PERMISSION TO VIEW LINK");
      //             System.out.println("========================================================================================================================");
      //             return false;
      //          }
      //       }
      //    }
      // }
      // //If the end of the loop is reached, as in the return statement never fired, then the user must belong
      // //to all groups of that permission type attached to a space, allow the user access in this case
      // return true;

      return Utilities.isContentRestricted((ContentEntityObject)this.getContent(target), user, this.userAccessor, ContentPermission.VIEW_PERMISSION);
   }

   public void setUserAccessor(UserAccessor userAccessor) {
      this.userAccessor = userAccessor;
   }

   private Object getContent(AbstractLink target) {
      return target.getSourceContent();
   }
}