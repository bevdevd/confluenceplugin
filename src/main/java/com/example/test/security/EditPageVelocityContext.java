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

import org.springframework.stereotype.Component;
import javax.inject.Named;

@Component
public class EditPageVelocityContext implements ContextProvider {
    String[] mandatoryPermissions = {"restricted", "highlyRestricted"};
    String maxClassification = "unrestricted";

    @ComponentImport
    private final ContentPermissionManager contentPermissionManager;
    @ComponentImport
    private final SpacePermissionManager spacePermissionManager;
    @ComponentImport
    private final SpaceManager spaceManager;

    public EditPageVelocityContext(SpacePermissionManager spacePermissionManager, SpaceManager spaceManager, ContentPermissionManager contentPermissionManager) {
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

        Set<String> restrictions = new HashSet<String>();
        List<String> spaceClassifications = new ArrayList<String>();
        List<String> pageClassifications= new ArrayList<String>();
        List<String> aggregatedClassifications= new ArrayList<String>();
        spaceClassifications.add("unrestricted");
        pageClassifications.add("unrestricted");
        aggregatedClassifications.add("unrestricted");
        try {
            // Try to get space information
            Space space = (Space) parameters.get("space");
            if (space != null) {
                System.out.println("space is: " + space);
                context.put("spaceName", space.getName());
                spaceClassifications = Utilities.getSpaceRestrictions(space);
            }

            // Try to get page information
            Page page = (Page) parameters.get("page");
            if (page != null) {
                System.out.println("page is: " + page);

                context.put("pageName", page.getNameForComparison());
                pageClassifications = Utilities.getPageHierarchyRestrictions((ContentEntityObject) page);
            }
            aggregatedClassifications = aggregateClassifications(spaceClassifications, pageClassifications);

        } catch (Exception e) {
            System.err.println("exception in edirtpagevelocityContext: " + e.getMessage());
        }

        

        System.out.println("aggregated classification is: " + aggregatedClassifications.toString());
        context.put("maxClassification", aggregatedClassifications);
        return context;
    }
}