package com.example.test.events.listeners;

// import com.example.test.security.utils.Utilities;
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

import com.atlassian.confluence.event.events.permission.SpacePermissionEvent;
import com.atlassian.confluence.event.events.permission.SpacePermissionRemoveEvent;

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

    String mandatoryPermission = "confluence-administrators";       // this should be an admin account to allow for system fixing
    String allowedToRemovePriorityGroups = "confluence-administrators";     //Ideally should be the same as the mandatory permission
    List<String> priorityPermissions = new ArrayList<>();

    private List<SpacePermission> removedSpacePermissions = new ArrayList<>();

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

        // Add any priority permission groups here
        this.priorityPermissions.addAll(Arrays.asList("rl2"));
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
    // Space Permission Processing
    public void spacePermissionRemovedChecker(Space space, SpacePermission removedPermission) {
        ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
        /* DEBUG */System.out.println("--------++++++++======== LOGGED IN USER : "+loggedInUser.getName()+" ========++++++++--------");

        if(removedPermission.isGroupPermission()) {
            if(this.priorityPermissions.contains(removedPermission.getGroup())){
                if(!userAccessor.getGroupNames(loggedInUser).contains(this.allowedToRemovePriorityGroups)) {
                    /* DEBUG */System.out.println("--------++++++++======== UNAUTHED USER ATTEMPTED TO REMOVE PRIORITY GROUP, UNDOING ========++++++++--------");
                    space.addPermission(removedPermission);
                } else {
                    /* DEBUG */System.out.println("--------++++++++======== USER ALLOWED TO REMOVE PRIORITY GROUP, CONTINUING ========++++++++--------");
                }
            }
        }
    }
    public void updateSpacePermissions(Space space) {
        /* DEBUG */System.out.println("--------++++++++======== CHECKING PERMISSION GROUPS ========++++++++--------");
        List<SpacePermission> spacePermissions = space.getPermissions();

        // REMOVE THE PERMISSION OVERRIDE STUFFS
        // List<SpacePermission> updatingPriorityPermission = new ArrayList<>();
        // for (SpacePermission permission : spacePermissions) {
        //     if(permission.isGroupPermission()) {
        //         if(this.priorityPermissions.contains(permission.getGroup())) {
        //             /* DEBUG */System.out.println("--------++++++++======== FOUND PRIORITY PERMISSION ========++++++++--------");
        //             updatingPriorityPermission.add(permission);
        //         }
        //     }
        // }
        // if(!updatingPriorityPermission.isEmpty()) {
        //     /* DEBUG */System.out.println("--------++++++++======== REPLACING PERMISSIONS ========++++++++--------");
        //     space.removeAllPermissions();
        //     for (SpacePermission permission : updatingPriorityPermission) {
        //         /* DEBUG */System.out.println("--------++++++++======== ADDING PERMISSION : "+permission.toString()+" ========++++++++--------");
        //         space.addPermission(permission);
        //     }
        // }

        Boolean missingMandatoryPermission = true;
        for (SpacePermission permission : spacePermissions) {
            /* DEBUG */System.out.println("--------++++++++======== "+permission.toString()+" ========++++++++--------");
            if(permission.isGroupPermission()) {
                /* DEBUG */System.out.println("--------++++++++======== GROUP PERMISSION : "+permission.getGroup()+" ========++++++++--------");
                if(permission.getGroup().equals(this.mandatoryPermission)) {
                    /* DEBUG */System.out.println("--------++++++++======== FOUND MANDATORY PERMISSION GROUP ========++++++++--------");
                    missingMandatoryPermission = false;
                }
            }
        }
        if(missingMandatoryPermission) {
            /* DEBUG */System.out.println("--------++++++++======== MISSING MANDATORY PERMISSION GROUP ========++++++++--------");
            for (String type : SpacePermission.PERMISSION_TYPES) {
                space.addPermission(SpacePermission.createGroupSpacePermission(type, space, mandatoryPermission));
            }
        }

        /* DEBUG */System.out.println("--------++++++++======== DONE CHECKING PERMISSIONS ========++++++++--------");
    }

    //Content Permission Processing
    public void updateContentPermissions(ContentEntityObject content, ContentPermission updatedPermission) {
        /* DEBUG */System.out.println("--------++++++++======== UPDATING PERMISSION "+updatedPermission.toString()+" ========++++++++--------");

        // Get all existing content permissions
        List<String> permissionTypes = Arrays.<String>asList(ContentPermission.VIEW_PERMISSION, ContentPermission.EDIT_PERMISSION);
        List<ContentPermission> contentPermissions = new ArrayList<>();
        for(String type : permissionTypes) {
            for(ContentPermission permission : content.getContentPermissionSet(type)) {
                contentPermissions.add(permission);
            }
        }

        // Check what permission was updated, and how
        if(contentPermissions.contains(updatedPermission)) {//The permission was added
            // contentPermissionAddedProcessing(content, updatedPermission);
        } else {//The permission was removed
            contentPermissionRemovedProcessing(content, updatedPermission);
        }

        /* DEBUG */System.out.println("--------++++++++======== CHECKING MANDATORY PERMISSION EXISTS ========++++++++--------");
        //Check if the mandatory permission is part of the content permissions
        for(String type : permissionTypes) {
            Boolean isMissingType = true;
            for(ContentPermission permission : content.getContentPermissionSet(type)) {
                if(permission.isGroupPermission()) {
                    if(permission.getGroupName().equals(this.mandatoryPermission)) {
                        isMissingType = false;
                    }
                }
            }
            if(isMissingType) {
                /* DEBUG */System.out.println("--------++++++++======== RECREATING "+type+" PERMISSION ========++++++++--------");
                ContentPermission permission = ContentPermission.createGroupPermission(type, this.mandatoryPermission);
                /* DEBUG */System.out.println(permission.toString());
                if(updatedPermission.equals(permission)) {
                    permission = new ContentPermission(updatedPermission);
                    this.contentPermissionManager.removeContentPermission(updatedPermission);
                    /* DEBUG */System.out.println(permission.toString());
                    this.contentPermissionManager.addContentPermission(updatedPermission, content);
                } else {
                    this.contentPermissionManager.addContentPermission(permission, content);
                }
            }
        }
        /* DEBUG */System.out.println("--------++++++++======== END PERMISSION UPDATE PROCESS ========++++++++--------");
    }
    public void contentPermissionAddedProcessing(ContentEntityObject content, ContentPermission permissionAdded) {
        /* DEBUG */System.out.println("--------++++++++======== ATTEMPTING TO ADD PERMISSION : "+permissionAdded.toString()+" ========++++++++--------");
        Boolean containsPriorityPermission = false;
        List<String> permissionTypes = Arrays.<String>asList(ContentPermission.EDIT_PERMISSION, ContentPermission.VIEW_PERMISSION);
        List<ContentPermission> contentPermissions = new ArrayList<>();
        for(String type : permissionTypes) {
            for(ContentPermission permission : content.getContentPermissionSet(type)) {
                if(permission.isGroupPermission()) {
                    if(this.priorityPermissions.contains(permission.getGroupName())) {
                        containsPriorityPermission = true;
                    }
                }
                contentPermissions.add(permission);
            }
        }

        if(this.priorityPermissions.contains(permissionAdded.getGroupName())) {
            // Remove all other permissions and add this one
            for(ContentPermission permission : contentPermissions){
                if(
                    (permission.isGroupPermission()) &&
                    (!this.priorityPermissions.contains(permission.getGroupName())) &&
                    (!permission.getGroupName().equals(this.mandatoryPermission)) &&
                    (!permission.equals(permissionAdded))
                ) {
                    /* DEBUG */System.out.println("--------++++++++======== REMOVING GROUP PERMISSION : "+permission.toString()+" ========++++++++--------");
                    this.contentPermissionManager.removeContentPermission(permission);
                }
                if(permission.isUserPermission()){
                    /* DEBUG */System.out.println("--------++++++++======== REMOVING USER PERMISSION : "+permission.toString()+" ========++++++++--------");
                    this.contentPermissionManager.removeContentPermission(permission);
                }
            }
        } else if(containsPriorityPermission) {
            // Remove the permission attempting to be added
            this.contentPermissionManager.removeContentPermission(permissionAdded);
        }
    }
    public void contentPermissionRemovedProcessing(ContentEntityObject content, ContentPermission permissionRemoved) {
        /* DEBUG */System.out.println("--------++++++++======== REMOVING GROUP PERMISSION : "+permissionRemoved.toString()+" ========++++++++--------");
        if(this.mandatoryPermission.equals(permissionRemoved.getGroupName())) {
            /* DEBUG */System.out.println("--------++++++++======== ATTEMPTING TO REMOVE MANDATORY PERMISSION : "+permissionRemoved.toString()+", UNDOING ========++++++++--------");
            this.contentPermissionManager.removeContentPermission(permissionRemoved);
            content.addPermission(new ContentPermission(permissionRemoved));
        }
        if(this.priorityPermissions.contains(permissionRemoved.getGroupName())) {
            /* DEBUG */System.out.println("--------++++++++======== ATTEMPTING TO REMOVE PRIORITY PERMISSION : "+permissionRemoved.toString()+" ========++++++++--------");
            ConfluenceUser loggedInUser = AuthenticatedUserThreadLocal.get();
            /* DEBUG */System.out.println("--------++++++++======== LOGGED IN USER : "+loggedInUser.getName()+" ========++++++++--------");
            if(!this.userAccessor.getGroupNames(loggedInUser).contains(this.allowedToRemovePriorityGroups)) {
                /* DEBUG */System.out.println("--------++++++++======== UNAUTHED USER ATTEMPTED TO REMOVE PRIORITY GROUP, UNDOING ========++++++++--------");
                this.contentPermissionManager.addContentPermission(permissionRemoved, content);
            } else {
                /* DEBUG */System.out.println("--------++++++++======== USER ALLOWED TO REMOVE PRIORITY GROUP, CONTINUING ========++++++++--------");
            }
        }
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
            if(com.example.test.security.utils.Utilities.isContentRestricted(content, user, this.userAccessor, ContentPermission.VIEW_PERMISSION)) {
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

    @EventListener
    public void onSpacePermissionEvents(SpacePermissionRemoveEvent event) {
        System.out.println("--------++++++++======== SPACE PERMISSIONS EVENT ========++++++++--------");
        List<SpacePermission> updatedPermissions = (List<SpacePermission>) event.getPermissions();
        for(SpacePermission spacePermission : updatedPermissions) {
            try {
                System.out.println(spacePermission.toString());
                System.out.println(event.getSpace().getKey());
                spacePermissionRemovedChecker(event.getSpace(), spacePermission);
            } catch (Exception e) {
                System.out.println(e);
            }
            
        }
    }

    // ON CONTENT PERMISSIONS UPDATE (Filtering for page update events specifically)
    @EventListener
    public void onContentPermissionsUpdate(ContentPermissionEvent event) {
        System.out.println("--------++++++++======== CONTENT PERMISSION EVENT ========++++++++--------");
        updateContentPermissions(event.getContent(), event.getContentPermission());
        contentUpdateController(event.getContent().getType(), event.getContent().getContentId());
    }
    /* ---+++=== END EVENT LISTENERS ===+++--- */
}


/* ------------- ==================== NOTES ==================== ------------- 
 *
 * IMPROVEMENTS
 *    - Keep a list of all the content already looked through to prevent looking through
 *      content multiple times
 */