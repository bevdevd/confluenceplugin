package com.example.test.events.listeners;

import com.example.test.utils.Utilities;

/* UTILITY IMPORTS */
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

import com.atlassian.confluence.api.service.content.ContentService;
import com.atlassian.confluence.api.service.content.ContentService.ContentFetcher;
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

import com.atlassian.plugin.ModuleCompleteKey;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.core.ContentEntityObject;

import com.atlassian.confluence.content.service.PageService;
import com.atlassian.confluence.content.service.BlogPostService;
import com.atlassian.confluence.api.service.content.SpaceService;


import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;

import com.atlassian.confluence.mail.notification.NotificationManager;
import com.atlassian.confluence.mail.notification.Notification;

import com.atlassian.confluence.labels.Label;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.ThreadLocal;
/* END UTILITY IMPORTS */
/* EVENT IMPORTS */
import com.atlassian.confluence.event.events.content.mail.notification.NotificationEvent;
import com.atlassian.confluence.event.events.content.mail.notification.ContentNotificationAddedEvent;
import com.atlassian.confluence.event.events.content.mail.notification.SiteNotificationAddedEvent;
import com.atlassian.confluence.event.events.content.mail.notification.SpaceNotificationAddedEvent;

import com.atlassian.confluence.event.events.content.page.PageEvent;
import com.atlassian.confluence.event.events.content.page.PageMoveEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;

import com.atlassian.confluence.event.events.content.blogpost.BlogPostEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostMovedEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostUpdateEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;

import com.atlassian.confluence.event.events.security.ContentPermissionEvent;

import com.atlassian.confluence.event.events.space.SpaceEvent;
import com.atlassian.confluence.event.events.space.SpacePermissionsUpdateEvent;
import com.atlassian.confluence.event.events.space.SpaceUpdateEvent;
import com.atlassian.confluence.event.events.space.SpaceCreateEvent;
import com.atlassian.confluence.event.events.space.SpaceArchivedEvent;

import com.atlassian.confluence.event.events.user.UserEvent;
import com.atlassian.confluence.event.events.user.UserDeactivateEvent;

/* END EVENT IMPORTS */

@Named
public class EventsListener implements InitializingBean, DisposableBean {

    /* Essential Setup */
    @ConfluenceImport
    private EventPublisher eventPublisher;
    @ConfluenceImport
    private ContentService contentService;
    @ComponentImport
    private final NotificationManager notificationManager;
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

    private static final ModuleCompleteKey PERMISSIONS_KEY =    new ModuleCompleteKey("com.plugins.permissions.listener", "confluence-permissions");
    private static final Logger log = LoggerFactory.getLogger(EventsListener.class);

    private Boolean isSpaceWatchEvent = false;

    private Utilities utilities;

    @Inject
    public EventsListener (
        EventPublisher eventPublisher,
        ContentService contentService,
        NotificationManager notificationManager,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor,
        PageService pageService,
        BlogPostService blogPostService,
        SpaceService spaceService
    ) {
        this.eventPublisher = eventPublisher;
        this.contentService = contentService;
        this.notificationManager = notificationManager;
        this.contentPermissionManager = contentPermissionManager;
        this.userAccessor = userAccessor;
        this.pageService = pageService;
        this.blogPostService = blogPostService;
        this.spaceService = spaceService;

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
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }
    /* End Essential Setup */

    /* ---+++=== PERMISSIONS PROCESSING ===+++--- */
    public void updateSpacePermissions(Space space) {
        System.out.println("--------++++++++======== CHECKING PERMISSION GROUPS ========++++++++--------");
        List<SpacePermission> spacePermissions = space.getPermissions();

        // Replace with the restriction/class level you want to overwrite all others
        String mandatoryPermission = "confluence-administrators";       // this should be an admin account to allow for system fixing
        List<String> priorityPermissions = new ArrayList<>();
        priorityPermissions.addAll(Arrays.asList("rl2"));    // mandatoryPermission should always be a part of this array to avoid it getting removed

        List<SpacePermission> replacePermissions = new ArrayList<>();
        Boolean replacePermissionsReq = false;
        for (SpacePermission permission : spacePermissions) {
            if(permission.isGroupPermission()) {
                if(priorityPermissions.contains(permission.getGroup())) {
                    if(permission.getGroup() != mandatoryPermission) {
                        System.out.println("--------++++++++======== FOUND PRIORITY PERMISSION ========++++++++--------");
                        replacePermissionsReq = true;
                    }
                    replacePermissions.add(permission);
                }
            }
        }
        if(replacePermissionsReq) {
            System.out.println("--------++++++++======== REPLACING PERMISSIONS ========++++++++--------");
            space.removeAllPermissions();
            for (SpacePermission permission : replacePermissions) {
                System.out.println("--------++++++++======== ADDING PERMISSION : "+permission.toString()+" ========++++++++--------");
                space.addPermission(permission);
            }
        }

        Boolean missingMandatoryPermission = true;
        for (SpacePermission permission : spacePermissions) {
            System.out.println("--------++++++++======== "+permission.toString()+" ========++++++++--------");
            if(permission.isGroupPermission()) {
                System.out.println("--------++++++++======== GROUP PERMISSION : "+permission.getGroup()+" ========++++++++--------");
                if(permission.getGroup() == mandatoryPermission) {
                    System.out.println("--------++++++++======== FOUND MANDATORY PERMISSION ========++++++++--------");
                    missingMandatoryPermission = false;
                }
            }
        }
        if(missingMandatoryPermission) {
            System.out.println("--------++++++++======== MISSING THE MANDATORY PERMISSION GROUP ========++++++++--------");
            for (String type : SpacePermission.PERMISSION_TYPES) {
                space.addPermission(SpacePermission.createGroupSpacePermission(type, space, mandatoryPermission));
            }
        }

        System.out.println("--------++++++++======== DONE CHECKING PERMISSIONS ========++++++++--------");
    }
    /* ---+++=== END PERMISSIONS PROCESSING ===+++--- */

