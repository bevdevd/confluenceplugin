package com.example.test.security.delegates;

import com.example.test.security.utils.Utilities;

import com.atlassian.confluence.links.AbstractLink;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.ContentPermissionSet;
import com.atlassian.confluence.security.ContentPermission;

import com.atlassian.confluence.pages.Page;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.security.SpacePermission;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;

import com.atlassian.confluence.security.delegate.LinkPermissionsDelegate;

import java.util.List;
import java.util.Iterator;

public class NewLinkPermissionsDelegate extends LinkPermissionsDelegate {
   protected UserAccessor userAccessor;

   public boolean canView(User user, AbstractLink target) {
      return Utilities.isContentRestricted((ContentEntityObject)this.getContent(target), user, this.userAccessor, ContentPermission.VIEW_PERMISSION);
   }

   public void setUserAccessor(UserAccessor userAccessor) {
      this.userAccessor = userAccessor;
   }

   private Object getContent(AbstractLink target) {
      return target.getSourceContent();
   }
}