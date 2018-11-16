package com.sierrawireless.avphone.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Fragment
import android.app.PendingIntent
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.MenuItem
import android.widget.ListView
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.ndk.CrashlyticsNdk
import com.sierrawireless.avphone.*
import com.sierrawireless.avphone.adapter.MenuAdapter
import com.sierrawireless.avphone.adapter.MenuEntry
import com.sierrawireless.avphone.adapter.MenuEntryType
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.auth.AuthenticationManager
import com.sierrawireless.avphone.listener.CustomLabelsListener
import com.sierrawireless.avphone.listener.LoginListener
import com.sierrawireless.avphone.listener.MonitorServiceListener
import com.sierrawireless.avphone.service.MonitorServiceManager
import com.sierrawireless.avphone.service.MonitoringService
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder
import com.sierrawireless.avphone.task.*
import com.sierrawireless.avphone.tools.DeviceInfo
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import net.airvantage.model.User
import net.airvantage.utils.PreferenceUtils
import org.jetbrains.anko.alert
import java.util.*

/**
 * The main activity, handling drawer and Fragments
 */
class MainActivity : FragmentActivity(), LoginListener, AuthenticationManager, OnSharedPreferenceChangeListener, MonitorServiceManager, CustomLabelsManager, SyncWithAvListener {
    override var monitoringService: MonitoringService? = null
    private var objectName: String? = null
    internal var startObjectName: String? = null

    private var alarmManager: AlarmManager? = null
    private var taskFactory: IAsyncTaskFactory? = null
    override var authentication: Authentication? = null
    private var prefs: SharedPreferences? = null

    internal var boundToMonitoringService = false
    private var monitoringServiceListener: MonitorServiceListener? = null

    private var customLabelsListener: CustomLabelsListener? = null


    private var drawerToggle: ActionBarDrawerToggle? = null

    private var configureFragment: ConfigureFragment? = null
    private var homeFragment: HomeFragment? = null
    private var runFragment: ArrayList<RunFragment>? = null

    internal var lastPosition = 0
    private var stopped = false

    private var serviceSendData: Boolean? = false
    internal lateinit var objectsManager: ObjectsManager
    var user: User? = null
    private var fragmentsMapping = HashMap<String, Fragment>()
    internal var fragmentsList: ArrayList<MenuEntry>? = null

    override val isLogged: Boolean
        get() = this.authentication != null && !this.authentication!!.isExpired(Date())

    private var connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            monitoringService = (binder as ServiceBinder).service

