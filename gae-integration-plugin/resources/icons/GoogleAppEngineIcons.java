package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Created by nbashirbello on 9/1/15.
 */
public class GoogleAppEngineIcons {
    private static Icon load(String path) {
        return IconLoader.getIcon(path, GoogleAppEngineIcons.class);
    }

    public static final Icon AppEngine = load("/icons/appEngine.png"); // 16x16
}
