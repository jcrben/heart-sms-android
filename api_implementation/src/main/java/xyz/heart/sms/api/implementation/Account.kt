package xyz.heart.sms.api.implementation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64

import java.util.Date

import javax.crypto.spec.SecretKeySpec

import xyz.heart.sms.encryption.EncryptionUtils
import xyz.heart.sms.encryption.KeyUtils

@SuppressLint("ApplySharedPref")
object Account {

    @JvmStatic
    val QUICK_SIGN_UP_SYSTEM = false

    enum class SubscriptionType constructor(var typeCode: Int) {
        TRIAL(1), SUBSCRIBER(2), LIFETIME(3), FREE_TRIAL(4), FINISHED_FREE_TRIAL_WITH_NO_ACCOUNT_SETUP(5);

        companion object {
            fun findByTypeCode(code: Int): SubscriptionType? {
                return values().firstOrNull { it.typeCode == code }
            }
        }
    }

    var encryptor: EncryptionUtils? = null
        private set

    var primary: Boolean = false
    var trialStartTime: Long = 0
    var subscriptionType: SubscriptionType? = null
    var subscriptionExpiration: Long = 0
    var myName: String? = null
    var myPhoneNumber: String? = null
    var deviceId: String? = null
    var accountId: String? = null
    var salt: String? = null
    var passhash: String? = null
    var key: String? = null

    var hasPurchased: Boolean = false

    fun init(context: Context) {
        val sharedPrefs = getSharedPrefs(context)

        // account info
        primary = sharedPrefs.getBoolean(context.getString(R.string.api_pref_primary), false)
        xyz.heart.sms.api.implementation.Account.subscriptionType = SubscriptionType.Companion.findByTypeCode(sharedPrefs.getInt(context.getString(R.string.api_pref_subscription_type), 1))
        xyz.heart.sms.api.implementation.Account.subscriptionExpiration = sharedPrefs.getLong(context.getString(R.string.api_pref_subscription_expiration), -1)
        xyz.heart.sms.api.implementation.Account.trialStartTime = sharedPrefs.getLong(context.getString(R.string.api_pref_trial_start), -1)
        xyz.heart.sms.api.implementation.Account.myName = sharedPrefs.getString(context.getString(R.string.api_pref_my_name), null)
        xyz.heart.sms.api.implementation.Account.myPhoneNumber = sharedPrefs.getString(context.getString(R.string.api_pref_my_phone_number), null)
        deviceId = sharedPrefs.getString(context.getString(R.string.api_pref_device_id), null)
        accountId = sharedPrefs.getString(context.getString(R.string.api_pref_account_id), null)
        salt = sharedPrefs.getString(context.getString(R.string.api_pref_salt), null)
        passhash = sharedPrefs.getString(context.getString(R.string.api_pref_passhash), null)
        xyz.heart.sms.api.implementation.Account.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null)

        xyz.heart.sms.api.implementation.Account.hasPurchased = sharedPrefs.getBoolean(context.getString(R.string.api_pref_has_purchased), false)

        if (xyz.heart.sms.api.implementation.Account.key == null && passhash != null && accountId != null && salt != null) {
            // we have all the requirements to recompute the key,
            // not sure why this wouldn't have worked in the first place..
            xyz.heart.sms.api.implementation.Account.recomputeKey(context)
            xyz.heart.sms.api.implementation.Account.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null)

