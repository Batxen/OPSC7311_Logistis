OPSC7311 - Summative Portfolio of Evidence Submission

Purpose:
The purpose of the app is to serve as a personal budgeting app taking inspiration from pre-existing personal budgeting apps and developing our own within Android Studio. 
GitHub has been used for the storage of the files for this project, including everything needed for the project to run within Android Studio. 
This app is to be able to:
1.) Allow a user to log in
2.) Allow a user to create categories
3.) Allow a user to create expense entries
4.) Allow a user to optionally add a photo to each expense entry
5.) Allow a user to set a minimum and maximum goals for amount of money spent
6.) Allow a user to view all expense entries within a group of dates
7.) Allow a user to view the total amount spent in each category within a group of dates
8.) Allow a user to view a graph that visually represents the amount spent per category. The user must also be able to see the minimum and maximum goals
9.) Allow a user to view how well they have been able to stay inside their minimum and maximum spending goals
10.) Store data of the users within an online database (Firebase)
11.) Offer two unique experiences for the user through gamification 

Video Presentation:
https://youtu.be/mVbB_Iy_RV4?si=0XSZcstng7Vcjkdy 

Gamification Features:

1. Budget Streak
Where: the calculateStreak function in RewardTools.kt.
What it does: divides the monthly maximum goal by 30 to get the daily safe-to-spend amount. It adds up spending per day, then walks back from today and counts consecutive days at or under that amount. The first day over the amount stops the count.
Details to mention: it ignores future-dated expenses and starts counting from the first recorded expense. If no monthly goal is saved, the streak is 0.
On screen: Menu.kt shows it in the tvMenuStreak counter on the home screen and refreshes it in onResume. Tapping it opens the Rewards screen.
2. Reward Points, Recreation tag and tiers
Tag on categories: dbRoom.kt has a new tag field on dcCategory (Essential, Recreation or Other), and the database version went from 3 to 4. The tag spinner is in dataCapture.kt, the Categories screen.
Points logic: the calculateRewards function in RewardTools.kt looks at the last 30 days:
+50 for each category at or under its maximum.
Recreation categories get up to +100 extra, calculated as (1 - spent ÷ max) × 100.
A category over its maximum gets 0.
+5 for each streak day.
No points are given until at least one expense is logged.
Tiers: the tierInfo function sets the tiers at Bronze 0, Silver 100, Gold 250 and Platinum 500, and works out the progress-bar percentage.
Design choice: nothing is stored for points or streak. Both are calculated from the expenses, categories and goals each time, so they always match what is in Firebase.
Screen: Rewards.kt and activity_rewards.xml show the streak, total points, tier, progress bar, and a per-category breakdown list.

Online database: (Firebase)

All of this is in FirebaseSync.kt.

Register and login: registerUser and loginUser check Firebase with an 8-second timeout. They return a result: OK, TAKEN, NOT_FOUND, WRONG_PASSWORD or OFFLINE.
Passwords: only a salted SHA-256 hash is stored, never the password. The username is the salt. The hashing function is in clsTools.kt.
Session: the Session object remembers the logged-in user using SharedPreferences. Logout clears it.
Saving: pushCategory, pushExpense and pushGoal save to Firebase every time something is saved locally. Firebase persistence is switched on, so saves made offline are queued and sent when the phone reconnects.
Photos: pushPhoto shrinks the photo to about 1000 px at 60% JPEG quality, turns it into Base64 text, and stores it under the user's photos node.
Loading: pullAll runs at login. It downloads the user's categories, expenses and goal, clears the local Room tables, and refills them. restorePhoto downloads any photo missing from the phone. This is how a fresh install gets everything back.
Layout online: users / {username} / password, categories, expenses, goal, photos.
Graphs (brief requirement)
Spent per category: CategoryTotals.kt uses MPAndroidChart. It draws bars for spending per category with lines for each category's min and max goal. The user picks the start and end dates with date pickers.
Past-month goal visual: MonthlyGoals.kt draws a 30-day running-total line graph with dashed lines for the min and max goals. A coloured status bar shows green when on track, amber when below the minimum, and red when over the maximum.


Testing and GitHub
Unit tests: RewardToolsTest.kt has 20 tests covering parsing, streak, reward points and tiers.
GitHub Actions: .github/workflows/build.yml runs the tests and builds the APK on every push. It's green, so show the Actions tab in the video.

