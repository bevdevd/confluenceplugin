package com.plugins.permissions;

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
public class UserPermissionsChangedServletFilter implements Filter {
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

    private List<Notification> notificationsToRemove = new ArrayList<Notification>();

    @Inject
    public UserPermissionsChangedServletFilter (
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

    // Check all notifications a user has, and compare the new permissions to that of the content within the notification, remove any restricted
    public void cullUserNotifications(ConfluenceUser user, List<String> newUserPermissions) {
        List<Notification> userNotifications = notificationManager.getNotificationsByUser(user);

        for(Notification notification : userNotifications) {
            /* DEBUG */System.out.println("---------- ========== ++++++++++ CHECKING NOTIFICATION ON: " + notification.getContent().getIdAsString() + " ++++++++++ ========== ----------");
            ContentEntityObject content = notification.getContent();
            
            if (utilities.isContentRestricted(content, newUserPermissions)){
                /* DEBUG */System.out.println("---------- ========== ++++++++++ REMOVING NOTIFICATION ON: " + content.getIdAsString() + " ++++++++++ ========== ----------");
                notificationManager.removeNotification(notification);
            }
        }
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        try{
            if(uri.contains("editusergroups.action")) {
                /* DEBUG */System.out.println("---------- ========== ++++++++++ EDIT USER GROUPS ACTION ++++++++++ ========== ----------");
                
                Map<String,String[]> paramMap =  request.getParameterMap();

                for (Map.Entry<String,String[]> entry : paramMap.entrySet()) {
                    /* DEBUG */System.out.println(entry.getKey() + " is: " + Arrays.toString(entry.getValue()));
                }

                // Get the user by username, and their new permissions
                ConfluenceUser user = userAccessor.getUserByName(request.getParameter("username"));
                List<String> newUserPermissions = Arrays.asList(request.getParameter("newGroups").split("[\\[\\]\\s*,]"));

                cullUserNotifications(user, newUserPermissions);

                /* DEBUG */System.out.println("---------- ========== ++++++++++ FINISHED CULLING USER NOTIFICATIONS ++++++++++ ========== ----------");
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }
    }
}