package com.example.test.servlet;

import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.user.UserManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.user.ConfluenceUser;

import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.security.PermissionManager;

//deprecated
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;


public class Utils {
    
    private final ConfluenceService confluenceService;


    public boolean isRestrictedPage(ConfluenceUser user, int confluencePageId, List<String> restrictedGroups) {
        System.out.println("checking if the page is restricted: " + confluencePageId);
        Page page = confluenceService.getPage(confluencePageId);
        System.out.println("id as long: ");
        if (page == null) {
            System.out.println("sadly this is null");
            return false;
        } else {
            System.out.println("id as long: " + page.getContentId().toString());

        }

        return isRestrictedPage(user, page, restrictedGroups);
    }

    public boolean isRestrictedPage(ConfluenceUser user, Page page, List<String> restrictedGroups) {
        long pageId = page.getContentId().asLong();
        System.out.println("checking if the page (from page) is restricted: " + page.getContentId().toString());

        ///First check whether the space is restricted to the user
        String spaceKey = spaceManager.getSpaceFromPageId(pageId);
        System.out.println("root space key: " + spaceKey);
        Space rootSpace = spaceManager.getSpace(spaceKey);
        System.out.println("root space : " + rootSpace.getName());
        if (isRestrictedSpace(user, rootSpace, restrictedGroups)) {
            return true;
        }
        System.out.println("Page is not restricted");

        // Then check whether any ancestors are restricted to the user
        List<Page> ancestors = page.getAncestors();
        System.out.println("ancestors: ");
        System.out.println(Arrays.toString(ancestors.toArray()));

        for (Page ancestor : ancestors) {
            if (isRestrictedPage(user, ancestor, restrictedGroups)) {
                System.out.println("ancestor is restricted");
                return true;
            } else {
                System.out.println("ancestor is not restricted");
            }
        }

        List<ContentPermissionSet> permissionList = ConfluenceService.getContentPermissions(page, ContentPermission.VIEW_PERMISSION);
        System.out.println("there are " + permissionList.size() + " sets of permissions here");
        for (ContentPermissionSet permissionSet : permissionList) {
            System.out.println(Arrays.toString(permissionSet.getGroupNames().toArray()));
        }
        List<String> userGroups = userAccessor.getGroupNamesForUserName(user.getName());

        List<String> viewGroups = new ArrayList<String>();
        // Only group permissions are relevant here
        for (ContentPermissionSet set : permissionList) {
            for (ContentPermission permission : set) {
                if (permission.isGroupPermission()) {
                    viewGroups.add(permission.getGroupName());
                }
            }
        }
        System.out.println("GOT VIEWGFROUPS");
        System.out.println(Arrays.toString(viewGroups.toArray()));

        for (String group : viewGroups) {
            if (restrictedGroups.contains(group)) {
                System.out.println("mandatory group present: " + group);
                System.out.println("user groups:  " + Arrays.toString(userGroups.toArray()));

                if (!userGroups.contains(group)) {
                    System.out.println("user should NOT be able to see");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRestrictedSpace(ConfluenceUser user, Space space, List<String> restrictedGroups) {        
        System.out.println("checking if the space is restricted: " + space.getKey());

        // Check whether the restricted groups have permissions on the page
        // if not, we can use the default confluence permissions going forward
        // If so, check whether the user is part of present the restricted groups 
        List<String> spaceRestrictedGroups = new ArrayList<String>();

        List<SpacePermission> spacePermissions = ConfluenceService.getSpacePermissions(space);
        System.out.println("restricted groups:  " + Arrays.toString(restrictedGroups.toArray()));

        for (SpacePermission permission : spacePermissions) {

            if (permission.isGroupPermission() && permission.getType().equals("VIEWSPACE")) {
                String group = permission.getGroup();
                if (restrictedGroups.contains(group)) {
                    spaceRestrictedGroups.add(group);
                }
            }
        }
        List<String> userGroups = ConfluenceService.getUserGroups(user);

        for (String group : spaceRestrictedGroups) {
            if (!userGroups.contains(group)) {
                System.out.println("user should NOT be able to see this restricted SPACE");
                return true;
            }
        }
        System.out.println("user can see this SPACE");
        return false;
    }

}
