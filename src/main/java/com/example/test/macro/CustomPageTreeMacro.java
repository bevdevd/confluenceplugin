package com.example.test.macro;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.content.service.space.DefaultSpaceService;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;

import com.atlassian.webresource.api.assembler.PageBuilderService;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.component.ComponentLocator;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class CustomPageTreeMacro implements Macro {

    @ComponentImport
    private final UserAccessor userAccessor;
    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final PageBuilderService pageBuilderService;

    public CustomPageTreeMacro(
        UserAccessor userAccessor,
        PageManager pageManager,
        PageBuilderService pageBuilderService
    ) {
        this.userAccessor = userAccessor;
        this.pageManager = pageManager;
        this.pageBuilderService = pageBuilderService;
    }

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {
        pageBuilderService.assembler().resources().requireWebResource("com.example.test.plugin:plugin-resources");

        DefaultSpaceService defaultSpaceService = ComponentLocator.getComponent(DefaultSpaceService.class);
        String spaceKey = map.get("SpaceKey");
        Space space = defaultSpaceService.getKeySpaceLocator(spaceKey).getSpace();

        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();

        String pageTreeHtml = "<div>";

        if(
            !(space == null)
        ) {
            pageTreeHtml += getPageList(space, loggedInUser);
        } else {
            pageTreeHtml +=  "<h3>Page tree is unavailable in this context</h3><br/><p>please open a space or a page for further navigation.</p>";
        }
        pageTreeHtml += "</div>";

        return pageTreeHtml;
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    private String getPageList(Space space, ConfluenceUser user){
        List<Page> topLevelPages = this.pageManager.getTopLevelPages(space);
        if(topLevelPages == null) {
            return "<p>No pages to display, get started by creating one.</p>";
        }
        return getPageList(topLevelPages, user, true, null);
    }

    private String getPageList(List<Page> pageList, ConfluenceUser user, Boolean topLevel, String id) {
        // Attempt to sort the pages by their set position, otherwise default to page ID
        Collections.sort(pageList, new Comparator<Page>() {
            @Override
            public int compare(Page p1, Page p2) {
                if((p1.getPosition() == null) || (p2.getPosition() == null)) {
                    return (int)(p1.getContentId().asLong()-p2.getContentId().asLong());
                }
                return (int)(p1.getPosition()-p2.getPosition());
            }
        });
        
        
        String htmlPageList = "<ul>";
        if(!topLevel){
            htmlPageList = "<ul id="+id+" class='childList'>";
        }

        for(Page page : pageList) {
            String pageId = Long.toString(page.getContentId().asLong());
            if(
                Utilities.isSpaceRestricted(page.getSpace(), user, this.userAccessor, "VIEWSPACE") &&
                Utilities.isContentRestricted((ContentEntityObject) page, user, this.userAccessor, ContentPermission.VIEW_PERMISSION)
            ) {
                if(page.hasChildren()){
                    htmlPageList += "<li class='macroListItem'><span class='collapsible' id=button_"+pageId+" onClick='hide(this, "+pageId+")'>+ </span><a href='/confluence/pages/viewpage.action?pageId="+pageId+"'>"+page.getTitle()+"</a>";
                    htmlPageList += getPageList(page.getChildren(), user, false, pageId);
                } else {
                    htmlPageList += "<li>• <a href='/confluence/pages/viewpage.action?pageId="+pageId+"'>"+page.getTitle()+"</a>";
                }
                htmlPageList += "</li>";
            } else{
                continue;
            }
        }
        htmlPageList += "</ul>";

        return htmlPageList;
    }
}
