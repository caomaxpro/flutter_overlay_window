package flutter.overlay.window.flutter_overlay_window;


import android.view.Gravity;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import io.flutter.plugin.common.BasicMessageChannel;

public abstract class WindowSetup {

    public static int height = WindowManager.LayoutParams.MATCH_PARENT;
    public static int width = WindowManager.LayoutParams.MATCH_PARENT;
    public static int flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    public static int gravity = Gravity.CENTER;
    public static BasicMessageChannel<Object> messenger = null;
    public static String overlayTitle = "Overlay is activated";
    public static String overlayContent = "Tap to edit settings or disable";
    public static String positionGravity = "none";
    public static int notificationVisibility = NotificationCompat.VISIBILITY_PRIVATE;
    public static boolean enableDrag = false;
    public static int dragHandleHeight = 0;
    public static String renderType = "texture";


    public static void setNotificationVisibility(String name) {
        if (name.equalsIgnoreCase("visibilityPublic")) {
            notificationVisibility = NotificationCompat.VISIBILITY_PUBLIC;
        }
        if (name.equalsIgnoreCase("visibilitySecret")) {
            notificationVisibility = NotificationCompat.VISIBILITY_SECRET;
        }
        if (name.equalsIgnoreCase("visibilityPrivate")) {
            notificationVisibility = NotificationCompat.VISIBILITY_PRIVATE;
        }
    }

    public static void setFlag(String name) {
        if (name.equalsIgnoreCase("flagNotFocusable") || name.equalsIgnoreCase("defaultFlag")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (name.equalsIgnoreCase("flagNotTouchable") || name.equalsIgnoreCase("clickThrough")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }
        if (name.equalsIgnoreCase("flagNotTouchModal") || name.equalsIgnoreCase("focusPointer")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (name.equalsIgnoreCase("focusable")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
    }

    public static void showWhenLocked(String name) {
        if (name.equalsIgnoreCase("flagNotFocusable") || name.equalsIgnoreCase("defaultFlag")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (name.equalsIgnoreCase("flagNotTouchable") || name.equalsIgnoreCase("clickThrough")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }
        if (name.equalsIgnoreCase("flagNotTouchModal") || name.equalsIgnoreCase("focusPointer")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (name.equalsIgnoreCase("focusable")) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
    }

    public static void setGravityFromAlignment(String alignment) {
        if (alignment.equalsIgnoreCase("topLeft")) {
            gravity = Gravity.TOP | Gravity.LEFT;
            return;
        }
        if (alignment.equalsIgnoreCase("topCenter")) {
            gravity = Gravity.TOP;
        }
        if (alignment.equalsIgnoreCase("topRight")) {
            gravity = Gravity.TOP | Gravity.RIGHT;
            return;
        }

        if (alignment.equalsIgnoreCase("centerLeft")) {
            gravity = Gravity.CENTER | Gravity.LEFT;
            return;
        }
        if (alignment.equalsIgnoreCase("center")) {
            gravity = Gravity.CENTER;
        }
        if (alignment.equalsIgnoreCase("centerRight")) {
            gravity = Gravity.CENTER | Gravity.RIGHT;
            return;
        }

        if (alignment.equalsIgnoreCase("bottomLeft")) {
            gravity = Gravity.BOTTOM | Gravity.LEFT;
            return;
        }
        if (alignment.equalsIgnoreCase("bottomCenter")) {
            gravity = Gravity.BOTTOM;
        }
        if (alignment.equalsIgnoreCase("bottomRight")) {
            gravity = Gravity.BOTTOM | Gravity.RIGHT;
            return;
        }

    }
}
