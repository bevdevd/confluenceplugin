package com.example.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.CharArrayWriter;

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

import java.net.URI;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.Page;
// import com.atlassian.confluence.pages.actions.PageAware;
import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.content.Content;
// import com.atlassian.confluence.api.model.content.Space;
import com.atlassian.confluence.api.service.content.SpaceService.SpaceFinder;
import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.SpaceService;

// Newer way to get Space/Pages

//import com.onresolve.scriptrunner.runner.ScriptRunnerImpl;


//deprecated
import com.atlassian.confluence.spaces.SpaceManager;
// import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.api.model.content.Space;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.security.SpacePermissionManager;

import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.plugin.servlet.PluginHttpRequestWrapper;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.sal.api.web.context.HttpContext;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.web.util.ContentCachingResponseWrapper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;


public class SpaceTest implements Filter{
    private String[] idPatterns = {
        "/quickreload/latest/",
        "/likes/1.0/content/",
        "/api/content/",
        "/rest/likes/1.0/content/"
    };

    private String[] staticPatterns = {
        "/plugins/pagetree/naturalchildren.action",
    };

    private String cattedPattern = "(" + String.join("|", idPatterns) + ")(?<pageId>\\d+)(/.*)?";
    private Pattern staticPattern = Pattern.compile("/plugins/pagetree/naturalchildren.action");
    
    private Pattern pageIdPattern = Pattern.compile(cattedPattern);
    // Pattern.compile("viewpage.action?pageId=(?<pageId>\d+)#"
    private Pattern spaceAndPagePattern = Pattern.compile("/display/(?<spaceKey>[a-zA-Z0-9]+)/(?<page>[^/]+)");
    private Pattern spacePattern = Pattern.compile("/display/(?<spaceKey>[a-zA-Z0-9]+)");
    // "/confluence/rest/api/content/1507332"
    private Pattern apiContentPattern = Pattern.compile("/confluence/rest/api/content/(?<pageId>[0-9]+)");
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final UserAccessor userAccessor;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
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

    private boolean fail = false;

    Logger logger = LoggerFactory.getLogger(SpaceTest.class);
    private FilterConfig config;

    @Inject
    public SpaceTest(
        UserManager userManager,
        LoginUriProvider loginUriProvider,
        UserAccessor userAccessor,
        PermissionManager permissionManager,
        PageManager pageManager,
        SpaceManager spaceManager,
        ContentPermissionManager contentPermissionManager,
        SpacePermissionManager spacePermissionManager,
        ContentService contentService,
        SpaceService spaceService
        ){
            this.userManager = userManager;
            this.loginUriProvider = loginUriProvider;
            this.userAccessor = userAccessor;
            this.permissionManager = permissionManager;
            this.spaceManager = spaceManager;
            this.pageManager = pageManager;
            this.contentPermissionManager = contentPermissionManager;
            this.spacePermissionManager = spacePermissionManager;
            this.contentService = contentService;
            this.spaceService = spaceService;
    }

    @Override
    public void init(FilterConfig config){
        this.config = config;
    }

    // private Page getPage(String uri, String confluencePageId){
        
