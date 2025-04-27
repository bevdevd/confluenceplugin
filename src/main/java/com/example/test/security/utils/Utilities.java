package com.example.test.security.utils;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.core.ContentPermissionManager;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.Attachment;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.security.delegate.PagePermissionsDelegate;

import com.atlassian.sal.api.component.ComponentLocator;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;

public class Utilities {
    
   private static final List<String> andPerms = Arrays.asList("rl2", "rl1");

   Utilities(){}

   // I was incredibly silly when I wrote this, and accidentally inverted all the checks,
   // so if is<blank>Restricted(...) returns false, the entity is in fact restricted, contrary to how that sounds
   // These methods used to be is<blank>Permitted which is how this mistake occured, changing it now would require a full refactor

   public static boolean isSpaceRestricted(Space space, User user, UserAccessor userAccessor, String Type){
       List<SpacePermission> spacePermissions = space.getPermissions();
       
       boolean userPermitted = false;
       for(SpacePermission spacePermission : spacePermissions) {
          if(spacePermission != null) {
             if(spacePermission.isGroupPermission() && spacePermission.getType().equals(Type)) {
                if(!userAccessor.getGroupNamesForUserName(user.getName()).contains(spacePermission.getGroup())) {
                   //if the user isn't a member of any one of the groups attached to the space, check if it's one of the AND permissions
                   //if so immediately return false
                    if(andPerms.contains(spacePermission.getGroup())) {
                        System.out.println("========================================================================================================================");
                        System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH SPACE");
                        System.out.println("========================================================================================================================");
                        return false;
                    }
                } else {
                    // If user is a part of any group attached to the content, grant permission, assuming they are part of all mandatory groups
                    userPermitted = true;
                }
             }
          }
       }
       if(!userPermitted) {
           System.out.println("========================================================================================================================");
           System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH SPACE");
           System.out.println("========================================================================================================================");
       }
       return userPermitted;
   }
   public static boolean isContentRestricted(ContentEntityObject content, User user, UserAccessor userAccessor, String type) {
       boolean userPermitted = false;

       System.out.println("Calling isContentRestricted with the following permissions:");
      ContentPermissionManager contentPermissionManager = ComponentLocator.getComponent(ContentPermissionManager.class);

       List<ContentPermission> contentPermissionsList = new ArrayList<>();
       for(ContentPermissionSet set : contentPermissionManager.getContentPermissionSets(content, type)) {
         for(ContentPermission permission : set) {
            contentPermissionsList.add(permission);
         }
       }
       System.out.println(contentPermissionsList.toString());

       if(!contentPermissionsList.isEmpty()) {
           for(ContentPermission permission : contentPermissionsList) {
              if(permission.isGroupPermission()) {
                 if(!userAccessor.getGroupNamesForUserName(user.getName()).contains(permission.getGroupName())) {
                   //if the user isn't a member of any one of the groups attached to the content, check if it's one of the AND permissions
                   //if so immediately return false
                    if(andPerms.contains(permission.getGroupName())) {
                        System.out.println("========================================================================================================================");
                        System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH CONTENT");
                        System.out.println("========================================================================================================================");
                        return false;
                    }
                 } else {
                    // If user is a part of any group attached to the content, grant permission, assuming they are part of all mandatory groups
                    userPermitted = true;
                 }
              }
           }
        } else {
           // If content if unrestricted, permission is automatically granted
           return true;
        }
        if(!userPermitted) {
           System.out.println("========================================================================================================================");
           System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH CONTENT");
           System.out.println("========================================================================================================================");
       }
        return userPermitted;
   }


   // Gets the page that a piece of content (Comment, Attachment) belongs to
   public static Page getParentPage(ContentEntityObject content) {
      try {
            if (content != null) {
               if (content instanceof Page) {
                  return (Page) content;
               } else if (content instanceof Comment) {
                  Comment comment = (Comment) content;
                  ContentEntityObject container = comment.getContainer();
                  while (container instanceof Comment) {
                        container = ((Comment) container).getContainer();
                  }
                  if (container instanceof Page) {
                        return (Page) container;
                  }
               } else if (content instanceof Attachment) {
                  Attachment attachment = (Attachment) content;
                  ContentEntityObject container = attachment.getContainer();
                  if (container instanceof Page) {
                        return (Page) container;
                  }
               }
            }
      } catch(Exception e) {
            System.out.println("Error getting parent page : "+e);
      }
   
      // If a parent page cannot be found, return null
      return null;
   }
}
