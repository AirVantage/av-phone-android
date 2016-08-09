# AV Phone

An android application to monitor phone state and send it periodically to
_AirVantage platform_.

Official application is available on
[Play Store](https://play.google.com/store/apps/details?id=com.sierrawireless.avphone)

Code is published here only as an example of how to use _AirVantage APIs_ from
an Android Application.

> Tested with Android `4.4.2`

## Installation

### Configuration

You need to edit `mainActivity/build/intermediates/assets/debug/avphone.properties`
to specify Airvantage API clients on `NA` and `EU` instances.

If API clients are missing, _login_ pages will display an error page:

> Something went wrong

### Configure custom server

Button appears as soon as `clientid.custom` is defined.

Define `clientid.custom` in `mainActivity/src/main/assets/avphone.properties`.

```ini
clientid.na=CHANGEME
clientid.eu=CHANGEME
clientid.custom=IF_YOU_NEED # <= Here
```

Define `pref_server_custom_value` in `mainActivity/src/main/res/values/strings.xml`.
It __has to__ be accessible from `https://`.

```xml
<string name="pref_server_custom">Custom</string>
<!-- Change following -->
<string name="pref_server_custom_value">get.some.io</string>
```

### Crashlytics

When checking out, code will probably not compile because of Crashlytics,
install [Crashlytics Android Studio plugin](https://fabric.io/downloads/android).

`mainActivity/src/main/assets/crashlytics-build.properties` __must not__ be stored,
it's listed in `.gitignore`.

## Release

Publish a new version
---------------------

1. Increase version number in:
    - `mainActivity/build.gradle`
    - `mainActivity/src/main/AndroidManifest.xml`

1. Using __Build > Generate Signed APK...__
    1. Use `airvantagekeystore.jks`, [stored in AWS](https://console.aws.amazon.com/s3/home?region=us-west-2#&bucket=av-prod-secrets&prefix=certificates/android/site-survey/) as __Key store path__.
    2. You'll find it's password in file stored in the same folder
    3. Select the only proposed __key alias__
    4. Enter the same password as for the key store
    5. Hit __Next__
    6. Select destination folder
    7. Select __release__ build type
    8. Hit __Finish__
1. Go to [Developer Console/APK](https://play.google.com/apps/publish/?dev_acc=14924182668628180177#ApkPlace:p=com.sierrawireless.avphone) section
1. Hit the __Upload new APK to production__ button and follow instructions
1. :tada:
