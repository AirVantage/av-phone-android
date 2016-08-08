# Going `Material`

## Compatibility

[Possible](https://developer.android.com/training/material/compatibility.html),
but not simple. Cherry pick from documentation:

```java
// Check if we're running on Android 5.0 or higher
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    // Call some material design APIs here
} else {
    // Implement this feature without material design
}
```

## Fragments

- Still [(hard)core component of Android SDK](https://developer.android.com/reference/android/view/View.OnCreateContextMenuListener.html)
- Could be used better with [FragmentManager](https://developer.android.com/reference/android/app/FragmentManager.html):
    - [FragmentManager#saveFragmentInstanceState](https://developer.android.com/reference/android/app/FragmentManager.html#saveFragmentInstanceState%28android.app.Fragment%29)
    - [FragmentManager#putFragment](https://developer.android.com/reference/android/app/FragmentManager.html#putFragment%28android.os.Bundle,%20java.lang.String,%20android.app.Fragment%29)
    - [FragmentManager#popBackStack](https://developer.android.com/reference/android/app/FragmentManager.html#popBackStack%28java.lang.String,%20int%29)

## Material Design

As far as I am concerned, we should just upgrade to:

- [Material Theme](https://developer.android.com/training/material/theme.html) with Sierra colors
- Use [Navigation Drawer](https://developer.android.com/training/implementing-navigation/nav-drawer.html)
    - Do not see links with `Fragments` yet.
