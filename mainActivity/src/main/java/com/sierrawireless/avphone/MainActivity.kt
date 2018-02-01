package com.sierrawireless.avphone

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
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.ndk.CrashlyticsNdk
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.auth.AuthenticationManager
import com.sierrawireless.avphone.service.MonitoringService
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder
import com.sierrawireless.avphone.task.*
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import net.airvantage.model.User
import net.airvantage.utils.PreferenceUtils
import java.util.*

/**
 * The main activity, handling drawer and Fragments
 */
class MainActivity : FragmentActivity(), LoginListener, AuthenticationManager, OnSharedPreferenceChangeListener, MonitorServiceManager, CustomLabelsManager, SyncWithAvListener {
    override var monitoringService: MonitoringService? = null
    private var objectName: String? = null

    private var alarmManager: AlarmManager? = null
    private var taskFactory: IAsyncTaskFactory? = null
    override var authentication: Authentication? = null
        set(value) {
            field = value
        }
    private var prefs: SharedPreferences? = null

    internal var boundToMonitoringService = false
    private var monitoringServiceListener: MonitorServiceListener? = null

    private var customLabelsListener: CustomLabelsListener? = null

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var configureFragment: ConfigureFragment? = null
    private var homeFragment: HomeFragment? = null
    private var runFragment: ArrayList<RunFragment>? = null

    private var lastPosition = 0

    private var serviceSendData: Boolean? = false
    internal lateinit var objectsManager: ObjectsManager
    internal var user: User? = null

    override val isLogged: Boolean
        get() = this.authentication != null && !this.authentication!!.isExpired(Date())

    private var connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            Log.d(TAG, "Connected to the monitoring service")
            monitoringService = (binder as ServiceBinder).service

