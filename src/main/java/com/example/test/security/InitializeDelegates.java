package com.example.test.security;

import com.example.test.security.delegates.NewPagePermissionsDelegate;
import com.example.test.security.delegates.NewLinkPermissionsDelegate;

import com.atlassian.confluence.security.DefaultPermissionManager;
import com.atlassian.confluence.security.delegate.PagePermissionsDelegate;
import com.atlassian.confluence.security.delegate.SharedAccessInterceptor;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.DefaultSpacePermissionManager;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.core.DefaultContentPermissionManager;

import com.atlassian.confluence.user.UserAccessor;

import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.confluence.pages.Page;

import java.lang.reflect.Field;
import java.util.Map;
import com.atlassian.confluence.security.PermissionDelegate;
import com.atlassian.confluence.security.DefaultPermissionManager;
import com.atlassian.confluence.security.PermissionManager;
import javax.inject.Named;

import com.atlassian.sal.api.component.ComponentLocator;

@Component 
@Named
public class InitializeDelegates {
    @ComponentImport
    private PermissionManager permissionManager;
    @ComponentImport
    private final SpacePermissionManager spacePermissionManager;
    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;
    @ComponentImport
    private final UserAccessor userAccessor;

    public InitializeDelegates(
        PermissionManager permissionManager,
        SpacePermissionManager spacePermissionManager,
        ContentPermissionManager contentPermissionManager,
        UserAccessor userAccessor
    ) {
        this.permissionManager = permissionManager;
        this.contentPermissionManager = contentPermissionManager;
        this.spacePermissionManager = spacePermissionManager;
        this.userAccessor = userAccessor;
    }

    @PostConstruct
    public void printDelegateKeyPairs() {
        try {
            DefaultPermissionManager defaultPermissionManager = ComponentLocator.getComponent(DefaultPermissionManager.class);
            DefaultContentPermissionManager defaultContentPermissionManager = ComponentLocator.getComponent(DefaultContentPermissionManager.class);

            printDelegateKeyPairs(defaultPermissionManager);
            
            NewPagePermissionsDelegate pagePermissionsDelegate = new NewPagePermissionsDelegate();
            pagePermissionsDelegate.setSpacePermissionManager(this.spacePermissionManager);
            pagePermissionsDelegate.setContentPermissionManager(defaultContentPermissionManager);
            pagePermissionsDelegate.setUserAccessor(this.userAccessor);

            NewLinkPermissionsDelegate linkPermissionsDelegate = new NewLinkPermissionsDelegate();
            linkPermissionsDelegate.setUserAccessor(this.userAccessor);

            defaultPermissionManager.register("Page", new SharedAccessInterceptor(pagePermissionsDelegate));
            defaultPermissionManager.register("OutgoingLink", new SharedAccessInterceptor(linkPermissionsDelegate));
            printDelegateKeyPairs(defaultPermissionManager);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printDelegateKeyPairs(DefaultPermissionManager defaultPermissionManager) {
        try {
            System.out.println("-----------++++++++++++++================= Printing Delegate and Key Pairs =================++++++++++++++-----------");
        Field registryField = defaultPermissionManager.getClass().getDeclaredField("delegates");
        registryField.setAccessible(true);

        Map<String, PermissionDelegate> delegates = (Map<String, PermissionDelegate>) registryField.get(defaultPermissionManager);
        for (Map.Entry<String, PermissionDelegate> entry : delegates.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " -> Delegate: " + entry.getValue().getClass().getName());
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}