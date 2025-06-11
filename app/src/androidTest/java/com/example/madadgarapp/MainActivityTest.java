package com.example.madadgarapp;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testBottomNavigation() {
        // Test Categories navigation
        onView(withId(R.id.navigation_categories))
                .perform(click());
        onView(withId(R.id.rv_categories))
                .check(matches(isDisplayed()));

        // Test Share Item navigation
        onView(withId(R.id.navigation_share_item))
                .perform(click());
        onView(withId(R.id.til_item_title))
                .check(matches(isDisplayed()));

        // Test My Posts navigation
        onView(withId(R.id.navigation_my_posts))
                .perform(click());
        onView(withId(R.id.rv_my_posts))
                .check(matches(isDisplayed()));

        // Test Account navigation
        onView(withId(R.id.navigation_account))
                .perform(click());
        onView(withId(R.id.btn_edit_profile))
                .check(matches(isDisplayed()));

        // Test Items navigation
        onView(withId(R.id.navigation_items))
                .perform(click());
        onView(withId(R.id.rv_items))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMenuOperations() {
        // Test opening menu and clicking profile
        onView(withId(R.id.action_profile))
                .perform(click());
        onView(withId(R.id.navigation_account))
                .check(matches(isDisplayed()));

        // Test settings menu item
        onView(withId(R.id.action_settings))
                .perform(click());
        onView(withId(R.id.navigation_account))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testShareItemForm() {
        // Navigate to Share Item
        onView(withId(R.id.navigation_share_item))
                .perform(click());

        // Verify all form elements are displayed
        onView(withId(R.id.til_item_title))
                .check(matches(isDisplayed()));
        onView(withId(R.id.til_item_description))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btn_share_item))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMyPostsEmptyState() {
        // Navigate to My Posts
        onView(withId(R.id.navigation_my_posts))
                .perform(click());

        // Verify empty state is shown
        onView(withId(R.id.layout_empty_posts))
                .check(matches(isDisplayed()));
        onView(withId(R.id.fab_add_post))
                .check(matches(isDisplayed()));
    }
}