            if (monitoringServiceListener != null) {
                monitoringServiceListener!!.onServiceStarted(monitoringService!!)
            }

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "Disconnected from the monitoring service")
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

    fun setUser(user: User) {
        this.user = user
    }

    private fun buildFragmentList(): ArrayList<MenuEntry> {
        val tmp = ArrayList<MenuEntry>()
        if (user != null) {
            tmp.add(MenuEntry("LOGGED AS", MenuEntryType.TITLE))
            tmp.add(MenuEntry(user!!.name!!, MenuEntryType.USER))
            tmp.add(MenuEntry(user!!.profile!!.name!!, MenuEntryType.USER))
            tmp.add(MenuEntry(user!!.company!!.name!!, MenuEntryType.USER))
            tmp.add(MenuEntry(user!!.server!!, MenuEntryType.USER))
        }
        tmp.add(MenuEntry("SIMULATED OBJECTS", MenuEntryType.TITLE))
        objectsManager.objects.mapTo(tmp) { MenuEntry(it.name!!, MenuEntryType.COMMAND) }
        tmp.add(MenuEntry(FRAGMENT_CONFIGURE, MenuEntryType.COMMAND))

        //tmp.add(FRAGMENT_SETTINGS);
        tmp.add(MenuEntry("NEED HELP", MenuEntryType.TITLE))
        tmp.add(MenuEntry(FRAGMENT_FAQ, MenuEntryType.COMMAND))
        tmp.add(MenuEntry("", MenuEntryType.TITLE))
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
                loadMenu()

            }
        }

        val params = GetUserParams()

        getUserTask.execute(params)

    }


    override fun onCreate(savedInstanceState: Bundle?) {

        MainActivity.instance = this
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


        loadMenu()

        left_drawer.setOnItemClickListener { _, _, position, _ -> selectItem(position) }

        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        drawerToggle = ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open,
                R.string.drawer_close)
        drawer_layout!!.addDrawerListener(drawerToggle!!)

        // Verify Permission

        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty()) {

                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.d(TAG, "onRequestPermissionsResult: answer ko")

                        Toast.makeText(applicationContext, "Permission not granted please grant permission", Toast.LENGTH_LONG).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
                        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
                        }
                    }
                }
            }
            2 -> {
                if (grantResults.isNotEmpty()) {

                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.d(TAG, "onRequestPermissionsResult: answer ko")

                        Toast.makeText(applicationContext, "Permission not granted please grant permission", Toast.LENGTH_LONG).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
                        }
                    }
                }
            }
            3 -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                        Log.d(TAG, "onRequestPermissionsResult: answer ko")

                        Toast.makeText(applicationContext, "Permission not granted please grant permission", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    fun loadMenu() {
        FRAGMENT_LIST = buildFragmentList()


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
                connectToService()
            }

        } else {
            lockDrawer()
            if (isServiceRunning()) {
                // The token is probably expired.
                // We stop the service since the "stop" button is not available anymore.
                this.stopMonitoringService()
            }

        }
        goHomeFragment()


    }

    override fun onResume() {
        super.onResume()
        left_drawer.requestFocusFromTouch()
        left_drawer.setItemChecked(lastPosition, true)
        left_drawer.setSelection(lastPosition)
        left_drawer.refreshDrawableState()
        //drawerListView.setSelection();

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

     override fun isServiceRunning(): Boolean {
        //return serviceSendData;

        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

         @Suppress("DEPRECATION")
         return if (manager.getRunningServices(Integer.MAX_VALUE).any { MonitoringService::class.java.name == it.service.className }) serviceSendData!! else false

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

    private fun connectToService() {
        val intent = Intent(this, MonitoringService::class.java)
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

    override fun sendAlarmEvent() {
        //if (boundToMonitoringService && monitoringService != null) {
        monitoringService!!.sendAlarmEvent()
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

        connectToService()
    }

    override fun startSendData() {
        val avPrefs = PreferenceUtils.getAvPhonePrefs(this)

        val intent = Intent(this, MonitoringService::class.java)
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this))
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost)
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password)
        intent.putExtra(MonitoringService.CONNECT, true)

        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        // registering our pending intent with alarm manager
        alarmManager!!.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000,
                (Integer.valueOf(avPrefs.period)!! * 60 * 1000).toLong(), pendingIntent)

        // connectToService();
        serviceSendData = true

    }

    override fun stopSendData() {
        val intent = Intent(this, MonitoringService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager!!.cancel(pendingIntent)
        serviceSendData = false
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
        stopMonitoringService()
        startMonitoringService(objectName!!)
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

        if (runFragment != null) {
            val systemUid = this.systemUid
            val systemName = this.systemName
            for (tmp in runFragment!!) {
                tmp.setLinkToSystem(systemUid, systemName)
            }
        } else {
            Log.w(TAG, "RunFragment reference is null when onSynced is called")
        }

    }

    /**
     * Swaps fragments in the main content view
     */
    private fun selectItem(position: Int) {

        val fragment = getFragment(position)
        val entry = FRAGMENT_LIST!![position]
        if (fragment == null) {
            //No item check if the position is valid
            Log.d(TAG, "selectItem: Fragment list " + entry.name)
            if (entry.name == FRAGMENT_FAQ) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://doc.airvantage.net/alms/"))
                startActivity(browserIntent)
                left_drawer.setSelection(lastPosition)
                drawer_layout.closeDrawer(left_drawer)
            }
            return
        }
        if (entry.type == MenuEntryType.COMMAND) {

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
            drawer_layout.closeDrawer(left_drawer)
            lastPosition = position
        }
    }

    fun goHomeFragment() {
        val position = FRAGMENT_LIST!!.size - 1
        val fragment = getFragment(position)
        // Insert the fragment by replacing any existing fragment
        fragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit()

        // Highlight the selected item, update the title, and close the drawer
        left_drawer.setItemChecked(position, true)
        title = FRAGMENT_LIST!![position].name
        left_drawer.setSelection(position)
        drawer_layout.closeDrawer(left_drawer)
        lastPosition = position

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
            tmp.setObjectName(obj.name!!)
            runFragment!!.add(tmp)
        }


        val fragmentsMapping = HashMap<String, Fragment>()
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
        val fragmentName = FRAGMENT_LIST!![fragmentPosition].name
        val fragmentMap = initFragments()
        return if (fragmentMap.containsKey(fragmentName)) fragmentMap[fragmentName] else null
    }

    companion object {

        private const val TAG = "MainActivity"
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

        private var FRAGMENT_LIST: ArrayList<MenuEntry>? = null
    }

}
