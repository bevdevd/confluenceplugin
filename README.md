You have successfully created an Atlassian Plugin!

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK

## Changing Confluence's Email Templates Upon Updating
1. copy com.atlassian.confluence.plugins.confluence-email-notifications-plugin-<version number>.jar from confluence's install files
    - Located under 'atlassian-bundled-plugins'
    - version number can vary between confluence updates, and may nor match confluence's version, this is fine
2. mvoe the copy into a working directory
3. run 
    ``` bash
    jar xvf <plugin file name> 
    ```
    - replacing <plugin file name> for the full name from step 1
    - DO NOT move the .jar file at this stage, it needs to be at the root level of the working directory
4. Replace the 'templates' folder created from the above step, with our version
5. run
    ``` bash
    jar uvf <plugin file name> templates
    ```
    - againe replacing <plugin file name> with the full file name from step 1
6. in confluence's install files, removing the original email notifications plugin, and replace it with the one from the working directory
7. restart Confluence