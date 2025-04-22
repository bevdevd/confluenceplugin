package com.example.test.security.servlet;

import com.example.test.security.utils.Utilities;

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

import org.springframework.web.util.ContentCachingResponseWrapper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.service.content.SpaceService.SpaceFinder;
import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.service.content.ContentService;

//deprecated
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;

import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.plugin.servlet.PluginHttpRequestWrapper;
import com.atlassian.sal.api.web.context.HttpContext;


@Named
public class RestrictSearchesServletFilter implements Filter{
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
    @ComponentImport
    private final NotificationManager notificationManager;

    private FilterConfig config;

    public RestrictSearchesServletFilter (
        ContentService contentService,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService,
        SpaceManager spaceManager,
        NotificationManager notificationManager
    ) {
        this.contentService = contentService;
        this.contentPermissionManager = contentPermissionManager;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;
        this.spaceManager = spaceManager;
        this.notificationManager = notificationManager;
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
        
        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        byte[] responseArray = responseWrapper.getContentAsByteArray();

        PrintWriter writer = responseWrapper.getWriter();

        try {
            if (
                uri.startsWith("rest/api/search") ||
                uri.startsWith("rest/searchv3/1.0/cqlSearch")
            ) {
                String responseStr = new String(responseArray,responseWrapper.getCharacterEncoding());
                int removedCount = 0;
                System.out.println("+++++++++++++++++++ api search +++++++++++++++++++++++++");
                JsonParser jsonParser = new JsonParser();
                JsonElement jsonElement = jsonParser.parse(responseStr);
                System.out.println(jsonElement.toString());
                JsonObject originalResponse = jsonElement.getAsJsonObject();
                JsonObject modifiedResponse = new JsonObject();
                //JsonArray results = originalResponse.getAsJsonArray("results");
                Set<Map.Entry<String,JsonElement>> originalParameters = originalResponse.entrySet();
                for (Map.Entry<String,JsonElement> entry : originalParameters) {
                    JsonElement value = entry.getValue();
                    if (entry.getKey().equals("results")) {
                        System.out.println("key is results");
                        JsonArray results = entry.getValue().getAsJsonArray();
                        JsonArray resultsArray = new JsonArray();
                        for (JsonElement result : results) {
                            // This is either a Page result or a Space result
                            JsonObject resultObject = result.getAsJsonObject();
                            JsonObject contentObject;
                            //Check for Page result
                            if (resultObject.has("content")) {
                                contentObject = resultObject.getAsJsonObject("content");
                                if (contentObject.has("id")) {
                                    int id = contentObject.getAsJsonPrimitive("id").getAsInt();
                                    Page page = pageService.getIdPageLocator(new Long(id)).getPage();
                                    System.out.println("id is " + id);
                                    if (Utilities.isContentRestricted((ContentEntityObject) page, loggedInUser, this.userAccessor, ContentPermission.VIEW_PERMISSION)) {
                                        removedCount++;
                                        continue;
                                    }
                                    // System.out.println("PAGE " + id + "IS NOT RESTRICTED");
                                }
                                resultsArray.add(result);
                                // System.out.println("ADDED page TI RESULTS ARRAY ");
                            } else if (resultObject.has("space")) {
                                // the sidebar search will contain the id which we can easily use to find the Space object
                                contentObject = resultObject.getAsJsonObject("space");
                                if (contentObject.has("id")) {
                                    int id = contentObject.getAsJsonPrimitive("id").getAsInt();
                                    Space foundSpace = spaceManager.getSpace(id);
                                    System.out.println("id is " + id);
                                    if (Utilities.isSpaceRestricted(foundSpace, loggedInUser, this.userAccessor, "VIEWSPACE")) {
                                        removedCount++;
                                        continue;
                                    }
                                    // System.out.println("SPACE " + id + "IS NOT RESTRICTED");
                                }
                            } else if (resultObject.has("entityType")) {
                                System.out.println("entityType is============================================================================= ");
                                String entityType = resultObject.getAsJsonPrimitive("entityType").getAsString();
                                System.out.println("entityType is " + entityType);
                                if (entityType.equals("space")) {
                                    String url = resultObject.getAsJsonPrimitive("url").getAsString();
                                    String resultSpaceKey = url.split("/display/")[1];
                                    System.out.println("spacekey is " + resultSpaceKey);
                                    Space foundSpace = spaceManager.getSpace(resultSpaceKey);
                                    if (Utilities.isSpaceRestricted(foundSpace, loggedInUser, this.userAccessor, "VIEWSPACE")) {
                                        removedCount++;
                                        continue;
                                    }
                                }
                            }
                            resultsArray.add(result);
                            System.out.println("ADDED space TI RESULTS ARRAY ");
                        }
                        value = resultsArray;
                        //modifiedResponse.add("results", resultsArray);
                        System.out.println("SET THE RESULTS ARAY    ");
                    }
                    // should probably change the total number of results returned
                    else if (entry.getKey().equals("size") || entry.getKey().equals("totalSize") ) {
                        int newSize = entry.getValue().getAsInt() - removedCount;
                        value = new JsonPrimitive(newSize);
                    }
                    System.out.println("adding to modified response");
                    // add the other parameters
                    modifiedResponse.add(entry.getKey(), value);
                }
                System.out.println("modified perms are:");
                responseWrapper.reset();
                System.out.println(modifiedResponse.toString());
                writer.write(modifiedResponse.toString());
                writer.flush();
                responseWrapper.copyBodyToResponse();
            } else {
                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }
    }
}

/*
    

    if (uri.startsWith("rest/api/search") || uri.startsWith("rest/searchv3/1.0/cqlSearch")) {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        byte[] responseArray = responseWrapper.getContentAsByteArray();
        String responseStr = new String(responseArray,responseWrapper.getCharacterEncoding());

        int removedCount = 0;
        System.out.println("+++++++++++++++++++ api search +++++++++++++++++++++++++");
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(responseStr);
        System.out.println(jsonElement.toString());
        JsonObject originalResponse = jsonElement.getAsJsonObject();
        JsonObject modifiedResponse = new JsonObject();
        //JsonArray results = originalResponse.getAsJsonArray("results");
        Set<Map.Entry<String,JsonElement>> originalParameters = originalResponse.entrySet();
        for (Map.Entry<String,JsonElement> entry : originalParameters) {
            JsonElement value = entry.getValue();
            if (entry.getKey().equals("results")) {
                System.out.println("key is results");
                JsonArray results = entry.getValue().getAsJsonArray();
                JsonArray resultsArray = new JsonArray();
                for (JsonElement result : results) {
                    // This is either a Page result or a Space result
                    JsonObject resultObject = result.getAsJsonObject();
                    JsonObject contentObject;
                    //Check for Page result
                    if (resultObject.has("content")) {
                        contentObject = resultObject.getAsJsonObject("content");
                        if (contentObject.has("id")) {
                            int id = contentObject.getAsJsonPrimitive("id").getAsInt();
                            System.out.println("id is " + id);
                            if (isRestrictedPage(loggedInUser, id, restrictedGroups)) {
                                removedCount++;
                                continue;
                            }
                            // System.out.println("PAGE " + id + "IS NOT RESTRICTED");
                        }
                        resultsArray.add(result);
                        // System.out.println("ADDED page TI RESULTS ARRAY ");
                    } else if (resultObject.has("space")) {
                        // the sidebar search will contain the id which we can easily use to find the Space object
                        contentObject = resultObject.getAsJsonObject("space");
                        if (contentObject.has("id")) {
                            int id = contentObject.getAsJsonPrimitive("id").getAsInt();
                            Space foundSpace = spaceManager.getSpace(id);
                            System.out.println("id is " + id);
                            if (isRestrictedSpace(loggedInUser, foundSpace, restrictedGroups)) {
                                removedCount++;
                                continue;
                            }
                            // System.out.println("SPACE " + id + "IS NOT RESTRICTED");
                        }
                    } else if (resultObject.has("entityType")) {
                        System.out.println("entityType is============================================================================= ");
                        String entityType = resultObject.getAsJsonPrimitive("entityType").getAsString();
                        System.out.println("entityType is " + entityType);
                        if (entityType.equals("space")) {
                            String url = resultObject.getAsJsonPrimitive("url").getAsString();
                            String resultSpaceKey = url.split("/display/")[1];
                            System.out.println("spacekey is " + resultSpaceKey);
                            Space foundSpace = spaceManager.getSpace(resultSpaceKey);
                            if (isRestrictedSpace(loggedInUser, foundSpace, restrictedGroups)) {
                                removedCount++;
                                continue;
                            }
                        }
                    }
                    resultsArray.add(result);
                    System.out.println("ADDED space TI RESULTS ARRAY ");
                }
                value = resultsArray;
                //modifiedResponse.add("results", resultsArray);
                System.out.println("SET THE RESULTS ARAY    ");
            }
            // should probably change the total number of results returned
            else if (entry.getKey().equals("size") || entry.getKey().equals("totalSize") ) {
                int newSize = entry.getValue().getAsInt() - removedCount;
                value = new JsonPrimitive(newSize);
            }
            System.out.println("adding to modified response");
            // add the other parameters
            modifiedResponse.add(entry.getKey(), value);
        }
        System.out.println("modified perms are:");
        responseWrapper.resetBuffer();
        System.out.println(modifiedResponse.toString());
        writer.write(modifiedResponse.toString());
        writer.flush();
    }

*/