            if (monitoringServiceListener != null) {
                runOnUiThread {
                    monitoringServiceListener!!.onServiceStarted(monitoringService!!)
                }
            }

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            boundToMonitoringService = false
        }
    }

    val systemUid: String?
        get() = prefs!!.getString("systemUid", null)

    var systemSerial: String?
        get() = prefs!!.getString(PREFERENCE_SYSTEM_SERIAL, null)
        @SuppressLint("DefaultLocale")
        set(serial) = prefs!!.edit().putString(PREFERENCE_SYSTEM_SERIAL, serial!!.toUpperCase()).apply()

    val systemName: String?
        get() = prefs!!.getString(PREFERENCE_SYSTEM_NAME, null)

    private fun buildFragmentList(): ArrayList<MenuEntry> {
        val tmp = ArrayList<MenuEntry>()
        if (user != null) {
            tmp.add(MenuEntry("LOGGED AS", MenuEntryType.TITLE))
            tmp.add(MenuEntry(user!!.name!!, MenuEntryType.USER, drawable = ContextCompat.getDrawable(this, R.drawable.ic_user)))
            tmp.add(MenuEntry(user!!.profile!!.name!!, MenuEntryType.USER, drawable = ContextCompat.getDrawable(this, R.drawable.ic_group)))
            tmp.add(MenuEntry(user!!.company!!.name!!, MenuEntryType.USER, drawable = ContextCompat.getDrawable(this, R.drawable.ic_departement)))
            tmp.add(MenuEntry(user!!.server!!, MenuEntryType.USER, drawable = ContextCompat.getDrawable(this, R.drawable.ic_domain)))
        }
        tmp.add(MenuEntry("SIMULATED OBJECTS", MenuEntryType.TITLE, button = true))
        objectsManager.objects.mapTo(tmp) {
            if (it.name != null) {
                MenuEntry(it.name!!, MenuEntryType.COMMAND, drawable = ContextCompat.getDrawable(this, R.drawable.ic_object))
            }else{
                MenuEntry("KO", MenuEntryType.COMMAND, drawable = ContextCompat.getDrawable(this, R.drawable.ic_object))
            }
        }
   //     tmp.add(MenuEntry(FRAGMENT_CONFIGURE, MenuEntryType.COMMAND))

        //tmp.add(FRAGMENT_SETTINGS);
        tmp.add(MenuEntry("NEED HELP", MenuEntryType.TITLE))
        tmp.add(MenuEntry(FRAGMENT_FAQ, MenuEntryType.COMMAND))
        tmp.add(MenuEntry("", MenuEntryType.TITLE))
        tmp.add(MenuEntry(FRAGMENT_SETTINGS, MenuEntryType.COMMAND))
        if (isLogged) {
            tmp.add(MenuEntry(FRAGMENT_LOGOUT, MenuEntryType.COMMAND))
        } else {
            tmp.add(MenuEntry(FRAGMENT_LOGIN, MenuEntryType.COMMAND))
        }
        return tmp
    }

    override fun setCustomLabelsListener(listener: CustomLabelsListener) {
        this.customLabelsListener = listener
    }

    //Get user
    private fun syncGetUser(auth: Authentication?) {

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(this)

        val getUserTask = taskFactory!!.getUserTak(avPhonePrefs.serverHost!!, auth!!.accessToken!!)

        getUserTask.addProgressListener { result ->
            if (!result.isError) {
                user = result.user
                user!!.server = avPhonePrefs.serverHost
                systemSerial = DeviceInfo.generateSerial(user!!.uid!!)
                loadMenu(true)
            }else{
                if (! stopped ) {
                    if (result.error!!.errorParameters.size == 1 && result.error!!.errorParameters[0] == "No Connection") {
                        stopped = true
                        alert("You don't have any data connection. The application will be stopped", "Alert") {
                            positiveButton("YES") {
                                finish()
                            }
                        }.show()
                    }else{
                        logout()
                    }
                }
            }
        }
        val params = GetUserParams()
        getUserTask.execute(params)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "OnCreate called")
        instance = this
        // Initialization og Object Manager
        objectsManager = ObjectsManager.getInstance()
        objectsManager.init(this)
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics(), CrashlyticsNdk())

        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs!!.registerOnSharedPreferenceChangeListener(this)

        //Auth is set here if exist
        readAuthenticationFromPreferences()

        taskFactory = AsyncTaskFactory(this@MainActivity)
        if (isLogged) {
            if (user == null) {
                syncGetUser(authentication)
            }
        }

        loadMenu(true)

        left_drawer.setOnItemClickListener { _, _, position, _ -> selectItem(position) }

        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        drawerToggle = ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open,
                R.string.drawer_close)
        drawer_layout!!.addDrawerListener(drawerToggle!!)

        // Verify Permission

        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Ask for READ PHONE STATE")
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
            }
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Ask for COARSE LOCATION STATE")
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
            }
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Ask for FINE LOCATION STATE")
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty()) {

                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.w(TAG, "onRequestPermissionsResult: READ_PHONE_STATE answer ko")

                        alert("Permission not granted. The application will exit now", "Alert") {
                            positiveButton("OK") {
                                finish()
                            }
                        }.show()
                    } else {
                        Log.d(TAG, " for READ PHONE STATE ok")
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Ask for COARSE LOCATION  STATE")
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
                        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                            Log.d(TAG, "Ask for FINE LOCATION STATE")
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
                        }
                    }
                }
            }
            2 -> {
                if (grantResults.isNotEmpty()) {

                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.w(TAG, "onRequestPermissionsResult: ACCESS_COARSE_LOCATION answer ko")
                        alert("Permission not granted. The application will exit now", "Alert") {
                            positiveButton("OK") {
                                finish()
                            }
                        }.show()
                    } else {
                        Log.d(TAG, " for COARSE LOCATION ok")
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                            Log.d(TAG, "Ask for FINE LOCATION STATE")
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
                        }
                    }
                }
            }
            3 -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.w(TAG, "onRequestPermissionsResult: answer ACCESS_FINE_LOCATION ko")
                        alert("Permission not granted. The application will exit now", "Alert") {
                            positiveButton("OK") {
                                finish()
                            }
                        }.show()
                    }else{
                        Log.d(TAG, "for FINE LOCATION STATE ok")
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    fun loadMenu(changeFragment:Boolean) {
        fragmentsList = buildFragmentList()


        val adapter = MenuAdapter(this, buildFragmentList())

        left_drawer.adapter = adapter

        left_drawer.choiceMode = ListView.CHOICE_MODE_SINGLE
        // Set the list's click listener
        left_drawer.invalidateViews()

        if (isLogged) {
            if (user == null) {
                syncGetUser(authentication)
            }

            unlockDrawer()
            if (isServiceRunning()) {
                if (objectName != null) {
                    connectToService(objectName!!)
                }
            }
            initFragments()
            if (changeFragment)
                goLastFragment()
        } else {
            lockDrawer()
            if (isServiceRunning()) {
                // The token is probably expired.
                // We stop the service since the "stop" button is not available anymore.
                this.stopMonitoringService()
            }
            initFragments()
            if (changeFragment)
                goHomeFragment()
        }
    }

    override fun onResume() {

        super.onResume()
        left_drawer.requestFocusFromTouch()
        left_drawer.setItemChecked(lastPosition, true)
        left_drawer.setSelection(lastPosition)
        left_drawer.refreshDrawableState()

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle!!.syncState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return (drawerToggle!!.onOptionsItemSelected(item)
                // Or just go with defaults
                || super.onOptionsItemSelected(item))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    private fun lockDrawer() {

        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(false)
            actionBar!!.setHomeButtonEnabled(false)
        }

        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        if (drawerToggle != null) {
            drawerToggle!!.isDrawerIndicatorEnabled = false
        }
    }

    private fun unlockDrawer() {
        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setHomeButtonEnabled(true)
        }

        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        if (drawerToggle != null) {
            drawerToggle!!.isDrawerIndicatorEnabled = true
        }
    }

    internal fun readAuthenticationFromPreferences() {
        this.authentication = PreferenceUtils.readAuthentication(this)
    }

    override fun onAuthentication(auth: Authentication) {

        this.authentication = auth
        unlockDrawer()

        saveAuthenticationInPreferences(auth)
    }

    private fun saveAuthenticationInPreferences(auth: Authentication) {
        PreferenceUtils.saveAuthentication(this, auth)
    }

    override fun forgetAuthentication() {

        lockDrawer()
        PreferenceUtils.resetAuthentication(this)
        this.authentication = null

        if (this.isServiceRunning()) {
            this.stopMonitoringService()
        }

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        if (PreferenceUtils.PREF_SERVER_KEY == key || PreferenceUtils.PREF_CLIENT_ID_KEY == key) {

            this.stopMonitoringService()
            this.forgetAuthentication()

        } else if (PreferenceUtils.PREF_PERIOD_KEY == key || PreferenceUtils.PREF_PASSWORD_KEY == key) {

            this.restartMonitoringService()

        } else if (key.contains("pref_data_custom")) {

            if (this.customLabelsListener != null) {
                this.customLabelsListener!!.onCustomLabelsChanged()
            }
        }
    }

     override fun isServiceRunning(name: String?): Boolean {
        //return serviceSendData;

        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

         @Suppress("DEPRECATION")
         return if (name == null) {
             if (manager.getRunningServices(Integer.MAX_VALUE).any { MonitoringService::class.java.name == it.service.className }) serviceSendData!! else false
         }else{
             if (manager.getRunningServices(Integer.MAX_VALUE).any { MonitoringService::class.java.name == it.service.className }) (serviceSendData!! && startObjectName == name) else false

         }
     }

    override fun isServiceStarted(name: String): Boolean {
        if (this.objectName == null) {
            return false
        }
        if (this.objectName != name) {
            return false
        }
        //return serviceSendData;

        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any { MonitoringService::class.java.name == it.service.className }
    }

    override fun oneServiceStarted(): Boolean {
        //return serviceSendData;

        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any { MonitoringService::class.java.name == it.service.className }

    }

    private fun connectToService(name: String) {
        val intent = Intent(this, MonitoringService::class.java)
        val avPrefs = PreferenceUtils.getAvPhonePrefs(this)
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this))
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost)
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password)
        intent.putExtra(MonitoringService.CONNECT, false)
        intent.putExtra(MonitoringService.OBJECT_NAME, name)
        boundToMonitoringService = this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun disconnectFromService() {
        if (boundToMonitoringService) {
            this.unbindService(connection)
            boundToMonitoringService = false
            if (monitoringServiceListener != null) {
                monitoringServiceListener!!.onServiceStopped(monitoringService!!)
            }
        }
    }

    override fun cancel() {
        monitoringService?.cancel()
    }
    override fun start() {
        monitoringService?.start()
    }

    override fun sendAlarmEvent(on:Boolean, name:String):Boolean {
        //if (boundToMonitoringService && monitoringService != null) {
        return monitoringService!!.sendAlarmEvent(on, name)
        // }
    }

    override fun startMonitoringService(name: String) {
        this.objectName = name
        val avPrefs = PreferenceUtils.getAvPhonePrefs(this)

        val intent = Intent(this, MonitoringService::class.java)
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this))
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost)
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password)
        intent.putExtra(MonitoringService.CONNECT, false)
        intent.putExtra(MonitoringService.OBJECT_NAME, name)

        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        // registering our pending intent with alarm manager
        alarmManager!!.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pendingIntent)

        connectToService(name)
    }

    internal fun setAlarm(timer:Int?) {
        val avPrefs = PreferenceUtils.getAvPhonePrefs(this)
        val intent = Intent(this, MonitoringService::class.java)
        Log.d(TAG, "Set intent")
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this))
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost)
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password)
        intent.putExtra(MonitoringService.CONNECT, true)
        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        // registering our pending intent with alarm manager

        val wait = if (timer == null) {
            SystemClock.elapsedRealtime() +(Integer.valueOf(avPrefs.period)!! * 60 * 1000).toLong()
        }else {
            SystemClock.elapsedRealtime() + 100
        }
        alarmManager!!.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,  wait, pendingIntent)
    }

    override fun startSendData(name: String):Boolean {
        if (startObjectName == null || name == startObjectName!!) {
            setAlarm(0)
            startObjectName = name
            serviceSendData = true
        }else{
            alert("A run already exist for $startObjectName.", "Alert") {
                positiveButton("OK") {

                }
            }.show()
            return false
        }
        return true
    }

    override fun stopSendData() {
        val intent = Intent(this, MonitoringService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager!!.cancel(pendingIntent)
        serviceSendData = false
        startObjectName = null
    }

    override fun stopMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager!!.cancel(pendingIntent)
        this.stopService(intent)

        disconnectFromService()
    }

    private fun restartMonitoringService() {
        if (objectName != null) {
            stopMonitoringService()
            startMonitoringService(objectName!!)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        disconnectFromService()
    }

    override fun setMonitoringServiceListener(listener: MonitorServiceListener) {
        this.monitoringServiceListener = listener
    }

    @SuppressLint("DefaultLocale")
    override fun invoke(result: SyncWithAvResult) {

        val system = result.system ?: return
        prefs!!.edit().putString("systemUid", system.uid).apply()
        prefs!!.edit().putString(PREFERENCE_SYSTEM_NAME, system.name).apply()

        val user = result.user
        prefs!!.edit().putString(PREFERENCE_USER_UID, user!!.uid).apply()
        prefs!!.edit().putString(PREFERENCE_USER_NAME, user.name).apply()

        val deviceSerial = DeviceInfo.generateSerial(user.uid!!)
        prefs!!.edit().putString(PREFERENCE_SYSTEM_SERIAL, deviceSerial).apply()
        
    }

    private fun clearStack() {
        //Here we are clearing back stack fragment entries
        MainActivity.instance.runOnUiThread {
            fragmentManager.popBackStack("HOME", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
//        val backStackEntry = fragmentManager.backStackEntryCount
//        if (backStackEntry > 0) {
//            for (i in 0 until backStackEntry) {
//                fragmentManager.popBackStackImmediate()
//            }
//        }

    }


    override fun onBackPressed() {
        if (isLogged) {
            super.onBackPressed()
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    fun logout() {
        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(this)

        // Already logout if authentication is null
        if (authentication == null) {
            return
        }

        val accessToken = authentication!!.accessToken

        val logoutTask = taskFactory!!.logoutTask(avPhonePrefs.serverHost!!, accessToken!!)

        logoutTask.execute()
        try {
            logoutTask.get()
        } catch (e: Exception) {
            Log.w(TAG, "Exception while logging out")
            Crashlytics.logException(e)
        } finally {
            forgetAuthentication()

            clearStack()
            //fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            // In this case we have no menu
            loadMenu(true)

        }
    }

    private fun selectItem(position: Int) {

        val fragment = getFragment(position)
        val entry = fragmentsList!![position]
        if (fragment == null) {
            //No item check if the position is valid
            if (entry.name == FRAGMENT_FAQ) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://source.sierrawireless.com/airvantage/av/avphone_faq/"))
                startActivity(browserIntent)
                left_drawer.setSelection(lastPosition)
                drawer_layout.closeDrawer(left_drawer)
            }
            return
        }
        if (entry.name == FRAGMENT_LOGOUT) {

            alert("Are you sure ?","Logout") {
                positiveButton("YES") {
                    logout()
                }
                negativeButton("NO") {
                    drawer_layout.closeDrawer(left_drawer)

                }
            }.show()
            return
        }
        if (entry.type == MenuEntryType.COMMAND) {
            // We have not selected this fragment now do it
            if (lastPosition != position) {
                // Insert the fragment by replacing any existing fragment
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commit()
                // Highlight the selected item, update the title, and close the drawer
                left_drawer.setItemChecked(position, true)
                title = entry.name
                left_drawer.setSelection(position)
                lastPosition = position
                left_drawer.invalidateViews()
            }
            drawer_layout.closeDrawer(left_drawer)
        }
    }

    private fun goHomeFragment() {
        val fragment = if (fragmentsMapping.containsKey(FRAGMENT_LOGIN)) fragmentsMapping[FRAGMENT_LOGIN] else null
        goFragment(fragment!!, FRAGMENT_LOGIN)
    }


    fun goLastFragment() {
        var fragmentName = objectsManager.objects[objectsManager.current].name
        var fragment = if (fragmentsMapping.containsKey(fragmentName)) fragmentsMapping[fragmentName] else null
        if (fragment == null) {
            fragmentName = objectsManager.objects[0].name
            fragment = if (fragmentsMapping.containsKey(fragmentName)) fragmentsMapping[fragmentName] else null
        }

        goFragment(fragment!!, fragmentName!!)
    }

    private fun goFragment(fragment:Fragment, fragmentName:String) {

        var position:Int? = 0
        for ((current, entry) in fragmentsList!!.withIndex()) {
            if (entry.name == fragmentName) {
                position = current
            }
        }

        // Insert the fragment by replacing any existing fragment
        try {

            val backStackEntry = fragmentManager.backStackEntryCount

            if (backStackEntry == 0) {
                val homeFragment = fragmentsMapping[FRAGMENT_LOGIN]
                if (homeFragment != null) {
                    Handler().post({
                        fragmentManager
                                .beginTransaction()
                                .add(R.id.content_frame, homeFragment)
                                .addToBackStack("HOME")
                                .commitAllowingStateLoss()
                    })
                }
            }

            Handler().post({
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
            })
            // Highlight the selected item, update the title, and close the drawer
            left_drawer.setItemChecked(position!!, true)
            title = fragmentsList!![position].name
            left_drawer.setSelection(position)
            drawer_layout.closeDrawer(left_drawer)
            lastPosition = position
        }catch(e:IllegalStateException){
            Log.e(TAG, "GO last fragment CATCH************************", e)
        }
    }


    fun goConfigureFragment() {
        val fragment = fragmentsMapping[FRAGMENT_CONFIGURE]
        // Insert the fragment by replacing any existing fragment
        fragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit()
        var position:Int? = null
        fragmentsList!!.forEachIndexed {
            index, menuEntry ->  if (menuEntry.name == FRAGMENT_CONFIGURE) {
                position = index
            }
        }

        if (position != null) {
            // Highlight the selected item, update the title, and close the drawer
            left_drawer.setItemChecked(position!!, true)
            left_drawer.setItemChecked(position!!, true)
            title = fragmentsList!![position!!].name
            left_drawer.setSelection(position!!)
            lastPosition = position!!
        }
        drawer_layout.closeDrawer(left_drawer)


    }

    private fun initFragments(): Map<String, Fragment> {

        if (configureFragment == null) {
            configureFragment = ConfigureFragment()
            configureFragment!!.setTaskFactory(taskFactory!!)
            configureFragment!!.syncListener = this
        }

        if (homeFragment == null) {
            homeFragment = HomeFragment()
            homeFragment!!.setTaskFactory(taskFactory!!)
        }

        runFragment = ArrayList()

        var tmp: RunFragment
        for (obj in objectsManager.objects) {
            tmp = RunFragment()
            tmp.setTaskFactory(taskFactory!!)
            if (obj.name != null) {
                tmp.setObjectName(obj.name!!)
                runFragment!!.add(tmp)
            }
        }


        fragmentsMapping = HashMap()
        fragmentsMapping[FRAGMENT_CONFIGURE] = configureFragment!!
        if (isLogged) {
            fragmentsMapping[FRAGMENT_LOGOUT] = homeFragment!!
        } else {
            fragmentsMapping[FRAGMENT_LOGIN] = homeFragment!!
        }
        fragmentsMapping[FRAGMENT_SETTINGS] = SettingsActivity.SettingsFragment()
        for ((pos, obj) in objectsManager.objects.withIndex()) {
            fragmentsMapping[obj.name!!] = runFragment!![pos]
        }

        return fragmentsMapping
    }

    private fun getFragment(fragmentPosition: Int): Fragment? {
        val fragmentName = fragmentsList!![fragmentPosition].name
        val fragmentMap = initFragments()
        return if (fragmentMap.containsKey(fragmentName)) fragmentMap[fragmentName] else null
    }

    companion object {
        private const val PREFERENCE_SYSTEM_NAME = "systemName"
        private const val PREFERENCE_SYSTEM_SERIAL = "systemSerial"
        private const val PREFERENCE_USER_NAME = "userName"
        private const val PREFERENCE_USER_UID = "userUid"

        private const val FRAGMENT_LOGOUT = "Logout"

        private const val FRAGMENT_LOGIN = "Login"
        private const val FRAGMENT_CONFIGURE = "Add/Modify/Delete"
        private const val FRAGMENT_SETTINGS = "Settings"
        private const val FRAGMENT_FAQ = "FAQ"



        @SuppressLint("StaticFieldLeak")
        internal lateinit var instance: MainActivity

        private val TAG = MainActivity::class.simpleName
    }

}
