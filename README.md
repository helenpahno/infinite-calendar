# infinite-calendar
An Android program that combines infinite-scroll capabilities with drag-and-drop events for an easy, convenient calendar.

What I have uploaded here is not the full project including Gradle files. It's simply the backup kit, a place to store the crucial, user-created files in case my laptop goes down. Here is the procedure for getting the program up-and-running on Android Studio on a new computer:
1. Download all XML files from "layout" and place them in the res/layout folder in your Studio project.
2. Download all XML files from "values" and place them in the res/values folder in your project.
3. Download the image files from "drawable" and place them into the res/drawable folder, EXCEPT for icon_foreground.png and icon_background.png.
4. Upload the icon files, mentioned above, separately so they go into mipmap.
5. Finally, copy-and-paste all java files from the java folder into your project's Java folder.
6. Replace the MainActivity.java and AndroidManifest.xml with their counterparts here from the repository.

Note: I've never tried to port this anywhere else yet. There's a non-zero chance it could run into some serious errors on first startup. But if you're here, I trust you. Iron out the bugs for me.

Here's an overview of the basic program structure, in Java:

MainActivity.java handles Android's native OnCreate() and OnResume() to latch onto the start of the program and make things work every time you open the app. It's a big beast, spanning about 700 lines, IIRC. It handles an OnClick() event for every button you see on the menu, as well as the ones you see in the nested menus. It also handles getting the RecyclerView, the main infinite-scroll portion, up and running. After the upheaval in which I introduced a central abstract class, InformationCentral, MainActivity mainly handles graphical updates and button clicks.

InformationCentral.java is the control tower, so to speak. I needed a way to pass information from MainActivity to all other scripts and vice-versa without running into static/non-static errors. Hence, everything in InformationCentral is static and public, and can be updated from anywhere in the app. Methods that deal with saved events, saved categories, and other end-user customizable things that need to be saved and loaded between app lifetimes, are all handled from InformationCentral. Over the course of development, I've tried to port as much to the central script as possible, to avoid weird decentralization/communication snares.

CalendarAdapter.java is the RecyclerView adapter. It handles a lot of RecyclerView-native functionality like onBindViewHolder(). A necessary evil, if you ask me, because I could probably homebrew my own version of RecyclerView at this point and make it less convoluted. But I digress. CalendarAdapter also handles the button interactions for all of the events on the scroll view, such as deletion and notification options. Like MainActivity, it calls back to InformationCentral to keep every script on the same page.

FloatingEvent.java is the adaptible version of a user-created event. Given a name, category, and duration, but no particular date, it's the "floating" form that can be dragged and dropped anywhere. These events show up in the side tab, waiting to be assigned a location.

BoundEvent.java is the real lifeblood of the app. It inherits from FloatingEvent and adds a whole bunch of date-and-time-specific variables, as well as notification settings. When a FloatingEvent is dragged into place, it spawns a BoundEvent with the same basic details, locked into a specific day and time.