    /* ---+++=== NOTIFICATION (WATCHER) PROCESSING ===+++--- */
    public void pageNotificationCull(Page page) {
        // get a list of all watchers on the page and remove if necesary
        for(Notification notification : notificationManager.getNotificationsByContent(page)) {
            /* DEBUG */System.out.println("--------++++++++======== CHECKING IF USER "+notification.getReceiver().getName()+" SHOULD WATCH PAGE ========++++++++--------");
            removeContentNotification(
                notification,
                page,
                notification.getReceiver()
            );
        }
        // check if page has children
        if(page.hasChildren()) {
            /* DEBUG */System.out.println("--------++++++++======== REPEATING ON CHILD PAGES ========++++++++--------");
            // if so, loop through all children repeating this function
            for(Page child : page.getChildren()) {
                pageNotificationCull(child);
            }
        }
        // end function (this will end the recursion)
        return;
    }
    public void blogPostNotificationCull(BlogPost blogPost) {
        // get a list of all watchers on the page and remove if necesary
        for(Notification notification : notificationManager.getNotificationsByContent(blogPost)) {
            /* DEBUG */System.out.println("--------++++++++======== CHECKING IF USER "+notification.getReceiver().getName()+" SHOULD WATCH BLOGPOST ========++++++++--------");
            removeContentNotification(
                notification,
                blogPost,
                notification.getReceiver()
            );
        }
    }
    public void cullSpaceChildContentWatchers(ContentType type, com.atlassian.confluence.api.model.content.Space space) {
        int start = 0;
        int limit = 50;
        Boolean repeat = true;
        do {
            PageRequest request = new SimplePageRequest(start, limit);
            PageResponse<Content> spaceContent = contentService.find()
                .withSpace(space)
                .fetchMany(type, request);

            for(Content content : spaceContent.getResults()) {
                contentUpdateController(content.getType().getType(), content.getId());
            }
            start += limit;
            repeat = spaceContent.hasMore();
        }while(repeat);
    }

    public void removeContentNotification(Notification notification, ContentEntityObject content, ConfluenceUser user) {
        if(!this.isSpaceWatchEvent){
            List<String> userPermissionGroups = this.userAccessor.getGroupNamesForUserName(user.getName());
            if(utilities.isContentRestricted(content, userPermissionGroups)) {
                /* DEBUG */System.out.println("--------++++++++======== WATCHING THIS CONTENT IS RESTRICTED TO THIS USER, REMOVING ========++++++++--------");
                notificationManager.removeNotification(notification);
            }
        }
    }

    public void watchSpaceChildrenContent(Space space, ConfluenceUser user) {
        /* DEBUG */System.out.println("--------++++++++========  ATTEMPTING TO WATCH SPACE CHILDREN ========++++++++--------");
        this.isSpaceWatchEvent = false;
        // Get all children content of a space
        List<ContentEntityObject> spaceContent = new ArrayList<>();
        spaceContent.addAll(utilities.getSpaceChildrenContent(ContentType.PAGE, space));
        spaceContent.addAll(utilities.getSpaceChildrenContent(ContentType.BLOG_POST, space));

        // add notification to child object
        for(ContentEntityObject content : spaceContent) {
            /* DEBUG */System.out.println("--------++++++++========  CHILD : "+content.getContentId().toString()+" ========++++++++--------");
            notificationManager.addContentNotification(user, content);
            //this will trigger a notification added event, which will cause the processing
            //setup for each content type to automatically remove any restricted watches
        }
    }
    /* ---+++=== END NOTIFICATION (WATCHER) PROCESSING ===+++--- */
    
