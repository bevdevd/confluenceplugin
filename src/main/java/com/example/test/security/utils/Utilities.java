package com.example.test.security.utils;

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
import java.util.Arrays;

public class Utilities {
    
    private static final List<String> andPerms = Arrays.asList("rl2", "rl1");

    Utilities(){}

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
        if(content.hasPermissions(type)) {
            for(ContentPermission permission : content.getContentPermissionSet(type)) {
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
}
