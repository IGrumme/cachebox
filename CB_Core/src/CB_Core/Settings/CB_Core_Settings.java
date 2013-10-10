package CB_Core.Settings;

import CB_Utils.Config_Core;
import CB_Utils.Settings.SettingBool;
import CB_Utils.Settings.SettingCategory;
import CB_Utils.Settings.SettingDouble;
import CB_Utils.Settings.SettingEncryptedString;
import CB_Utils.Settings.SettingFolder;
import CB_Utils.Settings.SettingInt;
import CB_Utils.Settings.SettingModus;
import CB_Utils.Settings.SettingStoreType;
import CB_Utils.Settings.SettingString;

public interface CB_Core_Settings
{

	// Abk�rzende Schreibweisen f�r die �bersichlichkeit bei den add Methoden
	public static final SettingModus INVISIBLE = SettingModus.Invisible;
	public static final SettingModus NORMAL = SettingModus.Normal;
	public static final SettingModus EXPERT = SettingModus.Expert;
	public static final SettingModus NEVER = SettingModus.Never;

	public static final SettingString GcLogin = new SettingString("GcLogin", SettingCategory.Login, SettingModus.Normal, "",
			SettingStoreType.Platform);

	public static final SettingEncryptedString GcAPI = new SettingEncryptedString("GcAPI", SettingCategory.Login, SettingModus.Invisible,
			"", SettingStoreType.Platform);

	public static final SettingEncryptedString GcAPIStaging = new SettingEncryptedString("GcAPIStaging", SettingCategory.Login,
			SettingModus.Invisible, "", SettingStoreType.Platform);

	public static final SettingBool StagingAPI = new SettingBool("StagingAPI", SettingCategory.Folder, SettingModus.Expert, false,
			SettingStoreType.Global);

	// Folder Settings
	public static final SettingFolder DescriptionImageFolder = new SettingFolder("DescriptionImageFolder", SettingCategory.Folder,
			SettingModus.Expert, Config_Core.WorkPath + "/repository/images", SettingStoreType.Global);

	public static final SettingFolder DescriptionImageFolderLocal = new SettingFolder("DescriptionImageFolderLocal",
			SettingCategory.Folder, SettingModus.Never, "", SettingStoreType.Local);

	public static final SettingFolder SpoilerFolder = new SettingFolder("SpoilerFolder", SettingCategory.Folder, SettingModus.Expert,
			Config_Core.WorkPath + "/repository/spoilers", SettingStoreType.Global);

	public static final SettingFolder SpoilerFolderLocal = new SettingFolder("SpoilerFolder", SettingCategory.Folder, SettingModus.Never,
			"", SettingStoreType.Local);

	public static final SettingInt conection_timeout = new SettingInt("conection_timeout", SettingCategory.Internal, INVISIBLE, 10000,
			SettingStoreType.Global);

	public static final SettingInt socket_timeout = new SettingInt("socket_timeout", SettingCategory.Internal, INVISIBLE, 60000,
			SettingStoreType.Global);

	public static final SettingEncryptedString GcVotePassword = new SettingEncryptedString("GcVotePassword", SettingCategory.Login, NORMAL,
			"", SettingStoreType.Platform);

	public static final SettingDouble ParkingLatitude = new SettingDouble("ParkingLatitude", SettingCategory.Positions, EXPERT, 0,
			SettingStoreType.Global);

	public static final SettingDouble ParkingLongitude = new SettingDouble("ParkingLongitude", SettingCategory.Positions, EXPERT, 0,
			SettingStoreType.Global);

	public static final SettingFolder UserImageFolder = new SettingFolder("UserImageFolder", SettingCategory.Folder, NORMAL,
			Config_Core.WorkPath + "/User/Media", SettingStoreType.Global);

}