    // }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException{
    
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        System.out.println("uri  :  " + uri);
        ServletContext context = config.getServletContext();
        List<String> restrictedGroups = new ArrayList<String>();
        restrictedGroups.add("restricted");
        restrictedGroups.add("nat.aus");
        try {
            UserProfile user = this.userManager.getRemoteUser(httpRequest);
            ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
            // if (loggedInUser != null) {
            //     System.out.println("_________________________________________________________________________________");
            //     System.out.println("logged in user is: " + loggedInUser);

            // } else {
            //     System.out.println("cant find logged in user");
            // }
            Map<String,String[]> paramMap =  request.getParameterMap();
            for (Map.Entry<String,String[]> entry : paramMap.entrySet()) {

                System.out.println(entry.getKey() + " is: " + Arrays.toString(entry.getValue()));
            }


            String confluencePageId = request.getParameter("pageId");   
            String confluenceSpaceId = request.getParameter("spaceId");    
            String confluenceSpaceKey = request.getParameter("spaceKey");      
            if (confluenceSpaceId != null) {
                // contentService.find()
                //     .withSpace(confluenceSpaceId)
                //     .fetch
            }
            boolean isPage = false;
            boolean isSpace = false;
            //String confluencePageId = "";
            String spaceKey = "";
            String pageKey = "";
            Page page = null;
            Space space = null;
            // Page foundPage = getPage(uri, confluencePageId);
            // Check whether its a page or a space
            System.out.println("====================checking for page or a space==========================");
            String pathInfo = uri;
            if (uri.startsWith("/confluence")) {
                pathInfo = uri.replaceFirst("/confluence", "");
            }
            System.out.println("pathInfo  :  " + pathInfo);
            System.out.println("confluencePageId  :  " + confluencePageId);
            if (pathInfo != null ) {
                Matcher pageId =  this.pageIdPattern.matcher(pathInfo);
                Matcher spaceAndPage =  this.spaceAndPagePattern.matcher(pathInfo);
                Matcher spaceMatcher =  this.spacePattern.matcher(pathInfo);
                Matcher staticPattern =  this.staticPattern.matcher(pathInfo);

                if (pageId.matches() || confluencePageId != null) {
                    System.out.println("pageid Matched ++++++++++++++++++  :  ");
                    confluencePageId = pageId.matches() 
                        ? pageId.group("pageId")
                        : confluencePageId;
                    page = pageManager.getPage(Long.parseLong(confluencePageId));
                    isPage = true;
                }
                else if (staticPattern.matches() || confluencePageId != null) {
                    System.out.println("static pattern Matched ++++++++++++++++++  :  ");
                    confluencePageId = confluencePageId;
                    page = pageManager.getPage(Long.parseLong(confluencePageId));
                    isPage = true;
                }
    
                else if (spaceAndPage.matches()) {
                    System.out.println("space and page Matched ++++++++++++++++++  :  ");
                    spaceKey = spaceAndPage.group("spaceKey");
                    // the "+" is automatically added to replace spaces so we need to swap it back
                    pageKey = spaceAndPage.group("page").replace("+", " ");
                    System.out.println("spaceKey  :  " + spaceKey);
                    System.out.println("pageKey  :  " + pageKey);
                    page = pageManager.getPage(spaceKey, pageKey);
                    isPage = true;
                }
                else if (spaceMatcher.matches()) {
                    System.out.println("space Matched ++++++++++++++++++  :  ");
                    spaceKey = spaceMatcher.group("spaceKey");
                    System.out.println("spaceKey  :  " + spaceKey);
                    Space otherSpace = spaceService.find()
                        .withKeys(spaceKey)
                        .fetch()
                        .get();
                    System.out.println("space=" + otherSpace.getName());
                    System.out.println("s=============================================+");
                    isSpace = true;
                }
            }
            
            // if (fail) {
            //     fail = false;
            //     ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            //     return;
            // }
            // if (foundPage != null) {
            //     System.out.println("_________________________________________________________________________________");
            //     System.out.println("found page is: " + foundPage);

            // } else {
            //     System.out.println("cant find page " + confluencePageId);
            // }
            if (isPage && loggedInUser != null && page != null) {
                System.out.println("_________________________________________________________________________________");
                System.out.println(String.format("Current Date/Time : %tc", new Date()));
                for (String key : paramMap.keySet()) {
                    System.out.println(key + "=" + Arrays.toString(paramMap.get(key)));
                }
                List<String> viewGroups = new ArrayList<String>();
                System.out.println(viewGroups + "=" + Arrays.toString(viewGroups.toArray()));

                List<ContentPermissionSet> permissionList = contentPermissionManager.getContentPermissionSets(page, ContentPermission.VIEW_PERMISSION);
                      
                System.out.println(permissionList + "=" + Arrays.toString(permissionList.toArray()));
                ContentId pageId = page.getContentId();
                //ContentId spaceId = ContentId.of(confluenceSpaceKey);
                // Using the content service as its the preferred way over using pagemanager.
                // Space rootSpace = spaceService.find()
                //     .withId(pageId)
                //     .fetch(new Expansion("space"))
                //     .get()
                //     .getSpace();
                // Content page = contentService.find()
                //     .withId(pageId)
                //     .fetch()
                //     .get();
                // System.out.println("THE PAGE IS: " + page.toString());
                // Extract the space key from the collapsed reference
                // String spaceKey = page.getSpace().getIdProperties().get("key");
                // System.out.println("THE spacekey IS: " + spaceKey);

                // // Fetch the complete space object using the space key
                // SpaceFinder spaceFinder = spaceService.find().withProperty("key", spaceKey);
                // Space space = spaceFinder.fetch().get();
                    System.out.println("THE SPACE key IS: " + confluenceSpaceKey);



                // Space rootSpace = spaceManager.getSpace(confluenceSpaceKey);

                // if (rootSpace != null) {
                //     System.out.println("THE SPACE IS: " + rootSpace.getName());
                //     if (isRestrictedSpace(loggedInUser, rootSpace, restrictedGroups)) {
                //         ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                //         System.out.println(rootSpace.getName() + " space is  restricted");
                //         return;
                //     } else {
                //         System.out.println("space is not restricted");
                //     }

                // } else {
                //     System.out.println("no root SPACE found :(");
                // }


                // Space rootSpace;
                // if (page.getSpace() == null) {
                //     // Fetch space separately
                //     rootSpace = spaceService.find().withKey(confluenceSpaceKey).fetch().get();
                // }
            
                // else {
                //     rootSpace = page.getSpace();
                // }
                // System.out.println("THE ROOT SPACE IS: " + rootSpace.getName());
                
                List<Page> ancestors = page.getAncestors();
                System.out.println("ancestors: ");
                System.out.println(Arrays.toString(ancestors.toArray()));

                
                // // Don't display the child if the user doesnt have permissions to view the parent
                // for (Page ancestor : ancestors) {
                //     if (isRestrictedPage(loggedInUser, ancestor, restrictedGroups)) {
                //         ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                //         return;
                //     } else {
                //         System.out.println("ancestor is not restricted");
                //     }
                // }
                // if (isRestrictedPage(loggedInUser, page, restrictedGroups)) {
                //     ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                //     return;
                // } else {
                //     System.out.println("page is not restricted");
                // }
            }
            // else if (isSpace && loggedInUser != null && space != null) {
            //     System.out.println("THE SPACE IS: " + space.getName());
            //     if (isRestrictedSpace(loggedInUser, space, restrictedGroups)) {
            //         ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            //         System.out.println("space is  restricted");
            //         return;
            //     } else {
            //         System.out.println("space is not restricted");
            //     }
            // }

        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            //e.printStackTrace();
        }
        finally {

            chain.doFilter(request, response);
        }
    }

 
    @Override
    public void destroy(){
        
    }


    public boolean isRestrictedPage(ConfluenceUser user, int confluencePageId, List<String> restrictedGroups) {
        System.out.println("checking if the page is restricted: " + confluencePageId);
        Page page = pageManager.getPage(confluencePageId);
        ContentId contentId = ContentId.of(confluencePageId);
        Content content = contentService.find()
            .withId(contentId)
            .fetch()
            .get();

        log.info("Page Title: {}", content.getTitle());
        log.info("Page ID: {}", content.getId());
        // System.out.println("GOT PAGE");
        // ///First check whether the space is restricted to the user
        // String spaceKey = spaceManager.getSpaceFromPageId(confluencePageId);
        // System.out.println("root space key: " + spaceKey);
        // Space rootSpace = spaceManager.getSpace(spaceKey);
        // System.out.println("root space : " + rootSpace.getName() );
        // if (isRestrictedSpace(user, rootSpace, restrictedGroups)) {
        //     return true;
        // }

        // List<ContentPermissionSet> permissionList = contentPermissionManager.getContentPermissionSets(page, ContentPermission.VIEW_PERMISSION);
        // System.out.println("GOT PERMISSION LIST");

        // List<String> userGroups = this.userAccessor.getGroupNamesForUserName(user.getName());
        // System.out.println("GOT USER GROUPS");

        // List<String> viewGroups = new ArrayList<String>();
        // // Only group permissions are relevant here
        // for (ContentPermissionSet set : permissionList) {
        //     for (ContentPermission permission : set) {
        //         if (permission.isGroupPermission()) {
        //             viewGroups.add(permission.getGroupName());
        //         }
        //     }
        // }
        // System.out.println("GOT VIEWGFROUPS");

        // for (String group : viewGroups) {
        //     if (restrictedGroups.contains(group)) {
        //         System.out.println("mandatory group present: " + group);
        //         System.out.println("user groups:  " + Arrays.toString(userGroups.toArray()));

        //         if (!userGroups.contains(group)) {
        //             System.out.println("user should NOT be able to see");
        //             return true;
        //         }
        //     }
        // }
        // System.out.println("RETURINING FALSE");
        // return false;
    }


    public boolean isRestrictedSpace(ConfluenceUser user, String spaceKey, List<String> restrictedGroups) {        
        System.out.println("checking if the space (from page) is restricted: " + space.getKey());
        for (String group : restrictedGroups) {

            if (this.spacePermissionManager.groupHasPermission("VIEWSPACE", )) 
        }
        // Check whether the restricted groups have permissions on the page
        // if not, we can use the default confluence permissions going forward
        // If so, check whether the user is part of present the restricted groups 
        List<String> spaceRestrictedGroups = new ArrayList<String>();
        List<SpacePermission> spacePermissions = space.getPermissions();
        System.out.println("restricted groups:  " + Arrays.toString(restrictedGroups.toArray()));

        for (SpacePermission permission : spacePermissions) {

            // System.out.println("space permission:  " + permission.toString());
            // System.out.println("space permission type:  " + permission.getType());
            // System.out.println("space permission group:  " + permission.getGroup());
            // System.out.println("is group permission:  " + permission.isGroupPermission());
            if (permission.isGroupPermission() && permission.getType().equals("VIEWSPACE")) {
                String group = permission.getGroup();
                if (restrictedGroups.contains(group)) {
                    spaceRestrictedGroups.add(group);

                    // System.out.println(" group in restricted groups:  " + group);
                }
                // System.out.println(" group not in restricted groups:  " + group);
            }
        }
        // for (String group : restrictedGroups) {
        //     if (spacePermissionManager.groupHasPermission(ContentPermission.VIEW_PERMISSION, space, group)) {
        //         spaceRestrictedGroups.add(group);        
        //         System.out.println("space has restricted group:  " + group);
        //     }
        //     System.out.println("space does not have restricted group:  " + group); 
        // }
        List<String> userGroups = this.userAccessor.getGroupNamesForUserName(user.getName());        
        // System.out.println("space restricted groups:  " + Arrays.toString(spaceRestrictedGroups.toArray()));

        // System.out.println("user groups:  " + Arrays.toString(userGroups.toArray()));

        // Filter permissions to get only view permissions
        // List<SpacePermission> viewPermissions = permissions.stream()
        //         .filter(permission -> SpacePermission.VIEWSPACE_PERMISSION.equals(permission.getType()))
        //         .toList();
        // //List<ContentPermissionSet> permissionList = contentPermissionManager.getContentPermissionSets(space, ContentPermission.VIEW_PERMISSION);
        // System.out.println("there are " + viewPermissions.size() + " sets of permissions here");
        // for (SpacePermission permission : viewPermissions) {
        //     System.out.println(permission.toString());
        // }
        // List<String> userGroups = this.userAccessor.getGroupNamesForUserName(user.getName());

        // List<String> viewGroups = new ArrayList<String>();
        // // Only group permissions are relevant here
        // for (ContentPermissionSet set : permissionList) {
        //     for (ContentPermission permission : set) {
        //         if (permission.isGroupPermission()) {
        //             viewGroups.add(permission.getGroupName());
        //         }
        //     }
        // }

        for (String group : spaceRestrictedGroups) {
            if (!userGroups.contains(group)) {
                System.out.println("user should NOT be able to see this restricted SPACE");
                return true;
            }
        }
        System.out.println("user can be able to see this  SPACE");
        return false;
    }
}
