package aaa.com.plugin.security;

import aaa.com.plugin.security.utils.Utilities;

import com.atlassian.user.User;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.security.ContentPermissionSet;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

public class PermissionInterceptor {
    @ComponentImport
    private static UserAccessor userAccessor;

    PermissionInterceptor(UserAccessor userAccessor){
        this.userAccessor = userAccessor;
    }

    public static boolean intercept(ContentPermissionSet instance, User user){
        return Utilities.isContentRestricted(user, userAccessor, instance);
    }
}