package com.example.test.security;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.setup.velocity.VelocityContextItemProvider;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.confluence.core.ContentEntityObject;
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
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.Method;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;


import org.springframework.stereotype.Component;
import javax.inject.Named;

@Component
public class ViewPageVelocityContext implements ContextProvider {
    String maxClassification = "unrestricted";

    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;
    @ComponentImport
    private final SpacePermissionManager spacePermissionManager;
    @ComponentImport
    private final SpaceManager spaceManager;

    public ViewPageVelocityContext(SpacePermissionManager spacePermissionManager, SpaceManager spaceManager, ContentPermissionManager contentPermissionManager) {
        this.spacePermissionManager = spacePermissionManager;
        this.spaceManager = spaceManager;
        this.contentPermissionManager = contentPermissionManager;
    }

    @Override
    public void init(Map<String, String> params) {
    }

    public List<String> aggregateClassifications(List<String> spaceRestrictions, List<String> pageRestrictions) {
        if (spaceRestrictions.size() == 1 && spaceRestrictions.contains("unrestricted")) {
            return pageRestrictions;
        } else if (pageRestrictions.size() == 1 && pageRestrictions.contains("unrestricted")){
            return spaceRestrictions;
        }
        Set<String> aggregatedClassifications = new HashSet<String>();
        aggregatedClassifications.addAll(spaceRestrictions);
        aggregatedClassifications.addAll(pageRestrictions);
        return new ArrayList<String>(aggregatedClassifications);
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> parameters) {
        
        maxClassification = "unrestricted";
        Map<String, Object> context = new HashMap<>();
        System.out.println("THE context IS _-----------------------------");
        System.out.println("Context keys: " + parameters.keySet());
        System.out.println("Context keys: " + parameters.get("space"));
        
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

        Set<String> restrictions = new HashSet<String>();
        List<String> spaceClassifications = new ArrayList<String>();
        List<String> pageClassifications= new ArrayList<String>();
        List<String> aggregatedClassifications= new ArrayList<String>();
        spaceClassifications.add("unrestricted");
        pageClassifications.add("unrestricted");
        aggregatedClassifications.add("unrestricted");

        try {
            // Try to get space information
            Method getSpaceMethod = action.getClass().getMethod("getSpace");
            Space space = (Space) getSpaceMethod.invoke(action);
            if (space != null) {
                System.out.println("space is: " + space);
                context.put("spaceName", space.getName());
                spaceClassifications = Utilities.getSpaceRestrictions(space);
            }

            // Try to get page information
            Method getPageMethod = action.getClass().getMethod("getPage");
            Page page = (Page) getPageMethod.invoke(action);
            if (page != null) {
                System.out.println("page is: " + page);

                context.put("pageName", page.getNameForComparison());
                pageClassifications = Utilities.getPageHierarchyRestrictions((ContentEntityObject) page);
            }
            aggregatedClassifications = aggregateClassifications(spaceClassifications, pageClassifications);

        } catch (NoSuchMethodException e) {
            System.err.println("getSpace method not found: " + e.getMessage());
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error invoking getSpace: " + e.getMessage());
        }

        System.out.println("aggregated classification is: " + aggregatedClassifications.toString());
        context.put("maxClassification", aggregatedClassifications);
        return context;
    }
}