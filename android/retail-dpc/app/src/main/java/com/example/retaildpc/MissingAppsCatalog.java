package com.example.retaildpc;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import java.util.ArrayList;

/** Explicit user-confirmed restore through official app stores. */
final class MissingAppsCatalog {
    private static final String[][] APPS={
        {"Kalendarz Samsung","com.samsung.android.calendar","S"},
        {"Kalkulator Samsung","com.sec.android.app.popupcalculator","S"},
        {"Zegar Samsung","com.sec.android.app.clockpackage","S"},
        {"Samsung Notes","com.samsung.android.app.notes","S"},
        {"Samsung Members","com.samsung.android.voc","S"},
        {"Wskazówki","com.samsung.android.app.tips","S"},
        {"Gaming Hub","com.samsung.android.game.gamehome","S"},
        {"Samsung Global Goals","com.samsung.sree","S"},
        {"Bixby","com.samsung.android.bixby.agent","S"},
        {"Facebook","com.facebook.katana","O"},
        {"Instagram","com.instagram.android","O"},
        {"Spotify","com.spotify.music","O"}
    };
    private MissingAppsCatalog(){}

    static void open(Activity activity){
        ArrayList<String[]> missing=new ArrayList<>();
        for(String[] app:APPS)
            if(!installed(activity.getPackageManager(),app[1]))missing.add(app);
        if(missing.isEmpty()){
            new AlertDialog.Builder(activity).setTitle("Nie wykryto braków")
                .setMessage("Wszystkie monitorowane pakiety są dostępne. " +
                    "Nie gwarantuje to widocznej ikony dla każdego pakietu.")
                .setPositiveButton("OK",null).show();
            return;
        }
        String[] names=new String[missing.size()];
        for(int i=0;i<names.length;i++) names[i]=missing.get(i)[0];
        new AlertDialog.Builder(activity)
            .setTitle("Brakujące aplikacje • "+missing.size())
            .setMessage("Wybierz aplikację, a następnie oficjalny sklep. " +
                "Instalacja wymaga potwierdzenia i może wymagać konta Google/Samsung. " +
                "Retail Setup nie instaluje obcych APK.")
            .setItems(names,(dialog,index)->choose(activity,missing.get(index)))
            .setNegativeButton("Zamknij",null).show();
    }
    private static boolean installed(PackageManager pm,String pkg){
        try{
            ApplicationInfo a=pm.getApplicationInfo(pkg,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return (a.flags & ApplicationInfo.FLAG_INSTALLED)!=0 && a.enabled;
        }catch(RuntimeException e){return false;}
        catch(PackageManager.NameNotFoundException e){return false;}
    }
    private static void choose(Activity a,String[] app){
        boolean samsung=app[2].equals("S");
        String[] sources=samsung?new String[]{"Google Play","Galaxy Store"}:
            new String[]{"Google Play"};
        new AlertDialog.Builder(a).setTitle(app[0])
            .setMessage("Otworzyć oficjalną stronę aplikacji? " +
                "Dostępność zależy od kraju, urządzenia i konta.")
            .setItems(sources,(d,choice)->{
                if(samsung && choice==1)openGalaxy(a,app[1]);
                else openPlay(a,app[1]);
            })
            .setNegativeButton("Wstecz",(d,w)->open(a)).show();
    }
    private static void openPlay(Activity a,String pkg){
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id="+pkg));
            intent.setPackage("com.android.vending");
            a.startActivity(intent);
            State.log(a,"Oficjalny sklep: Google Play "+pkg);
            return;
        }catch(RuntimeException absent){}
        try{
            a.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id="+pkg)));
            State.log(a,"Oficjalny sklep: Google Play web "+pkg);
        }catch(RuntimeException absent){unavailable(a);}
    }
    private static void openGalaxy(Activity a,String pkg){
        try{
            a.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("samsungapps://ProductDetail/"+pkg)));
            State.log(a,"Oficjalny sklep: Galaxy Store "+pkg);
        }catch(RuntimeException absent){unavailable(a);}
    }
    private static void unavailable(Activity a){
        new AlertDialog.Builder(a).setTitle("Sklep niedostępny")
            .setMessage("Sklep lub aplikacja może być niedostępna na tym telefonie. " +
                "Spróbuj drugiego sklepu lub wyszukaj aplikację ręcznie.")
            .setPositiveButton("Wróć",(d,w)->open(a)).show();
    }
}
