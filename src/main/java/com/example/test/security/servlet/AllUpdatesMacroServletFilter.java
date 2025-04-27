package com.example.test.security.servlet;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.SpaceService;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.CommentManager;
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
public class AllUpdatesMacroServletFilter implements Filter{
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

    public AllUpdatesMacroServletFilter (
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
        String uri = httpRequest.getRequestURI();
        
        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        System.out.println("logged in user" + loggedInUser);
        System.out.println("IN UPDATES MACRO");

        System.out.println("=======================================================");
        System.out.println("fetching latest updates");
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        String modifiedResponseContent;
        PrintWriter writer = responseWrapper.getWriter();
        chain.doFilter(request, responseWrapper);

        // build the changed object
        JsonObject modifiedObject = new JsonObject();
        
        try {
            byte[] responseArray = responseWrapper.getContentAsByteArray();
            String responseStr = new String(responseArray, responseWrapper.getCharacterEncoding());
            System.out.println("response string: " + responseStr);
            
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(responseStr);
    
            JsonObject originalObject = jsonElement.getAsJsonObject();

            Set<Map.Entry<String,JsonElement>> originalUpdatesSet = originalObject.entrySet();
            for (Map.Entry<String,JsonElement> entry : originalUpdatesSet) {
                JsonElement value = entry.getValue();
                JsonArray modifiedChangeSetsArray= new JsonArray();
                if (entry.getKey().equals("changeSets")) {
                    System.out.println("key is changesets");
                    JsonArray changeSetArray = entry.getValue().getAsJsonArray();

                    for (JsonElement changeSetEntry : changeSetArray) {
                        JsonObject resultObject = changeSetEntry.getAsJsonObject();
                        JsonArray recentUpdatesArray = resultObject.getAsJsonArray("recentUpdates");
                        boolean hasValidUpdates = false;
                        JsonArray modifiedUpdateArray = new JsonArray();
                        for (JsonElement recentUpdate : recentUpdatesArray) {
                            JsonObject recentUpdateObject = recentUpdate.getAsJsonObject();
                            String type = recentUpdateObject.getAsJsonPrimitive("contentType").getAsString();

                            System.out.println("type is " + type);
                            System.out.println("logged in user" + loggedInUser);
                            // check permissions on the page results
                            if (type.equals("page")) {
                                if (recentUpdateObject.has("id")) {
                                    int id = recentUpdateObject.getAsJsonPrimitive("id").getAsInt();

                                    Page page = pageService.getIdPageLocator(new Long(id)).getPage();
                                    System.out.println("page  is " + page.toString());

                                    if (page != null && (   
                                            !Utilities.isSpaceRestricted(page.getSpace(), loggedInUser, this.userAccessor, "VIEWSPACE") &&
                                            !Utilities.isContentRestricted((ContentEntityObject) page, loggedInUser, this.userAccessor, ContentPermission.VIEW_PERMISSION)
                                            )
                                        ) {
                                        hasValidUpdates = true;
                                        System.out.println("page is not restricted");
                                        modifiedUpdateArray.add(recentUpdateObject);
                                    } else {
                                        System.out.println("page is restricted");
                                    }
                                }
                            } else if (type.equals("attachment")) {
                                if (recentUpdateObject.has("id")) {
                                    int attachmentId = recentUpdateObject.getAsJsonPrimitive("id").getAsInt();
                                    System.out.println("attachment id is " + attachmentId);
                                    Attachment attachment = attachmentManager.getAttachment(attachmentId);
                                    long pageId = attachment.getContainer().getId();
                                    System.out.println("page id  is " + pageId);
                                    Page page = pageService.getIdPageLocator(new Long(pageId)).getPage();
                                    System.out.println("page  is " + page.toString());
                                    if (page != null && (   
                                        !Utilities.isSpaceRestricted(page.getSpace(), loggedInUser, this.userAccessor, "VIEWSPACE") &&
                                        !Utilities.isContentRestricted((ContentEntityObject) page, loggedInUser, this.userAccessor, ContentPermission.VIEW_PERMISSION)
                                        )
                                    ) {

                                        hasValidUpdates = true;
                                        System.out.println("attachment is not restricted");
                                        modifiedUpdateArray.add(recentUpdateObject);
                                    } else {
                                        System.out.println("attachment is restricted ");
                                    }
                                }
                            } else if (type.equals("comment")) {
                                if (recentUpdateObject.has("id")) {
                                    int id = recentUpdateObject.getAsJsonPrimitive("id").getAsInt();
                                    System.out.println("comment id is " + id);
                                    Comment commentResult = commentManager.getComment(new Long(id));
                                    Page page = Utilities.getParentPage((ContentEntityObject) commentResult);
                                    System.out.println("page  is " + page.toString());

                                    if (page != null && (   
                                        !Utilities.isSpaceRestricted(page.getSpace(), loggedInUser, this.userAccessor, "VIEWSPACE") &&
                                        !Utilities.isContentRestricted((ContentEntityObject) page, loggedInUser, this.userAccessor, ContentPermission.VIEW_PERMISSION)
                                        )
                                    ) {
                                        hasValidUpdates = true;
                                        System.out.println("comment is not restricted");
                                        modifiedUpdateArray.add(recentUpdateObject);
                                    } else {
                                        System.out.println("comment is restricted ");
                                    }
                                }
                            } 
                        }
                        if (hasValidUpdates) {
                            JsonObject changeSetEntryModified = new JsonObject();
                            Set<Map.Entry<String,JsonElement>> userUpdatesSet = resultObject.entrySet();
                            for (Map.Entry<String,JsonElement> userUpdateObject : userUpdatesSet) {
                                //JsonObject userObject = userUpdateObject.getValue();
                                if ("recentUpdates".equals(userUpdateObject.getKey())) {
                                    changeSetEntryModified.add(userUpdateObject.getKey(), modifiedUpdateArray);
                                } else {
                                    changeSetEntryModified.add(userUpdateObject.getKey(), userUpdateObject.getValue());
                                }
                            }
                            modifiedChangeSetsArray.add(changeSetEntryModified);
                        }                        
                    }
                }
                JsonElement topLevelValue = entry.getValue();
                // replace the changesets object with the modified one
                if (entry.getKey().equals("changeSets")) {
                    topLevelValue = modifiedChangeSetsArray;
                }

                // add the other parameters
                modifiedObject.add(entry.getKey(), topLevelValue);
            }
    
        } catch (Exception e) {
            System.out.println("unfortunately, we have errored: " + e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        System.out.println("printing out the response to the updates macro");
        System.out.println(modifiedObject.toString());
        responseWrapper.resetBuffer();
        
        writer.write(modifiedObject.toString());
        writer.flush();
        responseWrapper.copyBodyToResponse();

    }

}