    /* ---+++=== CONTROLLER FUNCTIONS ===+++--- */
    public void contentUpdateController(String type, ContentId id) {
        switch(type) {
            case "page" :
                Page page = pageService.getIdPageLocator(id.asLong()).getPage();
                /* DEBUG */System.out.println("--------++++++++======== PAGE IS "+page.getContentId().toString()+" ========++++++++--------");
                pageNotificationCull(page);
                break;
            case "blogpost" :
                BlogPost blogPost = blogPostService.getIdBlogPostLocator(id.asLong()).getBlogPost();
                /* DEBUG */System.out.println("--------++++++++======== PAGE IS "+blogPost.getContentId().toString()+" ========++++++++--------");
                blogPostNotificationCull(blogPost);
                break;
        }
    }
    /* ---+++=== END CONTROLLER FUNCTIONS ===+++--- */

    /* ---+++=== EVENT LISTENERS ===+++--- */
    @EventListener
    public void onContentNotificationAddedEvent(ContentNotificationAddedEvent event) {
        /* DEBUG */System.out.println("--------++++++++========  CONTENT NOTIFICATION EVENT ========++++++++--------");
        Notification notification = event.getNotification();
        removeContentNotification(notification, notification.getContent(), notification.getReceiver());
    }
    @EventListener
    public void onSiteNotificationAddedEvent(SiteNotificationAddedEvent event) {
        /* DEBUG */System.out.println("--------++++++++========  SITE NOTIFICATION EVENT ========++++++++--------");
    }
    @EventListener
    public void onSpaceNotificationAddedEvent(SpaceNotificationAddedEvent event) {
        this.isSpaceWatchEvent = true;
        /* DEBUG */System.out.println("--------++++++++========  SPACE NOTIFICATION EVENT ========++++++++--------");
        Notification notification = event.getNotification();
        notificationManager.removeNotification(notification);
        watchSpaceChildrenContent(notification.getSpace(), notification.getReceiver());
        this.isSpaceWatchEvent = false;
    }

    // ON CONTENT PERMISSIONS UPDATE (Filtering for page update events specifically)
    @EventListener
    public void onContentPermissionsUpdate(ContentPermissionEvent event) {
        /* DEBUG */System.out.println("--------++++++++======== PERMISSIONS EVENT ========++++++++--------");
        contentUpdateController(event.getContent().getType(), event.getContent().getContentId());
    }

    // ON PAGE EVENTS (Filtering for move, update and created events specifically)
    @EventListener
    public void onPageEvents(PageEvent event) {
        if (
            (event instanceof PageMoveEvent) ||
            (event instanceof PageUpdateEvent) ||
            (event instanceof PageCreateEvent)
        ) {
            /* DEBUG */System.out.println("--------++++++++======== PAGE UPDATE EVENT ========++++++++--------");
            pageNotificationCull(event.getPage());
        }
    }
    //ON BLOGPOST EVENTS (Filtering for Move, update and created events specifically)
    @EventListener
    public void onBlogPostEvents (BlogPostEvent event) {
        if(
            (event instanceof BlogPostMovedEvent) ||
            (event instanceof BlogPostUpdateEvent) ||
            (event instanceof BlogPostCreateEvent)
        ) {
            /* DEBUG */System.out.println("--------++++++++======== BLOGPOST UPDATE EVENT ========++++++++--------");
            blogPostNotificationCull(event.getBlogPost());
        }
    }
    // ON SPACE EVENTS (Filtering for Space update, create, permissions update, and archived events specifically)
    @EventListener
    public void onSpaceEvents(SpaceEvent event) { 
        if(
            (event instanceof SpacePermissionsUpdateEvent)
        ){ // Update space permissions
            updateSpacePermissions(event.getSpace());
        }

        if(
            (event instanceof SpacePermissionsUpdateEvent) ||
            (event instanceof SpaceUpdateEvent) ||
            (event instanceof SpaceCreateEvent)
        ) { // Update Children Page Watchers
            com.atlassian.confluence.api.model.content.Space space = spaceService.find()
                .withKeys(event.getSpace().getKey())
                .fetch().get();

            cullSpaceChildContentWatchers(ContentType.PAGE, space);
            cullSpaceChildContentWatchers(ContentType.BLOG_POST, space);
        }

        if(
            (event instanceof SpaceArchivedEvent)
        ) { // Remove all watchers of a space once it becomes archived
            notificationManager.removeAllNotificationsForSpace(event.getSpace());
        }
    }
    /* ---+++=== END EVENT LISTENERS ===+++--- */
}


/* ------------- ==================== NOTES ==================== ------------- 
 *
 * IMPROVEMENTS
 *    - Keep a list of all the content already looked through to prevent looking through
 *      content multiple times
 */