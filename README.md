# AV Phone

An android application to monitor phone state and send it periodically to
_AirVantage platform_.

Official application is available on
[Play Store](https://play.google.com/store/apps/details?id=com.sierrawireless.avphone)

Code is published here only as an example of how to use _AirVantage APIs_ from
an Android Application.

> Tested with Android `4.4.2`

## Installation

## Configuration

You need to edit `mainActivity/build/intermediates/assets/debug/avphone.properties`
to specify Airvantage API clients on `NA` and `EU` instances.

If API clients are missing, _login_ pages will display an error page:

> Something went wrong

## Configure custom server

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
