package com.example.test.security.servlet;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.SpaceService;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.CommentManager;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import org.springframework.web.util.ContentCachingResponseWrapper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import javax.inject.Named;

@Named
public class BasicSearchServletFilter implements Filter{
    @ConfluenceImport
    private ContentService contentService;
    @ComponentImport
    private final UserAccessor userAccessor;
    @ComponentImport
    private final PageService pageService;
    @ComponentImport
    private final SpaceService spaceService;
    @ComponentImport
    private final SpaceManager spaceManager;
    @ComponentImport
    private final CommentManager commentManager;
    @ComponentImport
    private final AttachmentManager attachmentManager;

    private FilterConfig config;
    // private final String prefix = "/confluence";

    public BasicSearchServletFilter (
        ContentService contentService,
        UserAccessor userAccessor,
        PageService pageService,
        SpaceService spaceService,
        SpaceManager spaceManager,
        AttachmentManager attachmentManager,
        CommentManager commentManager
    ) {
        this.contentService = contentService;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.spaceService = spaceService;
        this.spaceManager = spaceManager;
        this.commentManager = commentManager;
        this.attachmentManager = attachmentManager;
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
        
        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();

        System.out.println("IN BASIC SEARCH");
        try {
            this.filterSearchResults(request, httpResponse, chain, loggedInUser);
            
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            chain.doFilter(request, response);
        }
    }

    private void filterSearchResults(
        ServletRequest request,
        HttpServletResponse httpResponse,
        FilterChain chain,
        ConfluenceUser loggedInUser
    ) throws IOException, ServletException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        chain.doFilter(request, responseWrapper);
        byte[] responseArray = responseWrapper.getContentAsByteArray();
        String responseStr = new String(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
        PrintWriter writer = responseWrapper.getWriter();
        int resultsCount = 0;
        System.out.println("+++++++++++++++++++ api search +++++++++++++++++++++++++");
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(responseStr);
        System.out.println(jsonElement.toString());
        JsonObject originalResponse = jsonElement.getAsJsonObject();
        JsonObject modifiedResponse = new JsonObject();
        
        try {
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
                        // Content includes Pages, Attachments, Comments etc.
                        if (resultObject.has("content")) {
                            contentObject = resultObject.getAsJsonObject("content");
                            if (contentObject.has("type")) {
                                String contentType = contentObject.getAsJsonPrimitive("type").getAsString();
                                if (contentObject.has("id")) {
                                    
                                    Long id = new Long(contentObject.getAsJsonPrimitive("id").getAsInt());
                                    Page page = null;
                                    switch(contentType) {
                                        case "page":
                                            page = pageService.getIdPageLocator(id).getPage();
                                            break;
                                        case "attachment":
                                            Attachment attachmentResult = attachmentManager.getAttachment(id);
                                            page = Utilities.getParentPage((ContentEntityObject) attachmentResult);
                                            break;
                                        case "comment":
                                            Comment commentResult = commentManager.getComment(id);
                                            page = Utilities.getParentPage((ContentEntityObject) commentResult);
                                            break;
                                        default:
                                            // For the other content types: Calendars, Space Calendars, Blog posts we do not want to return any of these results
                                            // thus we will leave the page object as null;
                                            continue;
                                    }

                                    if (page != null && 
                                            (
                                            !Utilities.isSpaceRestricted(page.getSpace(), loggedInUser, this.userAccessor, "VIEWSPACE") ||
                                            !Utilities.isContentRestricted((ContentEntityObject) page, loggedInUser, this.userAccessor, ContentPermission.VIEW_PERMISSION)
                                            )
                                    ) {
                                        continue;
                                    }
                                    
                                }
                                
                            }
                            
                        } else if (resultObject.has("space")) {
                            // the sidebar search will contain the id which we can easily use to find the Space object
                            contentObject = resultObject.getAsJsonObject("space");
                            if (contentObject.has("id")) {
                                int id = contentObject.getAsJsonPrimitive("id").getAsInt();
                                Space foundSpace = spaceManager.getSpace(id);
                                System.out.println("id is " + id);
                                if (
                                    !Utilities.isSpaceRestricted(foundSpace, loggedInUser, this.userAccessor, "VIEWSPACE")
                                ) {
                                    continue;
                                }
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
                                if (
                                    !Utilities.isSpaceRestricted(foundSpace, loggedInUser, this.userAccessor, "VIEWSPACE")
                                ) {
                                    continue;
                                }
                            }
                        }
                        resultsCount++;
                        resultsArray.add(result);
                    }
                    value = resultsArray;
                    
                    System.out.println("SET THE RESULTS ARAY    ");
                }
                // edit the final number of results returned
                else if (entry.getKey().equals("size") || entry.getKey().equals("totalSize") ) {
                    int newSize = resultsCount; 
                    value = new JsonPrimitive(newSize);
                }
                System.out.println("adding to modified response");
                // add the other parameters
                modifiedResponse.add(entry.getKey(), value);
            }
        } catch (Exception error) {
            System.out.println("Basic filter error");
            System.out.println(error.toString());

        }
        
        System.out.println("modified perms are:");
        responseWrapper.reset();
        System.out.println(modifiedResponse.toString());
        writer.write(modifiedResponse.toString());
        writer.flush();
        responseWrapper.copyBodyToResponse();
    }

}
