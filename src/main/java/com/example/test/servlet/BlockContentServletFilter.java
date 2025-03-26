package com.example.test.servlet;

import com.example.test.utils.Utilities;

import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.ContentService.ContentFetcher;
import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.content.service.BlogPostService;
import com.atlassian.confluence.api.service.content.SpaceService;

import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.model.content.ContentType;

import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.sal.api.component.ComponentLocator;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.BlogPost;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.core.ContentEntityObject;

import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.mail.notification.NotificationManager;
import com.atlassian.confluence.mail.notification.Notification;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;

import javax.servlet.ServletException;
import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import javax.servlet.ServletException;
import java.io.IOException;

import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections; 
import java.util.List;
import java.lang.ThreadLocal;
import java.util.Map;
import java.util.Arrays;

@Named
public class BlockContentServletFilter implements Filter{
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
    @ComponentImport
    private final NotificationManager notificationManager;

    private FilterConfig config;

    private Utilities utilities;

    @Inject
    public BlockContentServletFilter (
        ContentService contentService,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService,
        NotificationManager notificationManager
    ) {
        this.contentService = contentService;
        this.contentPermissionManager = contentPermissionManager;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;
        this.notificationManager = notificationManager;

        this.utilities = new Utilities(
            this.contentService,
            this.contentPermissionManager,
            this.userAccessor,
            this.pageService,
            this.blogPostService,
            this.spaceService
        );
    }

    @Override
    public void init(FilterConfig config){
        this.config = config;
    }
    @Override
    public void destroy() {}

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        try {
            // Below for debugging
            // System.out.println("=======================================================================================");
            // System.out.println("|                                         URI                                         |");
            // System.out.println("=======================================================================================");
            // System.out.println();
            // System.out.println(uri);
            // System.out.println();
            // System.out.println("=======================================================================================");
            // System.out.println("|                                      END OF URI                                     |");
            // System.out.println("=======================================================================================");

            //http://localhost:1990/confluence/pages/viewpage.action?pageId=2228231

            if(
                uri.contains("spacecalendar.action") ||
                uri.contains("calendarpage.action") ||
                uri.contains("mycalendar.action") ||
                uri.contains("viewrecentblogposts.action") ||
                uri.contains("editblogpost.action")
            ){  // Block these accesses directly, no further logic needed
                System.out.println("uh uh, bad");
            }
            if(
                uri.contains("viewpage.action")
            ){  // View blog posts themselves is treated as viewing a page, use the pageId and content service to find the type of content to block blog posts
                if(request.getParameter("pageId") != null) {
                    Content content = contentService.find().withId(ContentId.of(Long.parseLong(request.getParameter("pageId")))).fetch().get();
                    if(content.getType().getType().equals("blogpost")) {
                        System.out.println("uh uh, bad");
                    }
                }
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }

    }
}
