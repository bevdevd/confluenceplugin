package com.plugins.permissions;

import com.example.test.utils.Utilities;

import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.ContentService.ContentFetcher;
import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.content.service.BlogPostService;
import com.atlassian.confluence.api.service.content.SpaceService;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.model.content.ContentType;

import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.sal.api.component.ComponentLocator;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.content.service.space.KeySpaceLocator;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.BlogPost;

import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.SpacePermission;

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
public class UserCreatesServletFilter implements Filter {
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
    private final SpaceManager spaceManager;

    private FilterConfig config;

    private Utilities utilities;

    @Inject
    public UserCreatesServletFilter (
        ContentService contentService,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService,
        SpaceManager spaceManager
    ) {
        this.contentService = contentService;
        this.contentPermissionManager = contentPermissionManager;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;
        this.spaceManager = spaceManager;

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

            // try{
            //     if(uri.contains("rest")){
            //         System.out.println(uri);
            //         ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
            //         if(loggedInUser != null){
            //             System.out.println(loggedInUser.getName());
            //         }else{
            //             throw new Exception("No User Logged In");
            //         }
            //     }
            // } catch (Exception e) {
            //     System.out.println("unfortunately, we have errored: " + e);
            // }
            


            if(
                uri.contains("createspace")
            ) {
                System.out.println("---------- ========== ++++++++++ SPACE CREATION ++++++++++ ========== ----------");
            }
            if( // Page Creation
                uri.contains("createpage-defaultspace.action") ||
                uri.contains("create-dialog") 
            ) {
                System.out.println("---------- ========== ++++++++++ STARTED CREATING PAGE ++++++++++ ========== ----------");
                httpResponse.getWriter().println("<script type=\"text/javascript\">alert('Boom! Alert Box!');</script>");
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }
    }
}