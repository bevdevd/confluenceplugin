package com.example.test.servlet;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.CharArrayWriter;

import java.net.URI;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;


import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.user.ConfluenceUser;
import org.springframework.web.util.ContentCachingResponseWrapper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import java.util.Iterator;

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

import com.atlassian.confluence.api.model.content.id.ContentId;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.Permission;

//deprecated
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;


public class PageServlet implements Filter{
    private String[] idPatterns = {
        "/quickreload/latest/",
        "/likes/1.0/content/",
        "/api/content/",
        "/rest/likes/1.0/content/"
    };

    private String[] staticPatterns = {
        "/plugins/pagetree/naturalchildren.action",
    };
    private String prefix = "/confluence";
    private String cattedPattern = "(" + String.join("|", idPatterns) + ")(?<pageId>\\d+)(/.*)?";
    private Pattern staticPattern = Pattern.compile("/plugins/pagetree/naturalchildren.action");
    
    private Pattern pageIdPattern = Pattern.compile(cattedPattern);
    // Pattern.compile("viewpage.action?pageId=(?<pageId>\d+)#"
    private Pattern spaceAndPagePattern = Pattern.compile("/display/(?<spaceKey>[a-zA-Z0-9]+)/(?<page>[^/]+)");
    private Pattern spacePattern = Pattern.compile("/display/(?<spaceKey>[a-zA-Z0-9]+)");
    // "/confluence/rest/api/content/1507332"
    private Pattern apiContentPattern = Pattern.compile("/confluence/rest/api/content/(?<pageId>[0-9]+)");
    
    @ComponentImport
    private final UserAccessor userAccessor;
    @ComponentImport
    private final PermissionManager permissionManager;

    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;

    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final SpaceManager spaceManager;

    private boolean fail = false;

    Logger logger = LoggerFactory.getLogger(PageServlet.class);
    private FilterConfig config;

    @Inject
    public PageServlet(
        UserAccessor userAccessor,
        PermissionManager permissionManager,
        PageManager pageManager,
        SpaceManager spaceManager,
        ContentPermissionManager contentPermissionManager
        ){
            this.userAccessor = userAccessor;
            this.permissionManager = permissionManager;
            this.spaceManager = spaceManager;
            this.pageManager = pageManager;
            this.contentPermissionManager = contentPermissionManager;
    }

    @Override
    public void init(FilterConfig config){
        this.config = config;
    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        
        List<String> restrictedGroups = new ArrayList<String>();
        restrictedGroups.add("restricted");
        restrictedGroups.add("nat.aus");
        try {

            ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();


            Matcher spaceAndPage =  this.spaceAndPagePattern.matcher(uri);
            String spaceKey = spaceAndPage.group("spaceKey");
            // the "+" is automatically added to replace spaces so we need to swap it back
            String pageKey = spaceAndPage.group("page").replace("+", " ");
            System.out.println("spaceKey  :  " + spaceKey);
            System.out.println("pageKey  :  " + pageKey);
            Page page = pageManager.getPage(spaceKey, pageKey);

    
            if (loggedInUser != null && page != null) {
                System.out.println("_________________________________________________________________________________");
                System.out.println(String.format("Current Date/Time : %tc", new Date()));
                
                List<String> viewGroups = new ArrayList<String>();
                System.out.println(viewGroups + "=" + Arrays.toString(viewGroups.toArray()));
    
                List<ContentPermissionSet> permissionList = contentPermissionManager.getContentPermissionSets(page, ContentPermission.VIEW_PERMISSION);
                      
                System.out.println(permissionList + "=" + Arrays.toString(permissionList.toArray()));
                ContentId pageId = page.getContentId();
                
                if (Utils.isRestrictedPage(loggedInUser, page, restrictedGroups)) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                } else {
                    System.out.println("page is not restricted");
                }
            }
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored in the PageServlet : " + e);
            //e.printStackTrace();
            //continue as per normal if the servlet filter fails
            chain.doFilter(request, response);

        }
       
    }
}
