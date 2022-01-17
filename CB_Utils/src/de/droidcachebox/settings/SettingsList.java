package de.droidcachebox.settings;

import static de.droidcachebox.settings.SettingStoreType.Global;
import static de.droidcachebox.settings.SettingStoreType.Local;
import static de.droidcachebox.settings.SettingStoreType.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.droidcachebox.database.Database_Core;
import de.droidcachebox.utils.log.Log;

public abstract class SettingsList extends ArrayList<SettingBase<?>> {
    private static final String sClass = "SettingsList";
    private static final long serialVersionUID = -969846843815877942L;
    private static SettingsList that;
    private boolean isLoaded = false;

    public SettingsList() {
        that = this;
        Member[] mbrs = this.getClass().getFields();
        for (Member mbr : mbrs) {
            if (mbr instanceof Field) {
                try {
                    Object obj = ((Field) mbr).get(this);
                    if (obj instanceof SettingBase<?>) {
                        add((SettingBase<?>) obj);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public boolean add(SettingBase<?> setting) {
        if (!that.contains(setting)) {
            return super.add(setting);
        }
        return false;
    }

    protected abstract Database_Core getSettingsDB();

    protected abstract Database_Core getDataDB();

    protected abstract SettingsDAO createSettingsDAO();

    protected abstract boolean canNotUsePlatformSettings();

    /**
     * Return true, if setting changes need restart
     *
     * @return ?
     */
    public boolean writeToDatabases() {
        getSettingsDB().beginTransaction();
        SettingsDAO dao = createSettingsDAO();
        // if used from Splash, DataDB is not possible = Data == null
        Database_Core Data = getDataDB();
        try {
            if (Data != null)
                Data.beginTransaction();
        } catch (Exception ex) {
            Data = null;
        }

        boolean needRestart = false;
        String currentSettingName = "";
        try {
            for (SettingBase<?> setting : this) {
                if (setting.isDirty()) {
                    currentSettingName = setting.name;

                    if (Local == setting.getStoreType()) {
                        if (Data != null)
                            dao.writeSetting(Data, setting);
                    } else if (Global == setting.getStoreType() || (canNotUsePlatformSettings() && Platform == setting.getStoreType())) {
                        dao.writeSetting(getSettingsDB(), setting);
                    } else if (Platform == setting.getStoreType()) {
                        dao.writePlatformSetting(setting);
                        dao.writeSetting(getSettingsDB(), setting);
                    }

                    if (setting.needRestart) {
                        needRestart = true;
                    }
                    setting.clearDirty();
                }
            }
            // all changes successful
            if (Data != null)
                Data.setTransactionSuccessful();
            getSettingsDB().setTransactionSuccessful();
        } catch (Exception ex) {
            Log.err(sClass, currentSettingName, ex);
        } finally {
            getSettingsDB().endTransaction();
            if (Data != null)
                Data.endTransaction();
        }
        return needRestart;
    }

    public void readFromDB() {
        AtomicInteger tryCount = new AtomicInteger(0);
        while (tryCount.incrementAndGet() < 10) {
            SettingsDAO dao = createSettingsDAO();
            try {

                for (SettingBase<?> setting : this) {
                    // String debugString;
                    // boolean isPlatform = false;
                    // boolean isPlattformoverride = false;

                    if (Local == setting.getStoreType()) {
                        if (getDataDB() == null || getDataDB().getDatabasePath() == null)
                            setting.loadDefault();
                        else
                            setting = dao.readSetting(getDataDB(), setting);
                    } else if (Global == setting.getStoreType() || (canNotUsePlatformSettings() && Platform == setting.getStoreType())) {
                        setting = dao.readSetting(getSettingsDB(), setting);
                    } else if (Platform == setting.getStoreType()) {
                        // isPlatform = true;
                        SettingBase<?> cpy = setting.copy();
                        cpy = dao.readSetting(getSettingsDB(), cpy);
                        setting = dao.readPlatformSetting(setting);

                        // chk for Value on User.db3 and cleared Platform Value

                        if (setting instanceof SettingString) {
                            SettingString st = (SettingString) setting;

                            if (st.value.length() == 0) {
                                // Platform Settings are empty use db3 value or default
                                setting = dao.readSetting(getSettingsDB(), setting);
                                dao.writePlatformSetting(setting);
                            }
                        } else if (!cpy.value.equals(setting.value)) {
                            if (setting.value.equals(setting.defaultValue)) {
                                // override Platformsettings with UserDBSettings
                                setting.setValueFrom(cpy);
                                dao.writePlatformSetting(setting);
                                setting.clearDirty();
                                // isPlattformoverride = true;
                            } else {
                                // override UserDBSettings with Platformsettings
                                cpy.setValueFrom(setting);
                                dao.writeSetting(getSettingsDB(), cpy);
                                cpy.clearDirty();
                            }
                        }
                    }

                    /*
                    if (setting instanceof SettingEncryptedString) {// Don't write encrypted settings in to a log file
                        debugString = "*******";
                    } else {
                        debugString = setting.value.toString();
                    }

                    if (isPlatform) {
                        if (isPlattformoverride) {
                            Log.trace(log, "Override Platform setting [" + setting.name + "] from DB to: " + debugString);
                        } else {
                            Log.trace(log, "Override PlatformDB setting [" + setting.name + "] from Platform to: " + debugString);
                        }
                    } else {
                        if (!setting.value.equals(setting.defaultValue)) {
                            Log.trace(log, "Change " + setting.getStoreType() + " setting [" + setting.name + "] to: " + debugString);
                        } else {
                            Log.trace(log, "Default " + setting.getStoreType() + " setting [" + setting.name + "] to: " + debugString);
                        }
                    }
                     */
                }
                tryCount.set(100);
            } catch (Exception e) {
                Log.err(sClass, "Error read settings, try again");
            }

        }
        Log.debug(sClass, "Settings are loaded");
        isLoaded = true;
    }

    public void loadFromLastValues() {
        for (SettingBase<?> setting : this) {
            setting.loadFromLastValue();
        }
    }

    public void saveToLastValues() {
        for (SettingBase<?> setting : this) {
            setting.saveToLastValue();
        }
    }

    public void loadAllDefaultValues() {
        for (SettingBase<?> setting : this) {
            setting.loadDefault();
        }
    }
}
