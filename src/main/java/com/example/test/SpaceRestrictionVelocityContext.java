package com.example.test;

import com.atlassian.confluence.setup.velocity.VelocityContextItemProvider;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;


import com.atlassian.confluence.pages.actions.AbstractPageAwareAction;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;




import java.lang.reflect.Method;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;



import org.springframework.stereotype.Component;
import javax.inject.Named;

@Component
public class SpaceRestrictionVelocityContext implements ContextProvider {
    String[] mandatoryPermissions = {"restricted", "highlyRestricted"};
    String maxClassification = "unrestricted";

    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;
    @ComponentImport
    private final SpacePermissionManager spacePermissionManager;
    @ComponentImport
    private final SpaceManager spaceManager;

    public SpaceRestrictionVelocityContext(SpacePermissionManager spacePermissionManager, SpaceManager spaceManager, ContentPermissionManager contentPermissionManager) {
        this.spacePermissionManager = spacePermissionManager;
        this.spaceManager = spaceManager;
        this.contentPermissionManager = contentPermissionManager;
    }

    @Override
    public void init(Map<String, String> params) {

    }
    public void updateClassification(Space currentSpace) {
        if (!maxClassification.equals("restricted")) {

            List<SpacePermission> spacePermissions = currentSpace.getPermissions();

            for (String mandatoryPermission : mandatoryPermissions) {
                for (SpacePermission permission : spacePermissions) {
                    if (permission.isGroupPermission() && permission.getType().equals("VIEWSPACE")) {
                        String group = permission.getGroup();

                        if (Arrays.stream(mandatoryPermissions).anyMatch(group::equals)) {
                            maxClassification = "restricted";
                        }
                    }
                }
            }
        }
    }
    
    public void updateClassification(Page currentPage) {
        if (!maxClassification.equals("restricted")) {

            List<ContentPermissionSet> permissionList = contentPermissionManager.getContentPermissionSets(currentPage, ContentPermission.VIEW_PERMISSION);

            for (ContentPermissionSet permissionSet : permissionList) {
                for (ContentPermission permission : permissionSet) {
                    if (permission.isGroupPermission()) {
                        if (permission.getGroupName().equals("restricted")) {
                            maxClassification = "restricted";
                        }
                    }
                }
            
            }
        }
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> parameters) {
        
        maxClassification = "unrestricted";
        Map<String, Object> context = new HashMap<>();
        System.out.println("THE context IS _-----------------------------");
        System.out.println("Context keys: " + parameters.keySet());
        
        System.out.println("current page is: " + parameters.get("action"));


        // Get SpaceDecoratorAction from the context
        Object action = parameters.get("action");
        
        System.out.println("current page type is: " + action.getClass());
        

        // Print all methods using reflection
        if (action != null) {
            System.out.println("Action methods:");
            for (Method method : action.getClass().getMethods()) {
                System.out.println(method.getName() + "()");
            }
        }

        try {
            // Try to get space information
            Method getSpaceMethod = action.getClass().getMethod("getSpace");
            Space space = (Space) getSpaceMethod.invoke(action);
            if (space != null) {
                System.out.println("space is: " + space);
                context.put("spaceName", space.getName());
                updateClassification(space);
            }

            // Try to get page information
            Method getPageMethod = action.getClass().getMethod("getPage");
            Page page = (Page) getPageMethod.invoke(action);
            if (page != null) {
                System.out.println("page is: " + page);

                context.put("pageName", page.getNameForComparison());
                updateClassification(page);
            }

        } catch (NoSuchMethodException e) {
            System.err.println("getSpace method not found: " + e.getMessage());
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error invoking getSpace: " + e.getMessage());
        }

        

        System.out.println("current classification is: " + maxClassification);
        context.put("maxClassification", maxClassification);
        return context;
    }
}

