package com.example.test.servlet;

import com.atlassian.sal.api.user.UserManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.user.ConfluenceUser;

import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.security.PermissionManager;

import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;


import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

//deprecated
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;

import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.sal.api.user.UserManager;



import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.SpaceService;

public class ConfluenceService {

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final UserAccessor userAccessor;

    @ComponentImport
    private final PermissionManager permissionManager;

    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;

    @ComponentImport
    private final SpacePermissionManager spacePermissionManager;

    @ComponentImport
    private final ContentService contentService;
    @ComponentImport
    private final SpaceService spaceService;

    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final SpaceManager spaceManager;
    @ComponentImport
    private final AttachmentManager attachmentManager;

    @Inject
    public ConfluenceService(
        UserManager userManager,
        UserAccessor userAccessor,
        PermissionManager permissionManager,
        PageManager pageManager,
        SpaceManager spaceManager,
        AttachmentManager attachmentManager,
        ContentPermissionManager contentPermissionManager,
        SpacePermissionManager spacePermissionManager,
        ContentService contentService,
        SpaceService spaceService
        ){
            this.userManager = userManager;
            this.userAccessor = userAccessor;
            this.permissionManager = permissionManager;
            this.spaceManager = spaceManager;
            this.pageManager = pageManager;
            this.attachmentManager = attachmentManager;
            this.contentPermissionManager = contentPermissionManager;
            this.spacePermissionManager = spacePermissionManager;
            this.contentService = contentService;
            this.spaceService = spaceService;
    }

    public Space getSpaceFromKey(long spaceId) {
        String spaceKey = spaceManager.getSpaceFromPageId(spaceId);

        System.out.println("root space key: " + spaceKey);
        return spaceManager.getSpace(spaceKey);
    }

    public Space getSpaceFromKey(String spaceKey) {

        return spaceManager.getSpace(spaceKey);
    }

    public List<ContentPermissionSet> getContentPermissions(Page page) {
        return contentPermissionManager.getContentPermissionSets(page, ContentPermission.VIEW_PERMISSION);
    }

    public Page getPage(String pageKey) {
        return pageManager.getPage(pageKey);
    }

    public Page getPage(long pageId) {
        return pageManager.getPage(pageId);
    }

    public List<String> getUserGroups(ConfluenceUser user) {
        return userAccessor.getGroupNamesForUserName(user.getName());
    }
    
    public List<SpacePermission> getSpacePermissions(Space space) {
        return space.getPermissions();
    }
}
