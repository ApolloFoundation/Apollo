package apl.updater;

public class UpdaterConstants {
   public static final String WINDOWS_UPDATE_SCRIPT_PATH = "update.vbs";
   public static final String LINUX_UPDATE_SCRIPT_PATH = "update.sh";
   public static final String OSX_UPDATE_SCRIPT_PATH = "update.sh";
   public static final int DOWNLOAD_ATTEMPTS = 10;
   public static final int NEXT_ATTEMPT_TIMEOUT = 60;
   public static final String TEMP_DIR_PREFIX = "Apollo-update";
   public static final String DOWNLOADED_FILE_NAME = "Apollo-newVersion.jar";
}
