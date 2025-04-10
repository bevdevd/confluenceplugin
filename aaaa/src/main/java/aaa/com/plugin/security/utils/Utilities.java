package aaa.com.plugin.security.utils;

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
    
   private static final List<String> andPerms = Arrays.asList("rl2");

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
                      /* DEBUG */System.out.println("========================================================================================================================");
                      /* DEBUG */System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH SPACE");
                      /* DEBUG */System.out.println("========================================================================================================================");
                      return false;
                  }
               } else {
                  /* DEBUG */System.out.println("========================================================================================================================");
                  /* DEBUG */System.out.println("USER IS PERMITTED TO INTERACT WITH SPACE");
                  /* DEBUG */System.out.println("========================================================================================================================");
                  // If user is a part of any group attached to the content, grant permission, assuming they are part of all mandatory groups
                  userPermitted = true;
               }
            }
         }
      }
      if(!userPermitted) {
         /* DEBUG */System.out.println("========================================================================================================================");
         /* DEBUG */System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH SPACE");
         /* DEBUG */System.out.println("========================================================================================================================");
      }
      return userPermitted;
   }

   //The below methods ignore inherritance, this can be fixed easily if needed, but the space check is always done with the content-level anyway
   public static boolean isContentRestricted(ContentEntityObject content, User user, UserAccessor userAccessor, String type) {
      if(content.hasPermissions(type)){
         return isContentRestricted(user, userAccessor, content.getContentPermissionSet(type));
      } else {
         /* DEBUG */System.out.println("========================================================================================================================");
         /* DEBUG */System.out.println("CONTENT HAS NO ATTACHED RESTRICTIONS");
         /* DEBUG */System.out.println("========================================================================================================================");
         return true;
      }
   }

   public static boolean isContentRestricted(User user, UserAccessor userAccessor, ContentPermissionSet permissionSet){
      if(permissionSet.isPermitted(user)) {
         //Check if the user belongs to any group attached to content, Confluence does this by default, so I'm leveraging that method
         for(ContentPermission permission : permissionSet) {
            // If the user is a member of any attached groups, check all of the groups to see if they are one of the AND groups
            // If so, check if the user is missing any of them
            // If so, the content should be blocked to the user 
            if(permission.isGroupPermission()) {
               if(andPerms.contains(permission.getGroupName())) {
                  /* DEBUG */System.out.println("========================================================================================================================");
                  /* DEBUG */System.out.println("CONTENT IS ATTACHED TO AND PERMISSION GROUP");
                  /* DEBUG */System.out.println("========================================================================================================================");
                  if(!userAccessor.getGroupNamesForUserName(user.getName()).contains(permission.getGroupName())) {
                     /* DEBUG */System.out.println("========================================================================================================================");
                     /* DEBUG */System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH CONTENT");
                     /* DEBUG */System.out.println("========================================================================================================================");
                     return false;
                  }
               }
            }
         }
      } else {
         /* DEBUG */System.out.println("========================================================================================================================");
         /* DEBUG */System.out.println("USER DOES NOT HAVE PERMISSION TO INTERACT WITH CONTENT");
         /* DEBUG */System.out.println("========================================================================================================================");
         return false;
      }
      /* DEBUG */System.out.println("========================================================================================================================");
      /* DEBUG */System.out.println("USER IS PERMITTED TO INTERACT WITH CONTENT");
      /* DEBUG */System.out.println("========================================================================================================================");
      return true;
   }
}