            val secretKey = SecretKeySpec(Base64.decode(xyz.heart.sms.api.implementation.Account.key, Base64.DEFAULT), "AES")
            xyz.heart.sms.api.implementation.Account.encryptor = EncryptionUtils(secretKey)
        } else if (xyz.heart.sms.api.implementation.Account.key == null && accountId != null) {
            // we cannot compute the key, uh oh. lets just start up the login activity and grab them...
            // This will do little good if they are on the api utils and trying to send a message or
            // something, or receiving a message. But they will have to re-login sometime I guess
            context.startActivity(Intent(context, xyz.heart.sms.api.implementation.LoginActivity::class.java))
        } else if (xyz.heart.sms.api.implementation.Account.key != null) {
            val secretKey = SecretKeySpec(Base64.decode(xyz.heart.sms.api.implementation.Account.key, Base64.DEFAULT), "AES")
            xyz.heart.sms.api.implementation.Account.encryptor = EncryptionUtils(secretKey)
        }

        val application = context.applicationContext
        if (application is xyz.heart.sms.api.implementation.AccountInvalidator) {
            application.onAccountInvalidated(xyz.heart.sms.api.implementation.Account)
        }
    }

    fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    fun forceUpdate(context: Context): xyz.heart.sms.api.implementation.Account {
        xyz.heart.sms.api.implementation.Account.init(context)
        return xyz.heart.sms.api.implementation.Account
    }

    fun clearAccount(context: Context) {
        getSharedPrefs(context).edit()
                .remove(context.getString(R.string.api_pref_account_id))
                .remove(context.getString(R.string.api_pref_salt))
                .remove(context.getString(R.string.api_pref_passhash))
                .remove(context.getString(R.string.api_pref_key))
                .remove(context.getString(R.string.api_pref_subscription_type))
                .remove(context.getString(R.string.api_pref_subscription_expiration))
                .commit()

        xyz.heart.sms.api.implementation.Account.init(context)
    }

    fun updateSubscription(context: Context, type: SubscriptionType, expiration: Date?) {
        xyz.heart.sms.api.implementation.Account.updateSubscription(context, type, expiration?.time, true)
    }

    fun updateSubscription(context: Context, type: SubscriptionType?, expiration: Long?, sendToApi: Boolean) {
        xyz.heart.sms.api.implementation.Account.subscriptionType = type
        xyz.heart.sms.api.implementation.Account.subscriptionExpiration = expiration!!

        getSharedPrefs(context).edit()
                .putInt(context.getString(R.string.api_pref_subscription_type), type?.typeCode ?: 0)
                .putLong(context.getString(R.string.api_pref_subscription_expiration), expiration)
                .commit()

        if (sendToApi) {
            xyz.heart.sms.api.implementation.ApiUtils.updateSubscription(accountId, type?.typeCode, expiration)
        }
    }

    fun setName(context: Context, name: String?) {
        xyz.heart.sms.api.implementation.Account.myName = name

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), name)
                .commit()
    }

    fun setPhoneNumber(context: Context, phoneNumber: String?) {
        xyz.heart.sms.api.implementation.Account.myPhoneNumber = phoneNumber

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), phoneNumber)
                .commit()
    }

    fun setPrimary(context: Context, primary: Boolean) {
        Account.primary = primary

        getSharedPrefs(context).edit()
                .putBoolean(context.getString(R.string.api_pref_primary), primary)
                .commit()
    }

    fun setDeviceId(context: Context, deviceId: String?) {
        Account.deviceId = deviceId

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_device_id), deviceId)
                .commit()
    }

    fun setHasPurchased(context: Context, hasPurchased: Boolean) {
        xyz.heart.sms.api.implementation.Account.hasPurchased = hasPurchased

        getSharedPrefs(context).edit()
                .putBoolean(context.getString(R.string.api_pref_has_purchased), hasPurchased)
                .commit()
    }

    fun recomputeKey(context: Context) {
        val keyUtils = xyz.heart.sms.encryption.KeyUtils()
        val key = keyUtils.createKey(passhash, accountId, salt)

        val encodedKey = Base64.encodeToString(key.encoded, Base64.DEFAULT)

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_key), encodedKey)
                .commit()
    }

    fun exists(): Boolean {
        return accountId != null && !accountId!!.isEmpty() &&
                deviceId != null && salt != null && passhash != null
                && xyz.heart.sms.api.implementation.Account.key != null
    }

    private const val TRIAL_LENGTH = 7 // days
    fun getDaysLeftInTrial(): Int {
        return if (xyz.heart.sms.api.implementation.Account.subscriptionType == SubscriptionType.FREE_TRIAL) {
            val now = Date().time
            val timeInTrial = now - xyz.heart.sms.api.implementation.Account.trialStartTime
            val trialLength = 1000 * 60 * 60 * 24 * xyz.heart.sms.api.implementation.Account.TRIAL_LENGTH
            if (timeInTrial > trialLength) {
                0
            } else {
                val timeLeftInTrial = trialLength - timeInTrial
                val timeInDays = (timeLeftInTrial / (1000 * 60 * 60 * 24)) + 1
                timeInDays.toInt()
            }
        } else {
            0
        }
    }
}
