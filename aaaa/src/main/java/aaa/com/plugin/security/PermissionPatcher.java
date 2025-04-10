package aaa.com.plugin.security;

import aaa.com.plugin.security.PermissionInterceptor;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

// import jakarta.annotation.PostConstruct;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;

import com.atlassian.confluence.security.ContentPermissionSet;

@Component
@ExportAsService
public class PermissionPatcher {
    @PostConstruct
    public void patchPermissionLogic() {
        System.out.println("Initializing ByteBuddy patch...");

        ByteBuddyAgent.install();

        new ByteBuddy()
            .redefine(ContentPermissionSet.class)
            .method(ElementMatchers.named("isPermitted")
                    .and(ElementMatchers.takesArguments(com.atlassian.user.User.class)))
            .intercept(net.bytebuddy.implementation.MethodDelegation.to(PermissionInterceptor.class))
            .make()
            .load(ContentPermissionSet.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        System.out.println("ContentPermissionSet.isPermitted successfully patched.");
    }
}
