package com.example.test.utils;

/* UTILITY IMPORTS */
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.ContentService.ContentFetcher;
import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.model.content.ContentType;
import com.atlassian.confluence.core.SpaceContentEntityObject;

import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.sal.api.component.ComponentLocator;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.BlogPost;

import com.atlassian.plugin.ModuleCompleteKey;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.core.ContentEntityObject;

import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.content.service.BlogPostService;
import com.atlassian.confluence.api.service.content.SpaceService;


import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.mail.notification.Notification;

import com.atlassian.confluence.labels.Label;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections; 
import java.util.List;
import java.lang.ThreadLocal;
/* END UTILITY IMPORTS */

public class Utilities {
    @ConfluenceImport
    private ContentService contentService;
    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;
    @ComponentImport
    private final UserAccessor userAccessor;
    @ComponentImport
    private final PageService pageService;
    @ComponentImport
    private final BlogPostService blogPostService;
    @ComponentImport
    private final SpaceService spaceService;
    
    public Utilities(
        ContentService contentService,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService
    ){
        this.contentService = contentService;
        this.contentPermissionManager = contentPermissionManager;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;
    }

    // Restriction Checks
    // public Boolean isContentRestricted(ContentEntityObject content, List<String> userPermissionGroups) {
    //     //before anything, check if the content is in a restricted space
    //     SpaceContentEntityObject spaceContent = (SpaceContentEntityObject) content;
    //     Space space = spaceContent.getSpace();
    //     if(isSpaceRestricted(space, userPermissionGroups)){
    //         return true;
    //     }
    //     //if not, check if the content itself is restricted, or has inherited any restrictions
    //     List<ContentPermissionSet> contentPermissionSets = contentPermissionManager.getContentPermissionSets(content, ContentPermission.VIEW_PERMISSION);
    //     for(ContentPermissionSet set : contentPermissionSets) {
    //         for(ContentPermission permission : set) {
    //             String groupName = permission.getGroupName();
    //             System.out.println("--------++++++++======== GROUP : "+groupName+" ========++++++++--------");
    //             if(groupName == null) {
    //                 System.out.println("--------++++++++======== SKIPPING NULL GROUP ========++++++++--------");
    //                 continue;
    //             }
    //             if(!userPermissionGroups.contains(groupName)){
    //                 return true;
    //             } 
    //         }
    //     }
    //     return false;
    // }
    // public Boolean isSpaceRestricted(Space space, List<String> userPermissionGroups){
    //     System.out.println("--------++++++++======== COMPARING SPACE "+space.getName()+" ========++++++++--------");

    //     List<SpacePermission> spacePermissions = space.getPermissions();
    //     for(SpacePermission permission : spacePermissions) {
    //         if(permission.isGroupPermission() && permission.getType().equals("VIEWSPACE")) {
    //             if(permission != null){
    //                 System.out.println("--------++++++++======== COMPARING SPACE PERMISSION "+permission.getGroup()+" ========++++++++--------");
    //                 if(!userPermissionGroups.contains(permission.getGroup())) {
    //                     System.out.println("--------++++++++======== SPACE RESTRICTED TO USER ========++++++++--------");
    //                     return true;
    //                 }
    //             }
    //         }
    //     }
    //     return false;
    // }

    // Misc. Utilities
    public List<ContentEntityObject> getSpaceChildrenContent(ContentType type, Space space) {
        System.out.println("--------++++++++========  GETTING CHILDREN TYPE : "+type.toString()+" ========++++++++--------");
        
        com.atlassian.confluence.api.model.content.Space APISpace = spaceService.find()
                .withKeys(space.getKey())
                .fetch().get();
        
        List<ContentEntityObject> spaceContentList = new ArrayList<ContentEntityObject>();

        int start = 0;
        int limit = 50;
        Boolean repeat = true;
        do {
            PageRequest request = new SimplePageRequest(start, limit);
            PageResponse<Content> spaceContent = contentService.find()
                .withSpace(APISpace)
                .fetchMany(type, request);

            for(Content content : spaceContent.getResults()) {
                System.out.println("--------++++++++========  GETTING CHILD : "+content.getId().toString()+" ========++++++++--------");
                switch (content.getType().getType()) {
                    case "page" :
                        Page page = pageService.getIdPageLocator(content.getId().asLong()).getPage();
                        spaceContentList.add(pageService.getIdPageLocator(content.getId().asLong()).getPage());
                        break;
                    case "blogpost" :
                        BlogPost blogPost = blogPostService.getIdBlogPostLocator(content.getId().asLong()).getBlogPost();
                        spaceContentList.add(blogPost);
                        break;
                }
            }
            start += limit;
            repeat = spaceContent.hasMore();
        }while(repeat);

        return spaceContentList;
    }
}