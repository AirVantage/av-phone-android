Crashlytics instructions
------------------------

When checking out, the code will probably not compile because of crashlytics.
You should : 

* install the Crashlytics Eclipse plugin from fabric.io : https://fabric.io/downloads/eclipse
* after installation, right-click on the kits.properties file, and click "Update kits"

Some files should *NOT* be stored in git : 

assets/crashlytics-build.properties
res/values/com_crashlytics_export_strings.xml

(They've been added to the .gitignore)