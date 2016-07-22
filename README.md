# av-phone

An android application to monitor phone state and send it periodically to
_AirVantage platform_.

Official application is available on
[Play Store](https://play.google.com/store/apps/details?id=com.sierrawireless.avphone)

Code is published here only as an example of how to use _AirVantage APIs_ from
an Android Application.

## Installation

Tested with Android `4.4.2`

## Configuration

You need to edit `mainActivity/build/intermediates/assets/debug/avphone.properties`
to specify Airvantage API clients on the NA and EU instances (respectively).

If the API clients are missing, the "login" pages will display an error page:

> Something went wrong
