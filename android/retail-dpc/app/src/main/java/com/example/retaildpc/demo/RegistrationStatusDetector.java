package com.example.retaildpc.demo;

import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Read-only, conservative recognition of Samsung Retail Mode demo status. */
final class RegistrationStatusDetector {
    static final int UNKNOWN=0, DOWNLOADING=1, REGISTERED=2;
    private static final String ROOT_PACKAGE="com.samsung.rms.retailagent.global";
    private RegistrationStatusDetector() {}

    static int detect(AccessibilityNodeInfo root) {
        if(root==null || root.getPackageName()==null ||
            !ROOT_PACKAGE.contentEquals(root.getPackageName())) return UNKNOWN;
        // If registration UI is still present, instructions elsewhere in the
        // view hierarchy must not be interpreted as proof of registration.
        if(hasId(root, ROOT_PACKAGE+":id/activation_fragment", 0) ||
           hasId(root, ROOT_PACKAGE+":id/layoutSubsidary", 0) ||
           hasText(root, "Select Profile", 0) ||
           hasText(root, "Select country", 0)) return UNKNOWN;
        return scan(root, 0);
    }
    private static int scan(AccessibilityNodeInfo node,int depth) {
        if(node==null||depth>28||!node.isVisibleToUser())return UNKNOWN;
        int hit=classify(node.getText());
        if(hit!=UNKNOWN)return hit;
        hit=classify(node.getContentDescription());
        if(hit!=UNKNOWN)return hit;
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child==null)continue;
            try{
                hit=scan(child,depth+1);
                if(hit!=UNKNOWN)return hit;
            } finally {child.recycle();}
        }
        return UNKNOWN;
    }
    static int classify(CharSequence label) {
        if(label==null)return UNKNOWN;
        String s=label.toString().trim().toLowerCase(Locale.ROOT)
            .replaceAll("\\s+"," ");
        if(s.length()<8||s.length()>160)return UNKNOWN;
        for(String n:new String[]{"not registered","unregistered","not downloaded",
            "failed","failure","error","cancel","niezarejestrow","nie zarejestrow",
            "nie pobrano","błąd","anulow","nie uda","nie powiod",
            "please register","?"})if(s.contains(n))return UNKNOWN;
        for(String p:new String[]{"already registered","registration successful",
            "registration completed","device is registered",
            "device already registered","registered successfully",
            "already activated","activation successful","demo mode is active",
            "demo mode activated","demo setup completed","demo is ready",
            "content downloaded","download completed","download finished",
            "demo content ready","demo content installed",
            "już zarejestrowan","rejestracja zakończon","rejestracja pomyśln",
            "demo aktywne","demo jest aktywne","demo gotowe",
            "zawartość demo pobrana","pobieranie zakończone"})
            if(s.contains(p))return REGISTERED;
        for(String p:new String[]{"downloading demo","demo content downloading",
            "downloading content","content download in progress",
            "download in progress","downloading in progress",
            "demo download started","demo content download started",
            "pobieranie demo","pobieranie zawartości demo",
            "trwa pobieranie demo","rozpoczęto pobieranie demo",
            "pobieranie w toku"})
            if(s.contains(p))return DOWNLOADING;
        return UNKNOWN;
    }
    private static boolean hasId(AccessibilityNodeInfo node,String id,int depth) {
        if(node==null||depth>24)return false;
        if(node.isVisibleToUser()&&id.equals(node.getViewIdResourceName()))return true;
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child==null)continue;
            try{if(hasId(child,id,depth+1))return true;}finally{child.recycle();}
        }
        return false;
    }
    private static boolean hasText(AccessibilityNodeInfo node,String expected,int depth) {
        if(node==null||depth>24)return false;
        if(node.isVisibleToUser()&&node.getText()!=null&&
            expected.equalsIgnoreCase(node.getText().toString().trim()))return true;
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child==null)continue;
            try{if(hasText(child,expected,depth+1))return true;}finally{child.recycle();}
        }
        return false;
    }
}
