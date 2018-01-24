package org.robovm.sdk.dyld.cache.tapi;

public class TapiUtils {


    /**
     * Returns true if install path is public
     */
    public static boolean isPublicLocation(String path) {
        // check https://github.com/ributzka/tapi/blob/b9205695b4edee91000383695be8de5ba8e0db41/lib/Core/Utils.cpp for details

        // Only libraries directly in /usr/lib are public. All other libraries in
        // sub-directories (such as /usr/lib/system) are considered private.
        if (path.startsWith("/usr/lib/") && path.indexOf('/', "/usr/lib/".length()) < 0)
            return true;

        // /System/Library/Frameworks/ is a public location
        if (path.startsWith("/System/Library/Frameworks/")) {

            // but only top level framework
            // /System/Library/Frameworks/Foo.framework/Foo ==> true
            // /System/Library/Frameworks/Foo.framework/Versions/A/Foo ==> true
            // /System/Library/Frameworks/Foo.framework/Resources/libBar.dylib ==> false
            // /System/Library/Frameworks/Foo.framework/Frameworks/Bar.framework/Bar
            // ==> false
            // /System/Library/Frameworks/Foo.framework/Frameworks/Xfoo.framework/XFoo
            // ==> false
            String name = path.substring("/System/Library/Frameworks/".length());
            String rest = name.substring(name.indexOf('.') + 1);
            name = name.substring(0, name.indexOf('.'));
            if (rest.startsWith("framework/") && rest.endsWith(name))
                return true;
        }

        return false;
    }

    public static boolean isStringCharsInRange(String s, char lo, char hi) {
        for (int idx = 0; idx < s.length(); idx ++) {
            char c = s.charAt(idx);
            if (c < lo || c > hi)
                return false;
        }

        return true;
    }
}
