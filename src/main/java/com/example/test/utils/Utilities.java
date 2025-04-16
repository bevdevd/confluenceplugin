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
    private final PageService pageService;
    @ComponentImport
    private final BlogPostService blogPostService;
    @ComponentImport
    private final SpaceService spaceService;
    
    public Utilities(
        ContentService contentService,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService
    ){
        this.contentService = contentService;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;
    }

    // Restriction Checks

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