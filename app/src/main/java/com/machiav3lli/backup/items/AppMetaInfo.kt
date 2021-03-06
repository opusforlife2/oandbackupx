package com.machiav3lli.backup.items

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.File
import java.util.*

open class AppMetaInfo : Parcelable {
    @SerializedName("packageName")
    @Expose
    var packageName: String? = null
        private set

    @SerializedName("packageLabel")
    @Expose
    var packageLabel: String? = null
        private set

    @SerializedName("versionName")
    @Expose
    var versionName: String? = null
        private set

    @SerializedName("versionCode")
    @Expose
    var versionCode = 0
        private set

    @SerializedName("profileId")
    @Expose
    var profileId = 0
        private set

    @SerializedName("sourceDir")
    @Expose
    var sourceDir: String? = null
        private set

    @SerializedName("splitSourceDirs")
    @Expose
    var splitSourceDirs: Array<String>? = null
        private set

    @SerializedName("isSystem")
    @Expose
    var isSystem = false
        private set

    @SerializedName("icon")
    @Expose
    var applicationIcon: Drawable? = null

    constructor() {
    }

    constructor(context: Context, pi: PackageInfo) {
        packageName = pi.packageName
        packageLabel = pi.applicationInfo.loadLabel(context.packageManager).toString()
        versionName = pi.versionName
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode.toInt()
        else pi.versionCode
        // Don't have access to UserManager service; using a cheap workaround to figure out
        // who is running by parsing it from the data path: /data/user/0/org.example.app
        try {
            profileId = Objects.requireNonNull(File(pi.applicationInfo.dataDir).parentFile).name.toInt()
        } catch (e: NumberFormatException) {
            // Android System "App" points to /data/system
            profileId = -1
        }
        sourceDir = pi.applicationInfo.sourceDir
        splitSourceDirs = pi.applicationInfo.splitSourceDirs
        // Boolean arithmetic to check if FLAG_SYSTEM is set
        isSystem = pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
        applicationIcon = context.packageManager.getApplicationIcon(pi.applicationInfo)
    }

    constructor(packageName: String?, packageLabel: String?, versionName: String?, versionCode: Int,
                profileId: Int, sourceDir: String?, splitSourceDirs: Array<String>?, isSystem: Boolean) {
        this.packageName = packageName
        this.packageLabel = packageLabel
        this.versionName = versionName
        this.versionCode = versionCode
        this.profileId = profileId
        this.sourceDir = sourceDir
        this.splitSourceDirs = splitSourceDirs
        this.isSystem = isSystem
    }

    protected constructor(source: Parcel) {
        packageName = source.readString()
        packageLabel = source.readString()
        versionName = source.readString()
        versionCode = source.readInt()
        profileId = source.readInt()
        sourceDir = source.readString()
        splitSourceDirs = source.createStringArray()
        isSystem = source.readByte().toInt() != 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageName)
        parcel.writeString(packageLabel)
        parcel.writeString(versionName)
        parcel.writeInt(versionCode)
        parcel.writeInt(profileId)
        parcel.writeString(sourceDir)
        parcel.writeStringArray(splitSourceDirs)
        parcel.writeByte((if (isSystem) 1 else 0).toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    open val isSpecial: Boolean
        get() = false

    fun hasIcon(): Boolean {
        return applicationIcon != null
    }

    companion object {
        val CREATOR: Parcelable.Creator<AppMetaInfo?> = object : Parcelable.Creator<AppMetaInfo?> {
            override fun createFromParcel(source: Parcel): AppMetaInfo? {
                return AppMetaInfo(source)
            }

            override fun newArray(size: Int): Array<AppMetaInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}