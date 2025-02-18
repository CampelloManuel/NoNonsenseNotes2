package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.DatabaseHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public class BaseTestClass {

	/**
	 * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
	 * for ActivityInstrumentationTestCase2.
	 *
	 * Rules are interceptors which are executed for each test method and will run before
	 * any of your setup code in the @Before method.
	 *
	 * This will create and launch of the activity for you and also expose
	 * the activity under test. To get a reference to the activity you can use:
	 * {@link ActivityTestRule#getActivity()}
	 * <br/>
	 * NOTE: the alternative, {@link ActivityScenarioRule}, <b>DOES NOT WORK</b>
	 */
	@SuppressWarnings("deprecation")
	@Rule
	public ActivityTestRule<ActivityMain_> mActRule = new ActivityTestRule<>(ActivityMain_.class);

	/**
	 * @return a string with the content of the given resourceId
	 */
	public String getStringResource(int resourceId) {
		return mActRule.getActivity().getString(resourceId);
	}

	@After
	public void clearAppData() {
		Context context = ApplicationProvider.getApplicationContext();

		// clear the app's data as the test is starting & finishing
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		DatabaseHandler.resetDatabase(context);
	}

	/**
	 * Many times, on the github VM, the tests fail with RootViewWithoutFocusException,
	 * I think it's due to the emulator being slow. Let's launch the activity and wait
	 * for it to load before starting the real test
	 */
	@Before
	public void launchAndWait() {
		// ensure that this is called BEFORE trying to start the activity
		clearAppData();

		try {
			// it responds => we can return now
			onView(isRoot()).check(matches(isDisplayed()));
			return;
		} catch (Exception e) {
			NnnLogger.error(this.getClass(), "Activity isn't responsive:");
			NnnLogger.exception(e);
		}

		// maybe we just have to wait
		EspressoHelper.waitUi();

		// if it's still not enough, let's crash here
		try {
			onView(isRoot()).check(matches(isDisplayed()));
		} catch (Exception e) {
			NnnLogger.error(this.getClass(), "Can't launch activity:");
			NnnLogger.exception(e);
			throw e;
		}
	}
}
