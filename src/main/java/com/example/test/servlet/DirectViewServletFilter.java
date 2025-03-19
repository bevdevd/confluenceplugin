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
public class DirectViewServletFilter implements Filter {
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
    public DirectViewServletFilter (
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
        try{
            // get the currently logged on user, and the permission groups they belong to
            if(     // Page View Actions
                uri.contains("viewpage.action") ||
                uri.contains("viewpageattachments.action") ||
                uri.contains("viewpreviousversions.action") ||
                uri.contains("viewinfo.action") ||
                uri.contains("viewsource.action") ||
                uri.contains("editpage.action") ||
                uri.contains("pdfpageexport.action") ||
                uri.contains("exportword")
            ) {
                /* DEBUG */System.out.println("---------- ========== ++++++++++ VIEW PAGE ACTION ++++++++++ ========== ----------");
                if(isPageViewActionRestricted(httpRequest, httpResponse))
                {
                    /* DEBUG */System.out.println("---------- ========== ++++++++++ RESTRICTED TO USER ++++++++++ ========== ----------");
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            if(     // Space View Actions
                uri.contains("pages.action") ||
                uri.contains("listpages.action") ||
                uri.contains("viewrecentblogposts.action") ||
                uri.contains("listpagetemplates.action") ||
                uri.contains("listattachmentsforspace.action") ||
                uri.contains("viewspacesummary.action") ||
                uri.contains("/display/")
            ) {
                if(isSpaceViewActionRestricted(httpRequest, httpResponse)) {
                    /* DEBUG */System.out.println("---------- ========== ++++++++++ RESTRICTED TO USER ++++++++++ ========== ----------");
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            if(     //Attachment Preview Action
            uri.contains("preview=/")
            ) {
                if(isAttachmentViewActionRestricted(httpRequest, httpResponse)) {
                    /* DEBUG */System.out.println("---------- ========== ++++++++++ RESTRICTED TO USER ++++++++++ ========== ----------");
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            

            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }
    }

    public Boolean isPageViewActionRestricted(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {

        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        List<String> userPermissionGroups = this.userAccessor.getGroupNamesForUserName(loggedInUser.getName());
        // /* DEBUG */System.out.println("---------- ========== ++++++++++ RETRIVED USER ++++++++++ ========== ----------");

        // get a map of all the request parameters
        Map<String,String[]> paramMap =  request.getParameterMap();
        for (Map.Entry<String,String[]> entry : paramMap.entrySet()) {
            /* DEBUG */System.out.println(entry.getKey() + " is: " + Arrays.toString(entry.getValue()));
        }

        // if there's a 'title' parameter, the request is trying access a page, so default to the page handling method
        if(request.getParameter("title") != null) {
            Page page = pageService.getTitleAndSpaceKeyPageLocator(request.getParameter("spaceKey"), request.getParameter("title")).getPage();
            /* DEBUG */System.out.println("---------- ========== ++++++++++ RETRIVED PAGE : "+page.getContentId().toString()+" ++++++++++ ========== ----------");     
            /* DEBUG */System.out.println("---------- ========== ++++++++++ CHECKING RESTRICTIONS ++++++++++ ========== ----------");       
            return utilities.isContentRestricted(page, userPermissionGroups);
        } else if(request.getParameter("pageId") != null) {     // otherwise, the request could be for eaither a page or a blogpost, we can use the pageId parameter to figure out, and restrict, each
            Content content = contentService.find().withId(ContentId.of(Long.parseLong(request.getParameter("pageId")))).fetch().get();
            switch (content.getType().getType()) {
                case "page":
                    Page page = pageService.getIdPageLocator(content.getId().asLong()).getPage();
                    return utilities.isContentRestricted(page, userPermissionGroups);
                    // break;
                case "blogpost":
                    BlogPost blogpost = blogPostService.getIdBlogPostLocator(content.getId().asLong()).getBlogPost();
                    return utilities.isContentRestricted(blogpost, userPermissionGroups);
                    // break;
                default:
                    throw new Exception("Unknown Content Type");
            }
        } else {    // if neither a pageID, or title parameter exist, then the request doesn't contain any information to identify the content, we should throw an error in this case
            throw new Exception("Content Identifier in Request");
        }
    }

    public Boolean isSpaceViewActionRestricted(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {

        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        List<String> userPermissionGroups = this.userAccessor.getGroupNamesForUserName(loggedInUser.getName());
        // /* DEBUG */System.out.println("---------- ========== ++++++++++ RETRIVED USER ++++++++++ ========== ----------");

        String uri = request.getRequestURI();
        
        // get a map of all the request parameters
        Map<String,String[]> paramMap =  request.getParameterMap();
        for (Map.Entry<String,String[]> entry : paramMap.entrySet()) {
            /* DEBUG */System.out.println(entry.getKey() + " is: " + Arrays.toString(entry.getValue()));
        }

        // If there's a 'key' parameter, use that to identify the space we're currrently actioning
        if(request.getParameter("key") != null){
            Space space = new KeySpaceLocator(this.spaceManager, request.getParameter("key")).getSpace();
            /* DEBUG */System.out.println("---------- ========== ++++++++++ SPACE FOUND: " + space.getName() + " ++++++++++ ========== ----------");
            return utilities.isSpaceRestricted(space, userPermissionGroups);
        } else if (uri.contains("/display/")) {
            /* DEBUG */ System.out.println("---------- ========== ++++++++++ Attempting to view space ++++++++++ ========== ----------");
            Space space = new KeySpaceLocator(this.spaceManager, uri.split("/display/")[1].split("/")[0]).getSpace();
             /* DEBUG */System.out.println("---------- ========== ++++++++++ SPACE FOUND: " + space.getName() + " ++++++++++ ========== ----------");
            // return utilities.isSpaceRestricted(space, userPermissionGroups);
        } else {    //Otherwise, throw an exception as we have now space Identifier
            throw new Exception("No Space Identifier in Request");
        }
        return false;
    }

    public Boolean isAttachmentViewActionRestricted(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        List<String> userPermissionGroups = this.userAccessor.getGroupNamesForUserName(loggedInUser.getName());
        // /* DEBUG */System.out.println("---------- ========== ++++++++++ RETRIVED USER ++++++++++ ========== ----------");

        String uri = request.getRequestURI();

        String pageId = uri.split("/attachments/")[1].split("/")[0];
        Page page = pageService.getIdPageLocator(Long.parseLong(pageId)).getPage();
        if(page != null) {
            return utilities.isContentRestricted(page, userPermissionGroups);
        } else {
            throw new Exception("No Page Identifier for Attachment");
        }
    }
}